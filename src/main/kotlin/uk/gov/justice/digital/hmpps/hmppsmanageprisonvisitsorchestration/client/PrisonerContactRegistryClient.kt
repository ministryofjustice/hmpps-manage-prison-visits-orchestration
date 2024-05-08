package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.util.UriBuilder
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.PrisonerContactDto
import java.time.Duration
import java.time.LocalDate
import java.util.*

@Component
class PrisonerContactRegistryClient(
  @Qualifier("prisonerContactRegistryWebClient") private val webClient: WebClient,
  @Value("\${prisoner-contact.registry.timeout:10s}") private val apiTimeout: Duration,
) {
  fun getPrisonersSocialContacts(
    prisonerId: String,
    withAddress: Boolean,
    personId: Long? = null,
    hasDateOfBirth: Boolean? = null,
    notBannedBeforeDate: LocalDate? = null,
  ): List<PrisonerContactDto>? {
    return webClient.get().uri("/prisoners/$prisonerId/approved/social/contacts") {
      getApprovedSocialContactsUriBuilder(personId, withAddress, hasDateOfBirth, notBannedBeforeDate, it).build()
    }
      .retrieve()
      .bodyToMono<List<PrisonerContactDto>>()
      .block(apiTimeout)
  }

  private fun getApprovedSocialContactsUriBuilder(
    personId: Long?,
    withAddress: Boolean,
    hasDateOfBirth: Boolean? = null,
    notBannedBeforeDate: LocalDate? = null,
    uriBuilder: UriBuilder,
  ): UriBuilder {
    uriBuilder.queryParamIfPresent("id", Optional.ofNullable(personId))
    uriBuilder.queryParamIfPresent("hasDateOfBirth", Optional.ofNullable(hasDateOfBirth))
    uriBuilder.queryParamIfPresent("notBannedBeforeDate", Optional.ofNullable(notBannedBeforeDate))
    uriBuilder.queryParam("withAddress", withAddress)
    return uriBuilder
  }
}
