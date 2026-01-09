package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

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
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonVisitBookerRegistryClient.Companion.logger
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation.PrisonerBalanceAdjustmentDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation.PrisonerVOBalanceDetailedDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation.VisitOrderHistoryDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation.VisitOrderHistoryDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation.VisitOrderPrisonerBalanceDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.InvalidPrisonerProfileException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.NotFoundException
import java.time.Duration
import java.time.LocalDate
import java.util.*

@Component
class VisitAllocationApiClient(
  @param:Qualifier("visitAllocationApiWebClient") private val webClient: WebClient,
  @param:Value("\${visit-allocation.api.timeout:10s}") private val apiTimeout: Duration,
  private val prisonerSearchClient: PrisonerSearchClient,
  private val prisonApiClient: PrisonApiClient,
) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
    const val VISIT_ORDER_HISTORY_URI = "/visits/allocation/prisoner/{prisonerId}/visit-order-history"
    const val VO_BALANCE_ENDPOINT = "/visits/allocation/prisoner/{prisonerId}/balance"
    const val VO_DETAILED_BALANCE_URI = "$VO_BALANCE_ENDPOINT/detailed"
  }

  fun adjustPrisonersVisitOrderBalanceAsMono(prisonerId: String, prisonerBalanceAdjustmentDto: PrisonerBalanceAdjustmentDto) {
    val uri = VO_BALANCE_ENDPOINT.replace("{prisonerId}", prisonerId)

    webClient.put()
      .uri(uri)
      .body(BodyInserters.fromValue(prisonerBalanceAdjustmentDto))
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .toBodilessEntity()
      .onErrorResume { e ->
        LOG.error("Could not manually adjust prisoner's balance due to exception when calling visit allocation api:", e)
        Mono.error(e)
      }
      .block(apiTimeout)
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

  fun getVisitOrderHistoryDetails(prisonerId: String, fromDate: LocalDate): VisitOrderHistoryDetailsDto? {
    LOG.trace("getVisitOrderHistory - for prisoner {} from date {}", prisonerId, fromDate)
    val prisonerMono = prisonerSearchClient.getPrisonerByIdAsMono(prisonerId)
    val inmateDetailMono = prisonApiClient.getInmateDetailsAsMono(prisonerId)
    val visitOrderHistoryListMono = getPrisonerVisitOrderHistoryAsMono(prisonerId, fromDate)
    return Mono.zip(prisonerMono, inmateDetailMono, visitOrderHistoryListMono)
      .map { visitOrderHistoryMonos ->
        val prisoner = visitOrderHistoryMonos.t1 ?: throw InvalidPrisonerProfileException("Unable to retrieve offender details from Prisoner Search API")
        val inmateDetails = visitOrderHistoryMonos.t2 ?: throw InvalidPrisonerProfileException("Unable to retrieve inmate details from Prison API")

        val visitOrderHistoryList = visitOrderHistoryMonos.t3.sortedBy { it.createdTimeStamp }
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
}
