package uk.gov.justice.digital.hmpps.prison.visits.orchestration.client

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.client.ClientUtils.Companion.isNotFoundError
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.prison.api.InmateDetailDto
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.prison.api.OffenderRestrictionsDto
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.exception.NotFoundException
import java.time.Duration

@Component
class PrisonApiClient(
  @param:Qualifier("prisonApiWebClient") private val webClient: WebClient,
  @param:Value("\${prison.api.timeout:10s}") private val apiTimeout: Duration,
) {

  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getInmateDetails(prisonerId: String): InmateDetailDto? = getInmateDetailsAsMono(prisonerId)
    .block(apiTimeout)

  fun getInmateDetailsAsMono(prisonerId: String): Mono<InmateDetailDto> = webClient.get()
    .uri("/api/offenders/$prisonerId")
    .retrieve()
    .bodyToMono()

  fun getPrisonerRestrictions(prisonerId: String): OffenderRestrictionsDto {
    val uri = "/api/offenders/$prisonerId/offender-restrictions"

    return getPrisonerRestrictionsAsMono(prisonerId)
      .onErrorResume { e ->
        if (!isNotFoundError(e)) {
          LOG.error("getOffenderRestrictions Failed get request $uri")
          Mono.error(e)
        } else {
          LOG.error("getOffenderRestrictions NOT FOUND get request $uri")
          Mono.error { NotFoundException("No Offender restrictions found for prisoner - $prisonerId on prison-api") }
        }
      }
      .blockOptional(apiTimeout).orElseThrow { NotFoundException("No Offender restrictions found for prisoner - $prisonerId on prison-api") }
  }

  fun getPrisonerRestrictionsAsMono(prisonerId: String): Mono<OffenderRestrictionsDto> {
    val uri = "/api/offenders/$prisonerId/offender-restrictions"
    return webClient.get()
      .uri(uri) {
        it.queryParam("activeRestrictionsOnly", true).build()
      }
      .retrieve()
      .bodyToMono<OffenderRestrictionsDto>()
  }
}
