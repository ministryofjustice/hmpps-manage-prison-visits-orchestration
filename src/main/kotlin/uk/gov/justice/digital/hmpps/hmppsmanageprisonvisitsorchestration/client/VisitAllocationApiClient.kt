package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.ClientUtils.Companion.isUnprocessableEntityError
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonVisitBookerRegistryClient.Companion.logger
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config.PrisonerBalanceAdjustmentValidationErrorResponse
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation.PrisonerBalanceAdjustmentDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation.PrisonerVOBalanceDetailedDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation.VisitOrderHistoryDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation.VisitOrderHistoryDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation.VisitOrderPrisonerBalanceDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.InvalidPrisonerProfileException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.NotFoundException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.PrisonerBalanceAdjustmentValidationException
import java.time.Duration
import java.time.LocalDate
import java.util.*

@Component
class VisitAllocationApiClient(
  @param:Qualifier("visitAllocationApiWebClient") private val webClient: WebClient,
  @param:Value("\${visit-allocation.api.timeout:10s}") private val apiTimeout: Duration,
  private val prisonApiClient: PrisonApiClient,
  private val objectMapper: ObjectMapper,
) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
    const val VISIT_ORDER_HISTORY_URI = "/visits/allocation/prisoner/{prisonerId}/visit-order-history"
    const val VO_BALANCE_ENDPOINT = "/visits/allocation/prisoner/{prisonerId}/balance"
    const val VO_DETAILED_BALANCE_URI = "$VO_BALANCE_ENDPOINT/detailed"
  }

  fun adjustPrisonersVisitOrderBalanceAsMono(prisonerId: String, prisonerBalanceAdjustmentDto: PrisonerBalanceAdjustmentDto): VisitOrderPrisonerBalanceDto {
    val uri = VO_BALANCE_ENDPOINT.replace("{prisonerId}", prisonerId)

    return webClient.put()
      .uri(uri)
      .body(BodyInserters.fromValue(prisonerBalanceAdjustmentDto))
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<VisitOrderPrisonerBalanceDto>()
      .onErrorResume { e ->
        if (isUnprocessableEntityError(e)) {
          LOG.error("Could not manually adjust prisoner's balance due to validation exception when calling visit allocation api:", e)
          val exception = getPrisonerBalanceAdjustmentValidationErrorResponse(e)
          Mono.error(exception)
        } else {
          LOG.error("Could not manually adjust prisoner's balance due to exception when calling visit allocation api:", e)
          Mono.error(e)
        }
      }
      .block(apiTimeout) ?: throw IllegalStateException("timeout response from visit-allocation-api for adjustPrisonersVisitOrderBalanceAsMono")
  }

  fun getPrisonerVOBalance(prisonerId: String): VisitOrderPrisonerBalanceDto {
    val uri = VO_BALANCE_ENDPOINT.replace("{prisonerId}", prisonerId)
    return webClient.get()
      .uri(uri)
      .retrieve()
      .bodyToMono<VisitOrderPrisonerBalanceDto>()
      .onErrorResume { e ->
        logger.error("getPrisonerVOBalance failed for get request $uri, $e")
        Mono.error(e)
      }
      .blockOptional(apiTimeout)
      .orElseThrow { NotFoundException("Prisoner not found for request $uri") }
  }

  fun getPrisonerVOBalanceDetailedAsMono(prisonerId: String): Mono<Optional<PrisonerVOBalanceDetailedDto>> {
    val uri = VO_DETAILED_BALANCE_URI.replace("{prisonerId}", prisonerId)
    return webClient.get()
      .uri(uri)
      .retrieve()
      .bodyToMono<Optional<PrisonerVOBalanceDetailedDto>>()
      .onErrorResume { e ->
        if (e is WebClientResponseException && e.statusCode == HttpStatus.NOT_FOUND) {
          // return an Optional.empty element if 404 is thrown
          return@onErrorResume Mono.just(Optional.empty())
        } else {
          Mono.error(e)
        }
      }
  }

  fun getVisitOrderHistoryDetails(prisoner: PrisonerDto, fromDate: LocalDate): VisitOrderHistoryDetailsDto? {
    val prisonerId = prisoner.prisonerNumber
    LOG.trace("getVisitOrderHistory - for prisoner {} from date {}", prisonerId, fromDate)
    val inmateDetailMono = prisonApiClient.getInmateDetailsAsMono(prisonerId)
    val visitOrderHistoryListMono = getPrisonerVisitOrderHistoryAsMono(prisonerId, fromDate)
    return Mono.zip(inmateDetailMono, visitOrderHistoryListMono)
      .map { visitOrderHistoryMonos ->
        val inmateDetails = visitOrderHistoryMonos.t1 ?: throw InvalidPrisonerProfileException("Unable to retrieve inmate details from Prison API")

        val visitOrderHistoryList = visitOrderHistoryMonos.t2.sortedBy { it.createdTimeStamp }
        VisitOrderHistoryDetailsDto(
          prisonerId = prisonerId,
          firstName = prisoner.firstName,
          lastName = prisoner.lastName,
          category = inmateDetails.category,
          convictedStatus = prisoner.convictedStatus,
          incentiveLevel = prisoner.currentIncentive?.level?.description,
          visitOrderHistory = visitOrderHistoryList,
        )
      }
      .block(apiTimeout)
  }

  private fun getPrisonerVisitOrderHistoryAsMono(prisonerId: String, fromDate: LocalDate): Mono<List<VisitOrderHistoryDto>> {
    val uri = VISIT_ORDER_HISTORY_URI.replace("{prisonerId}", prisonerId)
    return webClient.get()
      .uri(uri) {
        visitOrderHistoryUriBuilder(fromDate, it).build()
      }
      .retrieve()
      .bodyToMono<List<VisitOrderHistoryDto>>()
      .onErrorResume { e ->
        LOG.error("getPrisonerVisitOrderHistory Failed for get request $uri")
        Mono.error(e)
      }
  }

  private fun visitOrderHistoryUriBuilder(
    fromDate: LocalDate,
    uriBuilder: UriBuilder,
  ): UriBuilder {
    uriBuilder.queryParam("fromDate", fromDate)

    return uriBuilder
  }

  private fun getPrisonerBalanceAdjustmentValidationErrorResponse(e: Throwable): Throwable {
    if (e is WebClientResponseException && isUnprocessableEntityError(e)) {
      try {
        val errorResponse = objectMapper.readValue(e.responseBodyAsString, PrisonerBalanceAdjustmentValidationErrorResponse::class.java)
        return PrisonerBalanceAdjustmentValidationException(errorResponse.validationErrors)
      } catch (jsonProcessingException: Exception) {
        LOG.error("An error occurred submitting request to manually adjust prisoner VO balance, error response - ${e.stackTraceToString()}")
        throw jsonProcessingException
      }
    }

    return e
  }
}
