package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.AuthDetailDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.BookerPrisonerVisitorsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.BookerPrisonersDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.BookerReference
import java.time.Duration

@Component
class PrisonVisitBookerRegistryClient(
  @Qualifier("prisonVisitBookerRegistryWebClient") private val webClient: WebClient,
  @Value("\${prison-visit-booker-registry.api.timeout:10s}") private val apiTimeout: Duration,
) {
  fun bookerAuthorisation(createBookerAuthDetailDto: AuthDetailDto): BookerReference? {
    return webClient.put()
      .uri("/register/auth")
      .body(BodyInserters.fromValue(createBookerAuthDetailDto))
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<BookerReference>().block(apiTimeout)
  }

  fun getPrisonersForBooker(bookerReference: String): List<BookerPrisonersDto>? {
    return webClient.get()
      .uri("/public/booker/$bookerReference/prisoners?active=true")
      .retrieve()
      .bodyToMono<List<BookerPrisonersDto>>().block(apiTimeout)
  }

  fun getVisitorsForBookersAssociatedPrisoner(bookerReference: String, prisonerNumber: String): List<BookerPrisonerVisitorsDto>? {
    return webClient.get()
      .uri("/public/booker/$bookerReference/prisoners/$prisonerNumber/visitors?active=true")
      .retrieve()
      .bodyToMono<List<BookerPrisonerVisitorsDto>>().block(apiTimeout)
  }
}
