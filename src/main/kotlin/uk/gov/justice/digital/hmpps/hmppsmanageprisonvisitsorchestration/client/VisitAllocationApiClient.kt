package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.VisitBalancesDto
import java.time.Duration
import java.util.Optional

@Component
class VisitAllocationApiClient(
  @Qualifier("visitAllocationApiWebClient") private val webClient: WebClient,
  @Value("\${visit-allocation.api.timeout:10s}") private val apiTimeout: Duration,
) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getPrisonerVOBalance(prisonerId: String): Optional<VisitBalancesDto>? = getPrisonerVOBalanceAsMono(prisonerId).block(apiTimeout)

  fun getPrisonerVOBalanceAsMono(prisonerId: String): Mono<Optional<VisitBalancesDto>> {
    val uri = "/visits/allocation/prisoner/$prisonerId/balance"
    return webClient.get()
      .uri(uri)
      .retrieve()
      .bodyToMono<Optional<VisitBalancesDto>>()
      .onErrorResume { e ->
        if (e is WebClientResponseException && e.statusCode == HttpStatus.NOT_FOUND) {
          // return an Optional.empty element if 404 is thrown
          return@onErrorResume Mono.just(Optional.empty())
        } else {
          Mono.error(e)
        }
      }
  }
}
