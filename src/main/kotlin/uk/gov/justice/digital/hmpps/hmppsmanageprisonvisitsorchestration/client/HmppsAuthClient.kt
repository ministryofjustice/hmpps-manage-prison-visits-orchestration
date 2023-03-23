package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.hmpps.auth.UserDetails
import java.util.Optional

@Component
class HmppsAuthClient(
  @Qualifier("hmppsAuthWebClient") private val webClient: WebClient,
) {
  fun getUserDetails(userName: String): Mono<Optional<UserDetails>> {
    return webClient.get()
      .uri("/api/user/$userName")
      .retrieve()
      .bodyToMono<Optional<UserDetails>>()
      .onErrorResume { e ->
        if (e is WebClientResponseException && e.statusCode == HttpStatus.NOT_FOUND) {
          // return an Optional.empty element if 404 is thrown
          return@onErrorResume Mono.just(Optional.empty())
        } else {
          Mono.error(e)
        }
      }
  }
}
