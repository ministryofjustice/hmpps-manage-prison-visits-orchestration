package uk.gov.justice.digital.hmpps.prison.visits.orchestration.client

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.visit.allocation.PrisonerVOBalanceDto
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.visit.allocation.VisitOrderHistoryDetailsDto
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.visit.allocation.VisitOrderHistoryDto
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.exception.InvalidPrisonerProfileException
import java.time.Duration
import java.time.LocalDate
import java.util.Optional

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
    const val VO_DETAILED_BALANCE_URI = "/visits/allocation/prisoner/{prisonerId}/balance/detailed"
  }

  fun getPrisonerVOBalance(prisonerId: String): Optional<PrisonerVOBalanceDto>? = getPrisonerVOBalanceAsMono(prisonerId).block(apiTimeout)

  fun getPrisonerVOBalanceAsMono(prisonerId: String): Mono<Optional<PrisonerVOBalanceDto>> {
    val uri = VO_DETAILED_BALANCE_URI.replace("{prisonerId}", prisonerId)
    return webClient.get()
      .uri(uri)
      .retrieve()
      .bodyToMono<Optional<PrisonerVOBalanceDto>>()
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
    PrisonerProfileClient.Companion.LOG.trace("getVisitOrderHistory - for prisoner {} from date {}", prisonerId, fromDate)
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
