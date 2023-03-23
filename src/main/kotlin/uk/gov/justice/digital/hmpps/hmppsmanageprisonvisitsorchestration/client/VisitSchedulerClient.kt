package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.RestPage
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.CancelVisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.ChangeVisitSlotRequestDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionCapacityDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionScheduleDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SupportTypeDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSchedulerReserveVisitSlotDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSessionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.filter.VisitSearchRequestFilter
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.Optional

@Component
class VisitSchedulerClient(

  @Qualifier("visitSchedulerWebClient") private val webClient: WebClient,
  @Value("\${visit-scheduler.api.timeout:10s}") val apiTimeout: Duration,
) {
  fun getVisitByReference(reference: String): VisitDto? {
    return webClient.get()
      .uri("/visits/$reference")
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<VisitDto>().block(apiTimeout)
  }

  fun getVisits(visitSearchRequestFilter: VisitSearchRequestFilter): RestPage<VisitDto>? {
    return getVisitsAsMono(visitSearchRequestFilter).block(apiTimeout)
  }

  fun getVisitsAsMono(visitSearchRequestFilter: VisitSearchRequestFilter): Mono<RestPage<VisitDto>> {
    return webClient.get()
      .uri("/visits/search") {
        visitSearchUriBuilder(visitSearchRequestFilter, it).build()
      }
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono()
  }

  fun reserveVisitSlot(reserveVisitSlotDto: VisitSchedulerReserveVisitSlotDto): VisitDto? {
    return webClient.post()
      .uri("/visits/slot/reserve")
      .body(BodyInserters.fromValue(reserveVisitSlotDto))
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<VisitDto>().block(apiTimeout)
  }

  fun bookVisitSlot(applicationReference: String): VisitDto? {
    return webClient.put()
      .uri("/visits/$applicationReference/book")
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<VisitDto>().block(apiTimeout)
  }

  fun changeVisitSlot(applicationReference: String, changeVisitSlotRequestDto: ChangeVisitSlotRequestDto): VisitDto? {
    return webClient.put()
      .uri("/visits/$applicationReference/slot/change")
      .body(BodyInserters.fromValue(changeVisitSlotRequestDto))
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<VisitDto>().block(apiTimeout)
  }

  fun changeBookedVisit(reference: String, reserveVisitSlotDto: VisitSchedulerReserveVisitSlotDto): VisitDto? {
    return webClient.put()
      .uri("/visits/$reference/change")
      .body(BodyInserters.fromValue(reserveVisitSlotDto))
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<VisitDto>().block(apiTimeout)
  }

  fun cancelVisit(reference: String, cancelVisitDto: CancelVisitDto): VisitDto? {
    return webClient.put()
      .uri("/v2/visits/$reference/cancel")
      .body(BodyInserters.fromValue(cancelVisitDto))
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<VisitDto>().block(apiTimeout)
  }

  fun getVisitSupport(): List<SupportTypeDto>? {
    return webClient.get()
      .uri("/visit-support")
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<List<SupportTypeDto>>().block(apiTimeout)
  }

  fun getVisitSessions(prisonId: String, prisonerId: String?, min: Long?, max: Long?): List<VisitSessionDto>? {
    return webClient.get()
      .uri("/visit-sessions") {
        visitSessionsUriBuilder(prisonId, prisonerId, min, max, it).build()
      }
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<List<VisitSessionDto>>().block(apiTimeout)
  }

  fun getSupportedPrisons(): List<String>? {
    return webClient.get()
      .uri("/config/prisons/supported")
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<List<String>>().block(apiTimeout)
  }

  fun getSessionCapacity(
    prisonCode: String,
    sessionDate: LocalDate,
    sessionStartTime: LocalTime,
    sessionEndTime: LocalTime,
  ): SessionCapacityDto? {
    return webClient.get()
      .uri("/visit-sessions/capacity") {
        visitSessionsCapacityUriBuilder(prisonCode, sessionDate, sessionStartTime, sessionEndTime, it).build()
      }
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<SessionCapacityDto>().block(apiTimeout)
  }

  fun getSessionSchedule(
    prisonCode: String,
    sessionDate: LocalDate,
  ): List<SessionScheduleDto>? {
    return webClient.get()
      .uri("/visit-sessions/schedule") {
        it.queryParam("prisonId", prisonCode)
          .queryParam("date", sessionDate)
          .build()
      }
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<List<SessionScheduleDto>>().block(apiTimeout)
  }

  private fun visitSearchUriBuilder(visitSearchRequestFilter: VisitSearchRequestFilter, uriBuilder: UriBuilder): UriBuilder {
    uriBuilder.queryParamIfPresent("prisonId", Optional.ofNullable(visitSearchRequestFilter.prisonCode))
    uriBuilder.queryParamIfPresent("prisonerId", Optional.ofNullable(visitSearchRequestFilter.prisonerId))
    uriBuilder.queryParam("visitStatus", visitSearchRequestFilter.visitStatusList)
    uriBuilder.queryParamIfPresent("startDateTime", Optional.ofNullable(visitSearchRequestFilter.startDateTime))
    uriBuilder.queryParamIfPresent("endDateTime", Optional.ofNullable(visitSearchRequestFilter.endDateTime))
    uriBuilder.queryParamIfPresent("visitorId", Optional.ofNullable(visitSearchRequestFilter.visitorId))
    uriBuilder.queryParam("page", visitSearchRequestFilter.page)
    uriBuilder.queryParam("size", visitSearchRequestFilter.size)
    return uriBuilder
  }

  private fun visitSessionsUriBuilder(prisonId: String, prisonerId: String?, min: Long?, max: Long?, uriBuilder: UriBuilder): UriBuilder {
    uriBuilder.queryParam("prisonId", prisonId)
    uriBuilder.queryParamIfPresent("prisonerId", Optional.ofNullable(prisonerId))
    uriBuilder.queryParamIfPresent("min", Optional.ofNullable(min))
    uriBuilder.queryParamIfPresent("max", Optional.ofNullable(max))
    return uriBuilder
  }

  private fun visitSessionsCapacityUriBuilder(
    prisonCode: String,
    sessionDate: LocalDate,
    sessionStartTime: LocalTime,
    sessionEndTime: LocalTime,
    uriBuilder: UriBuilder,
  ): UriBuilder {
    uriBuilder.queryParam("prisonId", prisonCode)
    uriBuilder.queryParam("sessionDate", sessionDate)
    uriBuilder.queryParam("sessionStartTime", sessionStartTime)
    uriBuilder.queryParam("sessionEndTime", sessionEndTime)

    return uriBuilder
  }
}
