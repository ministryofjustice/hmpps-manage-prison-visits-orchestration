package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.AuthDetailDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.BookerReference
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.PermittedPrisonerForBookerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.PermittedVisitorsForPermittedPrisonerBookerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.NotFoundException
import java.time.Duration

const val PUBLIC_BOOKER_CONTROLLER_PATH: String = "/public/booker/{bookerReference}"
const val PERMITTED_PRISONERS: String = "$PUBLIC_BOOKER_CONTROLLER_PATH/permitted/prisoners"
const val PERMITTED_VISITORS: String = "$PERMITTED_PRISONERS/{prisonerId}/permitted/visitors"

@Component
class PrisonVisitBookerRegistryClient(
  @Qualifier("prisonVisitBookerRegistryWebClient") private val webClient: WebClient,
  @Value("\${prison-visit-booker-registry.api.timeout:10s}") private val apiTimeout: Duration,
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

  fun getPermittedVisitorsForPermittedPrisonerAndBooker(bookerReference: String): List<PermittedPrisonerForBookerDto> {
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
}
