package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.register.PrisonDto
import java.time.Duration

@Component
class PrisonRegisterClient(
  @Qualifier("prisonRegisterWebClient") private val webClient: WebClient,
  @Value("\${prisoner.offender.search.timeout:10s}") private val apiTimeout: Duration
) {
  fun getPrisons(): List<PrisonDto>? {
    return webClient.get().uri("/prisons")
      .retrieve()
      .bodyToMono<List<PrisonDto>>()
      .block(apiTimeout)
  }

  fun getPrison(
    prisonId: String
  ): PrisonDto? {
    return webClient.get().uri("/prisons/id/$prisonId")
      .retrieve()
      .bodyToMono<PrisonDto>()
      .block(apiTimeout)
  }
}
