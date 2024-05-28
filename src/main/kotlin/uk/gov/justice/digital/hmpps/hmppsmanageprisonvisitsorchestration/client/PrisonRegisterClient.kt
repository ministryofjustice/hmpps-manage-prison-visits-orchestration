package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.register.PrisonNameDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.register.PrisonRegisterPrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.NotFoundException
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

  fun getPrison(prisonCode: String): PrisonRegisterPrisonDto {
    val uri = "/prisons/id/$prisonCode"
    return webClient.get().uri(uri)
      .retrieve()
      .bodyToMono<PrisonRegisterPrisonDto>()
      .onErrorResume {
          e ->
        if (!ClientUtils.isNotFoundError(e)) {
          PrisonVisitBookerRegistryClient.logger.error("getPrison Failed for get request $uri")
          Mono.error(e)
        } else {
          PrisonVisitBookerRegistryClient.logger.error("getPrison NOT_FOUND for get request $uri")
          Mono.error { NotFoundException("Prison with code - $prisonCode not found on prison-register") }
        }
      }
      .blockOptional(apiTimeout).orElseThrow { NotFoundException("Prison with code - $prisonCode not found on prison-register") }
  }
}
