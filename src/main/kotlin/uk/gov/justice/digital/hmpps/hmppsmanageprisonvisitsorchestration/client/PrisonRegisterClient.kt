package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.register.PrisonNameDto
import java.time.Duration

@Component
class PrisonRegisterClient(
  @Qualifier("prisonRegisterWebClient") private val webClient: WebClient,
  @Value("\${prison-register.api.timeout:10s}") private val apiTimeout: Duration,
) {
  fun getPrisonNames(): List<PrisonNameDto>? {
    return webClient.get().uri("/prisons/names")
      .retrieve()
      .bodyToMono<List<PrisonNameDto>>()
      .block(apiTimeout)
  }
}
