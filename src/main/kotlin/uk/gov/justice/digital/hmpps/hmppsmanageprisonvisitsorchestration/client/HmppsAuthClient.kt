package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.hmpps.auth.UserDetails
import java.time.Duration

@Component
class HmppsAuthClient(
  @Qualifier("hmppsAuthWebClient") private val webClient: WebClient,
  @Value("\${hmpps.auth.timeout:10s}") private val apiTimeout: Duration,
) {
  fun getUserDetails(userName: String): UserDetails? {
    return webClient.get().uri("/api/user/$userName")
      .retrieve()
      .bodyToMono<UserDetails>()
      .block(apiTimeout)
  }
}
