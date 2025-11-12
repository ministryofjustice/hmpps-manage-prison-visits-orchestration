package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.manage.users.UserDetailsDto

@Component
class ManageUsersApiClient(
  @param:Qualifier("manageUsersApiWebClient") private val webClient: WebClient,
) {

  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }
  fun getUserDetails(userName: String): Mono<UserDetailsDto> {
    return webClient.get()
      .uri("/users/$userName")
      .retrieve()
      .bodyToMono<UserDetailsDto>()
      .onErrorResume { e ->
        if (e is WebClientResponseException) {
          LOG.warn("Failed to acquire user information from hmpps-manage-users-api $userName ", e)
          return@onErrorResume Mono.just(UserDetailsDto(userName))
        }
        LOG.error("Failed to acquire user information from hmpps-manage-users-api $userName ", e)
        Mono.error(e)
      }
  }
}
