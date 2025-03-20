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
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.ClientUtils.Companion.isNotFoundError
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.RestPage
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.InmateDetailDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.OffenderRestrictionsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.PrisonerBookingSummaryDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.VisitBalancesDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.NotFoundException
import java.time.Duration
import java.util.Optional

@Component
class PrisonApiClient(
  @Qualifier("prisonApiWebClient") private val webClient: WebClient,
  @Value("\${prison.api.timeout:10s}") private val apiTimeout: Duration,
) {

  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getInmateDetails(prisonerId: String): InmateDetailDto? = getInmateDetailsAsMono(prisonerId)
    .block(apiTimeout)

  fun getInmateDetailsAsMono(prisonerId: String): Mono<InmateDetailDto> = webClient.get()
    .uri("/api/offenders/$prisonerId")
    .retrieve()
    .bodyToMono()

  fun getBookings(prisonId: String, prisonerId: String): RestPage<PrisonerBookingSummaryDto>? = getBookingsAsMono(prisonId, prisonerId)
    .block(apiTimeout)

  fun getBookingsAsMono(prisonId: String, prisonerId: String): Mono<RestPage<PrisonerBookingSummaryDto>> = webClient.get()
    .uri("/api/bookings/v2") {
      it.queryParam("prisonId", prisonId)
        .queryParam("offenderNo", prisonerId)
        .queryParam("legalInfo", true).build()
    }
    .retrieve()
    .bodyToMono()

  fun getVisitBalances(prisonerId: String): Optional<VisitBalancesDto>? = getVisitBalancesAsMono(prisonerId).block(apiTimeout)

  fun getVisitBalancesAsMono(prisonerId: String): Mono<Optional<VisitBalancesDto>> {
    return webClient.get()
      .uri("/api/bookings/offenderNo/$prisonerId/visit/balances")
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

  fun getPrisonerRestrictions(prisonerId: String): OffenderRestrictionsDto {
    val uri = "/api/offenders/$prisonerId/offender-restrictions"

    return getPrisonerRestrictionsAsMono(prisonerId)
      .onErrorResume { e ->
        if (!isNotFoundError(e)) {
          LOG.error("getOffenderRestrictions Failed get request $uri")
          Mono.error(e)
        } else {
          LOG.error("getOffenderRestrictions NOT FOUND get request $uri")
          Mono.error { NotFoundException("No Offender restrictions found for prisoner - $prisonerId on prison-api") }
        }
      }
      .blockOptional(apiTimeout).orElseThrow { NotFoundException("No Offender restrictions found for prisoner - $prisonerId on prison-api") }
  }

  fun getPrisonerRestrictionsAsMono(prisonerId: String): Mono<OffenderRestrictionsDto> {
    val uri = "/api/offenders/$prisonerId/offender-restrictions"
    return webClient.get()
      .uri(uri) {
        it.queryParam("activeRestrictionsOnly", true).build()
      }
      .retrieve()
      .bodyToMono<OffenderRestrictionsDto>()
  }
}
