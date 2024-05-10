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
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.BookerPrisonerVisitorsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.BookerPrisonersDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.BookerReference
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.NotFoundException
import java.time.Duration

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

  fun getPrisonersForBooker(bookerReference: String): List<BookerPrisonersDto> {
    val uri = "/public/booker/$bookerReference/prisoners?active=true"
    return webClient.get()
      .uri(uri)
      .retrieve()
      .bodyToMono<List<BookerPrisonersDto>>()
      .onErrorResume {
          e ->
        if (!ClientUtils.isNotFoundError(e)) {
          logger.error("getPrisonersForBooker Failed for get request $uri")
          Mono.error(e)
        } else {
          logger.error("getPrisonersForBooker NOT_FOUND for get request $uri")
          Mono.error { NotFoundException("Prisoners for booker reference - $bookerReference not found on public-visits-booker-registry") }
        }
      }
      .blockOptional(apiTimeout).orElseThrow { NotFoundException("Prisoners for booker reference - $bookerReference not found on public-visits-booker-registry") }
  }

  fun getVisitorsForBookersAssociatedPrisoner(bookerReference: String, prisonerNumber: String): List<BookerPrisonerVisitorsDto> {
    val uri = "/public/booker/$bookerReference/prisoners/$prisonerNumber/visitors?active=true"
    return webClient.get()
      .uri(uri)
      .retrieve()
      .bodyToMono<List<BookerPrisonerVisitorsDto>>()
      .onErrorResume { e ->
        if (!ClientUtils.isNotFoundError(e)) {
          logger.error("getVisitorsForBookersAssociatedPrisoner Failed for get request $uri")
          Mono.error(e)
        } else {
          logger.error("getVisitorsForBookersAssociatedPrisoner NOT_FOUND for get request $uri")
          Mono.error { NotFoundException("Visitors for booker reference - $bookerReference and prisoner id - $prisonerNumber not found on public-visits-booker-registry") }
        }
      }
      .blockOptional(apiTimeout)
      .orElseThrow { NotFoundException("Visitors for booker reference - $bookerReference and prisoner id - $prisonerNumber not found on public-visits-booker-registry") }
  }
}
