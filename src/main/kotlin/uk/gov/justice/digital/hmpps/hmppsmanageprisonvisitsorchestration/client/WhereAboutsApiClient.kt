package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.whereabouts.ScheduledEventDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.NotFoundException
import java.time.Duration
import java.time.LocalDate

@Component
class WhereAboutsApiClient(
  @Qualifier("whereAboutsApiWebClient") private val webClient: WebClient,
  @Value("\${whereabouts.api.timeout:10s}") private val apiTimeout: Duration,
) {
  fun getEvents(prisonerId: String, fromDate: LocalDate, toDate: LocalDate): List<ScheduledEventDto> {
    val uri = "/events/$prisonerId"
    return webClient.get()
      .uri(uri) {
        it.queryParam("fromDate", fromDate)
          .queryParam("toDate", toDate).build()
      }
      .retrieve()
      .bodyToMono<List<ScheduledEventDto>>()
      .onErrorResume { e ->
        if (!ClientUtils.isNotFoundError(e)) {
          VisitSchedulerClient.LOG.error("getEvents Failed for get request $uri")
          Mono.error(e)
        } else {
          VisitSchedulerClient.LOG.error("getEvents NOT_FOUND for get request $uri")
          Mono.error { NotFoundException("No Events found for Prisoner - $prisonerId on whereabouts-api") }
        }
      }
      .blockOptional(apiTimeout).orElse(emptyList())
  }
}
