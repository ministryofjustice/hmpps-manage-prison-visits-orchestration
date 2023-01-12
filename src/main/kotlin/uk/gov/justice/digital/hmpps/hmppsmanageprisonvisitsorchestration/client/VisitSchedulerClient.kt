package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.util.UriBuilder
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.ChangeVisitSlotRequestDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.ReserveVisitSlotDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.RestPage
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.SupportTypeDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.VisitCancelDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.VisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.VisitSearchDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.VisitSessionDto

import java.time.Duration
import java.util.Optional

@Component
class VisitSchedulerClient(

  @Qualifier("visitSchedulerWebClient") private val webClient: WebClient,
  @Value("\${visit-scheduler.api.timeout:10s}") val apiTimeout: Duration
) {
  private val pagedModel = object : ParameterizedTypeReference<RestPage<VisitDto>>() {}

  fun getVisitByReference(reference: String): VisitDto? {
    return webClient.get()
      .uri("/visits/$reference")
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<VisitDto>().block(apiTimeout)
  }

  fun getVisits(visitSearchDto: VisitSearchDto): RestPage<VisitDto>? {
    return webClient.get()
      .uri("/visits/search") {
        visitSearchUriBuilder(visitSearchDto, it).build()
      }
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono(pagedModel).block(apiTimeout)
  }

  fun reserveVisitSlot(reserveVisitSlotDto: ReserveVisitSlotDto): VisitDto? {
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

  fun changeBookedVisit(reference: String, reserveVisitSlotDto: ReserveVisitSlotDto): VisitDto? {
    return webClient.put()
      .uri("/visits/$reference/change")
      .body(BodyInserters.fromValue(reserveVisitSlotDto))
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<VisitDto>().block(apiTimeout)
  }

  fun cancelVisit(visitCancelDto: VisitCancelDto): VisitDto? {
    return webClient.put()
      .uri("/visits/${visitCancelDto.reference}/cancel")
      .body(BodyInserters.fromValue(visitCancelDto.outcome))
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

  private fun visitSearchUriBuilder(visitSearchDto: VisitSearchDto, uriBuilder: UriBuilder): UriBuilder {
    uriBuilder.queryParamIfPresent("prisonId", Optional.ofNullable(visitSearchDto.prisonCode))
    uriBuilder.queryParam("prisonerId", visitSearchDto.prisonerId)
    uriBuilder.queryParam("visitStatus", visitSearchDto.visitStatusList)
    uriBuilder.queryParamIfPresent("startDateTime", Optional.ofNullable(visitSearchDto.startDateTime))
    uriBuilder.queryParamIfPresent("endDateTime", Optional.ofNullable(visitSearchDto.endDateTime))
    uriBuilder.queryParamIfPresent("visitorId", Optional.ofNullable(visitSearchDto.visitorId))
    uriBuilder.queryParam("page", visitSearchDto.page)
    uriBuilder.queryParam("size", visitSearchDto.size)
    return uriBuilder
  }

  private fun visitSessionsUriBuilder(prisonId: String, prisonerId: String?, min: Long?, max: Long?, uriBuilder: UriBuilder): UriBuilder {
    uriBuilder.queryParam("prisonId", prisonId)
    uriBuilder.queryParamIfPresent("prisonerId", Optional.ofNullable(prisonerId))
    uriBuilder.queryParamIfPresent("min", Optional.ofNullable(min))
    uriBuilder.queryParamIfPresent("max", Optional.ofNullable(max))
    return uriBuilder
  }
}
