package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.hmpps.auth.UserDetailsDto

@Component
class HmppsAuthClient(
  @Qualifier("hmppsAuthWebClient") private val webClient: WebClient,
) {
  fun getUserDetails(userName: String): Mono<UserDetailsDto> {
    return webClient.get()
      .uri("/api/user/$userName")
      .retrieve()
      .bodyToMono<UserDetailsDto>()
      .onErrorResume { e ->
        return@onErrorResume Mono.just(UserDetailsDto(userName))
      }
  }
}
