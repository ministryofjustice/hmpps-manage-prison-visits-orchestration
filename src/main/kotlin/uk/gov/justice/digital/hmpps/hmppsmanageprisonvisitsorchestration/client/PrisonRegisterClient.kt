package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.register.PrisonNameDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.register.PrisonRegisterContactDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.register.PrisonRegisterPrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.NotFoundException
import java.time.Duration
import java.util.*

@Component
class PrisonRegisterClient(
  @Qualifier("prisonRegisterWebClient") private val webClient: WebClient,
  @Value("\${prison-register.api.timeout:10s}") private val apiTimeout: Duration,
) {
  companion object {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getPrisonNames(): List<PrisonNameDto>? = webClient.get().uri("/prisons/names")
    .retrieve()
    .bodyToMono<List<PrisonNameDto>>()
    .block(apiTimeout)

  fun getPrisonAsMono(prisonCode: String): Mono<PrisonRegisterPrisonDto> {
    val uri = "/prisons/id/$prisonCode"
    return webClient.get().uri(uri)
      .retrieve()
      .bodyToMono<PrisonRegisterPrisonDto>()
      .onErrorResume { e ->
        if (!ClientUtils.isNotFoundError(e)) {
          logger.error("getPrison Failed for get request $uri")
          Mono.error(e)
        } else {
          logger.error("getPrison NOT_FOUND for get request $uri")
          Mono.error { NotFoundException("Prison with code - $prisonCode not found on prison-register") }
        }
      }
  }

  fun getPrisonAsMonoEmptyIfNotFound(prisonCode: String): Mono<Optional<PrisonRegisterPrisonDto>> {
    val uri = "/prisons/id/$prisonCode"
    return webClient.get().uri(uri)
      .retrieve()
      .bodyToMono<Optional<PrisonRegisterPrisonDto>>()
      .onErrorResume { e ->
        if (!ClientUtils.isNotFoundError(e)) {
          logger.error("getPrisonAsMonoEmptyIfError Failed for get request $uri")
          Mono.error(e)
        } else {
          logger.error("getPrisonAsMonoEmptyIfError NOT_FOUND for get request $uri")
          return@onErrorResume Mono.just(Optional.empty())
        }
      }
  }

  fun getPrison(prisonCode: String): PrisonRegisterPrisonDto = getPrisonAsMono(prisonCode)
    .blockOptional(apiTimeout).orElseThrow { NotFoundException("Prison with code - $prisonCode not found on prison-register") }

  fun prisonsByIds(prisonCodes: List<String>): List<PrisonRegisterPrisonDto>? {
    val uri = "/prisons/prisonsByIds"
    val prisonRequestDto = PrisonRequestDto(prisonIds = prisonCodes)

    return webClient
      .post()
      .uri(uri)
      .body(BodyInserters.fromValue(prisonRequestDto))
      .retrieve()
      .bodyToMono<List<PrisonRegisterPrisonDto>>()
      .block(apiTimeout)
  }

  fun getPrisonContactDetails(prisonCode: String): Optional<PrisonRegisterContactDetailsDto> {
    val uri = "/secure/prisons/id/$prisonCode/department/contact-details?departmentType=SOCIAL_VISIT"
    return webClient.get()
      .uri(uri)
      .retrieve()
      .bodyToMono<PrisonRegisterContactDetailsDto>()
      .onErrorResume { e ->
        if (!ClientUtils.isNotFoundError(e)) {
          logger.error("getPrisonContactDetails Failed for get request $uri", e)
          Mono.error(e)
        } else {
          logger.error("getPrisonContactDetails NOT_FOUND for prison $prisonCode contact details on get request $uri")
          Mono.empty()
        }
      }
      .blockOptional(apiTimeout)
  }

  data class PrisonRequestDto(
    val prisonIds: List<String>,
  )
}
