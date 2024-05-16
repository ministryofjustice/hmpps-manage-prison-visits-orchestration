package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.RestPage
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.CaseLoadDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.InmateDetailDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.OffenderRestrictionsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.PrisonerBookingSummaryDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.VisitBalancesDto
import java.time.Duration
import java.util.Optional
import kotlin.collections.ArrayList

@Component
class PrisonApiClient(
  @Qualifier("prisonApiWebClient") private val webClient: WebClient,
  @Value("\${prison.api.timeout:10s}") private val apiTimeout: Duration,
) {

  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getInmateDetails(prisonerId: String): InmateDetailDto? {
    return getInmateDetailsAsMono(prisonerId)
      .block(apiTimeout)
  }

  fun getInmateDetailsAsMono(prisonerId: String): Mono<InmateDetailDto> {
    return webClient.get()
      .uri("/api/offenders/$prisonerId")
      .retrieve()
      .bodyToMono()
  }

  fun getBookings(prisonId: String, prisonerId: String): RestPage<PrisonerBookingSummaryDto>? {
    return getBookingsAsMono(prisonId, prisonerId)
      .block(apiTimeout)
  }

  fun getBookingsAsMono(prisonId: String, prisonerId: String): Mono<RestPage<PrisonerBookingSummaryDto>> {
    return webClient.get()
      .uri("/api/bookings/v2") {
        it.queryParam("prisonId", prisonId)
          .queryParam("offenderNo", prisonerId)
          .queryParam("legalInfo", true).build()
      }
      .retrieve()
      .bodyToMono()
  }

  fun getVisitBalances(prisonerId: String): Optional<VisitBalancesDto>? {
    return getVisitBalancesAsMono(prisonerId).block(apiTimeout)
  }

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

  fun getPrisonerRestrictions(prisonerId: String): OffenderRestrictionsDto? {
    return webClient.get()
      .uri("/api/offenders/$prisonerId/offender-restrictions") {
        it.queryParam("activeRestrictionsOnly", true).build()
      }
      .retrieve()
      .bodyToMono<OffenderRestrictionsDto>()
      .onErrorResume {
          e ->
        if (!isNotFoundError(e)) {
          LOG.error("getOffenderRestrictions Failed get request /api/offenders/$prisonerId/offender-restrictions")
          Mono.error(e)
        } else {
          LOG.error("getOffenderRestrictions NOT FOUND get request /api/offenders/$prisonerId/offender-restrictions")
          return@onErrorResume Mono.justOrEmpty(null)
        }
      }
      .block(apiTimeout)
  }

  fun isNotFoundError(e: Throwable?) =
    e is WebClientResponseException && e.statusCode == NOT_FOUND

  fun getUserCaseLoads(): ArrayList<CaseLoadDto>? {
    return webClient.get()
      .uri("/api/users/me/caseLoads")
      .retrieve()
      .bodyToMono<ArrayList<CaseLoadDto>>()
      .block(apiTimeout)
  }

  fun setActiveCaseLoad(caseLoadId: String) {
    webClient.put().uri("/api/users/me/activeCaseLoad")
      .body(BodyInserters.fromValue(caseLoadId))
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
  }
}
