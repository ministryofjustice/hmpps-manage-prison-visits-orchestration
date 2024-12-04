package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.ClientUtils.Companion.isUnprocessableEntityError
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitSchedulerClient.Companion.LOG
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config.PrisonerValidationErrorResponse
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.AuthDetailDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.BookerReference
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.PermittedPrisonerForBookerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.PermittedVisitorsForPermittedPrisonerBookerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.BookerPrisonerValidationException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.NotFoundException
import java.time.Duration

const val PUBLIC_BOOKER_CONTROLLER_PATH: String = "/public/booker/{bookerReference}"
const val PERMITTED_PRISONERS: String = "$PUBLIC_BOOKER_CONTROLLER_PATH/permitted/prisoners"
const val PERMITTED_VISITORS: String = "$PERMITTED_PRISONERS/{prisonerId}/permitted/visitors"
const val VALIDATE_PRISONER: String = "$PERMITTED_PRISONERS/{prisonerId}/validate"

@Component
class PrisonVisitBookerRegistryClient(
  @Qualifier("prisonVisitBookerRegistryWebClient") private val webClient: WebClient,
  @Value("\${prison-visit-booker-registry.api.timeout:10s}") private val apiTimeout: Duration,
  val objectMapper: ObjectMapper,
) {
  companion object {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun bookerAuthorisation(createBookerAuthDetailDto: AuthDetailDto): BookerReference? {
    return webClient.put()
      .uri("/register/auth")
      .body(BodyInserters.fromValue(createBookerAuthDetailDto))
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<BookerReference>().block(apiTimeout)
  }

  fun getPermittedPrisonersForBooker(bookerReference: String): List<PermittedPrisonerForBookerDto> {
    val uri = PERMITTED_PRISONERS.replace("{bookerReference}", bookerReference) + "?active=true"
    return webClient.get()
      .uri(uri)
      .retrieve()
      .bodyToMono<List<PermittedPrisonerForBookerDto>>()
      .onErrorResume {
          e ->
        if (!ClientUtils.isNotFoundError(e)) {
          logger.error("getPermittedPrisonersForBooker Failed for get request $uri")
          Mono.error(e)
        } else {
          logger.error("getPermittedPrisonersForBooker NOT_FOUND for get request $uri")
          Mono.error { NotFoundException("Prisoners for booker reference - $bookerReference not found on public-visits-booker-registry") }
        }
      }
      .blockOptional(apiTimeout).orElseThrow { NotFoundException("Prisoners for booker reference - $bookerReference not found on public-visits-booker-registry") }
  }

  fun getPermittedVisitorsForBookersAssociatedPrisoner(bookerReference: String, prisonerNumber: String): List<PermittedVisitorsForPermittedPrisonerBookerDto> {
    val uri = PERMITTED_VISITORS.replace("{bookerReference}", bookerReference).replace("{prisonerId}", prisonerNumber) + "?active=true"
    return webClient.get()
      .uri(uri)
      .retrieve()
      .bodyToMono<List<PermittedVisitorsForPermittedPrisonerBookerDto>>()
      .onErrorResume { e ->
        if (!ClientUtils.isNotFoundError(e)) {
          logger.error("getPermittedVisitorsForBookersAssociatedPrisoner Failed for get request $uri")
          Mono.error(e)
        } else {
          logger.error("getPermittedVisitorsForBookersAssociatedPrisoner NOT_FOUND for get request $uri")
          Mono.error { NotFoundException("Visitors for booker reference - $bookerReference and prisoner id - $prisonerNumber not found on public-visits-booker-registry") }
        }
      }
      .blockOptional(apiTimeout)
      .orElseThrow { NotFoundException("Permitted visitors for booker reference - $bookerReference and prisoner id - $prisonerNumber not found on public-visits-booker-registry") }
  }

  fun validatePrisoner(bookerReference: String, prisonerNumber: String) {
    val uri = VALIDATE_PRISONER.replace("{bookerReference}", bookerReference).replace("{prisonerId}", prisonerNumber)
    webClient.get()
      .uri(uri).retrieve().toBodilessEntity().onErrorResume {
          e ->
        if (isUnprocessableEntityError(e)) {
          val exception = getPrisonerValidationErrorResponse(e)
          Mono.error(exception)
        } else {
          Mono.error(e)
        }
      }.block(apiTimeout)
  }

  private fun getPrisonerValidationErrorResponse(e: Throwable): Throwable {
    if (e is WebClientResponseException && isUnprocessableEntityError(e)) {
      try {
        val errorResponse = objectMapper.readValue(e.responseBodyAsString, PrisonerValidationErrorResponse::class.java)
        return BookerPrisonerValidationException(errorResponse.validationErrors)
      } catch (jsonProcessingException: Exception) {
        LOG.error("An error occurred processing the booker prisoner validation error response - ${e.stackTraceToString()}")
        throw jsonProcessingException
      }
    }

    return e
  }
}
