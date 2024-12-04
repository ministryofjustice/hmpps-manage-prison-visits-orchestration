package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.ClientUtils.Companion.isNotFoundError
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.NotFoundException
import java.time.Duration

@Component
class PrisonerSearchClient(
  @Qualifier("prisonerSearchWebClient") private val webClient: WebClient,
  @Value("\${prisoner.search.timeout:10s}") private val apiTimeout: Duration,
) {
  companion object {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getPrisonerById(prisonerId: String): PrisonerDto {
    return getPrisonerByIdAsMono(prisonerId)
      .onErrorResume {
          e ->
        if (!isNotFoundError(e)) {
          logger.error("Failed to get prisoner with id - $prisonerId on prisoner search")
          Mono.error(e)
        } else {
          logger.error("Prisoner with id - $prisonerId not found.")
          Mono.error { NotFoundException("Prisoner with id - $prisonerId not found on prisoner search") }
        }
      }
      .blockOptional(apiTimeout).orElseThrow { NotFoundException("Prisoner with id - $prisonerId not found on prisoner search") }
  }

  fun getPrisonerByIdAsMono(prisonerId: String): Mono<PrisonerDto> {
    return webClient.get().uri("/prisoner/$prisonerId")
      .retrieve()
      .bodyToMono()
  }

  fun getPrisonerByIdAsMonoEmptyIfNotFound(prisonerId: String): Mono<PrisonerDto> {
    return getPrisonerByIdAsMono(prisonerId).onErrorResume {
        e ->
      if (!isNotFoundError(e)) {
        logger.error("getPrisonerByIdAsMonoEmptyIfNotFound - Failed to get prisoner with id - $prisonerId on prisoner search")
        Mono.error(e)
      } else {
        logger.error("getPrisonerByIdAsMonoEmptyIfNotFound - Prisoner with id - $prisonerId not found.")
        Mono.empty()
      }
    }
  }
}
