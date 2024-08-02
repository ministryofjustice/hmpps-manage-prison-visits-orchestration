package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.RestPage
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.alerts.api.AlertDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.alerts.api.AlertResponseDto
import java.time.Duration

@Component
class AlertsApiClient(
  @Qualifier("alertsApiWebClient") private val webClient: WebClient,
  @Value("\${prison.api.timeout:10s}") private val apiTimeout: Duration,
) {

  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getPrisonerAlerts(prisonerId: String): RestPage<AlertResponseDto>? {
    return getPrisonerAlertsAsMono(prisonerId)
      .block(apiTimeout)
  }

  fun getPrisonerAlertsAsMono(prisonerId: String): Mono<RestPage<AlertResponseDto>> {
    return webClient.get()
      .uri("/prisoners/$prisonerId/alerts")
      .retrieve()
      .bodyToMono()
  }
}
