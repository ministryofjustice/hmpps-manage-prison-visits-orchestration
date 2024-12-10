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

  fun getPrisonerAlerts(prisonerId: String): RestPage<AlertResponseDto> {
    return getPrisonerAlertsAsMono(prisonerId).block(apiTimeout)
      ?: throw IllegalStateException("Unable to retrieve alerts for prisoner, possibly due to timeout $prisonerId")
  }

  fun getPrisonerAlertsAsMono(prisonerId: String): Mono<RestPage<AlertResponseDto>> {
    val uri = "/prisoners/$prisonerId/alerts"
    return webClient.get()
      .uri(uri)
      .retrieve()
      .bodyToMono<RestPage<AlertResponseDto>>()
      .doOnError { e ->
        LOG.error("getPrisonerAlertsAsMono Failed for get request $uri, exception - $e")
      }
  }
}
