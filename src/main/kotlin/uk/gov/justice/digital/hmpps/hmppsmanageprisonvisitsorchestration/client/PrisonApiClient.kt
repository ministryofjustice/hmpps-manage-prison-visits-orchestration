package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.RestPage
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.CaseLoadDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.InmateDetailDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.OffenderRestrictionsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.PrisonerBookingSummaryDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.VisitBalancesDto
import java.time.Duration

@Component
class PrisonApiClient(
  @Qualifier("prisonApiWebClient") private val webClient: WebClient,
  @Value("\${prison.api.timeout:10s}") private val apiTimeout: Duration
) {
  // TODO - keep this till we do the performance tests
/*
  fun getInmateDetails(offenderNo: String): InmateDetailDto? {
    return webClient.get()
      .uri("/api/offenders/$offenderNo")
      .retrieve()
      .bodyToMono<InmateDetailDto>()
      .block(apiTimeout)
  }
*/

  fun getInmateDetails(prisonerId: String): Mono<InmateDetailDto> {
    return webClient.get()
      .uri("/api/offenders/$prisonerId")
      .retrieve()
      .bodyToMono()
  }

  /*
  // TODO - keep this till we do the performance tests
  fun getBookings(offenderNo: String, prisonId: String): RestPage<PrisonerBookingSummaryDto>? {
    return webClient.get()
      .uri("/api/bookings/v2") {
        it.queryParam("prisonId", prisonId)
          .queryParam("offenderNo", offenderNo)
          .queryParam("legalInfo", true).build()
      }
      .retrieve()
      .bodyToMono<RestPage<PrisonerBookingSummaryDto>>()
      .block(apiTimeout)
  }*/

  fun getBookings(prisonId: String, prisonerId: String): Mono<RestPage<PrisonerBookingSummaryDto>> {
    return webClient.get()
      .uri("/api/bookings/v2") {
        it.queryParam("prisonId", prisonId)
          .queryParam("offenderNo", prisonerId)
          .queryParam("legalInfo", true).build()
      }
      .retrieve()
      .bodyToMono()
  }

  // TODO - keep this till we do the performance tests
  /*fun getVisitBalances(offenderNo: String): VisitBalancesDto? {
    return webClient.get()
      .uri("/api/bookings/offenderNo/$offenderNo/visit/balances")
      .retrieve()
      .bodyToMono<VisitBalancesDto>()
      .block(apiTimeout)
  }*/

  fun getVisitBalances(prisonerId: String): Mono<VisitBalancesDto> {
    return webClient.get()
      .uri("/api/bookings/offenderNo/$prisonerId/visit/balances")
      .retrieve()
      .bodyToMono()
  }

  fun getOffenderRestrictions(prisonerId: String): OffenderRestrictionsDto? {
    return webClient.get()
      .uri("/api/offenders/$prisonerId/offender-restrictions") {
        it.queryParam("activeRestrictionsOnly", true).build()
      }
      .retrieve()
      .bodyToMono<OffenderRestrictionsDto>()
      .block(apiTimeout)
  }

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
