package uk.gov.justice.digital.hmpps.orchestration.client

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
import uk.gov.justice.digital.hmpps.orchestration.dto.visit.allocation.PrisonerVOBalanceDto
import uk.gov.justice.digital.hmpps.orchestration.dto.visit.allocation.VisitOrderHistoryDto
import java.time.Duration
import java.time.LocalDate
import java.util.Optional

@Component
class VisitAllocationApiClient(
  @param:Qualifier("visitAllocationApiWebClient") private val webClient: WebClient,
  @param:Value("\${visit-allocation.api.timeout:10s}") private val apiTimeout: Duration,
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

  fun getPrisonerVisitOrderHistory(prisonerId: String, fromDate: LocalDate): List<VisitOrderHistoryDto> {
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
      .blockOptional(apiTimeout)
      .orElse(emptyList())
  }

  private fun visitOrderHistoryUriBuilder(
    fromDate: LocalDate,
    uriBuilder: UriBuilder,
  ): UriBuilder {
    uriBuilder.queryParam("fromDate", fromDate)

    return uriBuilder
  }
}
