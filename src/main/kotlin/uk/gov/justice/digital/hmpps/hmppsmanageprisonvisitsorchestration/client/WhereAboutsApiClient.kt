package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.whereabouts.ScheduledEventDto
import java.time.Duration
import java.time.LocalDate
import java.util.Optional

@Component
class WhereAboutsApiClient(
  @Qualifier("whereAboutsApiWebClient") private val webClient: WebClient,
  @Value("\${whereabouts.api.timeout:10s}") private val apiTimeout: Duration,
) {
  fun getEvents(prisonerId: String, fromDate: LocalDate?, toDate: LocalDate?): List<ScheduledEventDto>? {
    return webClient.get()
      .uri("/events/$prisonerId") {
        it.queryParamIfPresent("fromDate", Optional.ofNullable(fromDate))
          .queryParamIfPresent("toDate", Optional.ofNullable(toDate)).build()
      }
      .retrieve()
      .bodyToMono<List<ScheduledEventDto>>()
      .block(apiTimeout)
  }
}
