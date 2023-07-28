package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.ContactDto
import java.time.Duration

@Component
class PrisonerContactRegistryClient(
  @Qualifier("prisonerContactRegistryWebClient") private val webClient: WebClient,
  @Value("\${prisoner-contact.registry.timeout:10s}") private val apiTimeout: Duration,
) {
  companion object {
    const val SOCIAL_VISITOR_TYPE = "S"
  }

  fun getPrisonersSocialContactsWithoutAddress(prisonerId: String): List<ContactDto>? {
    return webClient.get().uri("/prisoners/$prisonerId/contacts") {
      it.queryParam("type", SOCIAL_VISITOR_TYPE)
        .queryParam("withAddress", false).build()
    }
      .retrieve()
      .bodyToMono<List<ContactDto>>()
      .block(apiTimeout)
  }
}
