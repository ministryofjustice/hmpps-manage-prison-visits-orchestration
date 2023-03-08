package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.RestPage
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.ChangeVisitSlotRequestDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.ReserveVisitSlotDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionCapacityDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionScheduleDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SupportTypeDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitCancelDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSessionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.filter.VisitSearchRequestFilter
import java.time.LocalDate
import java.time.LocalTime
import java.util.Optional

@Component
class VisitSchedulerClient(

  @Qualifier("visitSchedulerWebClient") private val webClient: WebClient,
) {
  fun getVisitByReference(reference: String): Mono<VisitDto> {
    return webClient.get()
      .uri("/visits/$reference")
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono()
  }

  fun getVisits(visitSearchRequestFilter: VisitSearchRequestFilter): Mono<RestPage<VisitDto>> {
    return webClient.get()
      .uri("/visits/search") {
        visitSearchUriBuilder(visitSearchRequestFilter, it).build()
      }
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono()
  }

  fun reserveVisitSlot(reserveVisitSlotDto: ReserveVisitSlotDto): Mono<VisitDto> {
    return webClient.post()
      .uri("/visits/slot/reserve")
      .body(BodyInserters.fromValue(reserveVisitSlotDto))
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono()
  }

  fun bookVisitSlot(applicationReference: String): Mono<VisitDto> {
    return webClient.put()
      .uri("/visits/$applicationReference/book")
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono()
  }

  fun changeVisitSlot(applicationReference: String, changeVisitSlotRequestDto: ChangeVisitSlotRequestDto): Mono<VisitDto> {
    return webClient.put()
      .uri("/visits/$applicationReference/slot/change")
      .body(BodyInserters.fromValue(changeVisitSlotRequestDto))
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono()
  }

  fun changeBookedVisit(reference: String, reserveVisitSlotDto: ReserveVisitSlotDto): Mono<VisitDto> {
    return webClient.put()
      .uri("/visits/$reference/change")
      .body(BodyInserters.fromValue(reserveVisitSlotDto))
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono()
  }

  fun cancelVisit(visitCancelDto: VisitCancelDto): Mono<VisitDto> {
    return webClient.put()
      .uri("/visits/${visitCancelDto.reference}/cancel")
      .body(BodyInserters.fromValue(visitCancelDto.outcome))
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono()
  }

  fun getVisitSupport(): Mono<List<SupportTypeDto>> {
    return webClient.get()
      .uri("/visit-support")
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono()
  }

  fun getVisitSessions(prisonId: String, prisonerId: String?, min: Long?, max: Long?): Mono<List<VisitSessionDto>> {
    return webClient.get()
      .uri("/visit-sessions") {
        visitSessionsUriBuilder(prisonId, prisonerId, min, max, it).build()
      }
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono()
  }

  fun getSupportedPrisons(): Mono<List<String>> {
    return webClient.get()
      .uri("/config/prisons/supported")
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono()
  }

  fun getSessionCapacity(
    prisonCode: String,
    sessionDate: LocalDate,
    sessionStartTime: LocalTime,
    sessionEndTime: LocalTime
  ): Mono<SessionCapacityDto> {
    return webClient.get()
      .uri("/visit-sessions/capacity") {
        visitSessionsCapacityUriBuilder(prisonCode, sessionDate, sessionStartTime, sessionEndTime, it).build()
      }
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono()
  }

  fun getSessionSchedule(
    prisonCode: String,
    sessionDate: LocalDate
  ): Mono<List<SessionScheduleDto>> {
    return webClient.get()
      .uri("/visit-sessions/schedule") {
        it.queryParam("prisonId", prisonCode)
          .queryParam("date", sessionDate)
          .build()
      }
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono()
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
    uriBuilder: UriBuilder
  ): UriBuilder {
    uriBuilder.queryParam("prisonId", prisonCode)
    uriBuilder.queryParam("sessionDate", sessionDate)
    uriBuilder.queryParam("sessionStartTime", sessionStartTime)
    uriBuilder.queryParam("sessionEndTime", sessionEndTime)

    return uriBuilder
  }
}
