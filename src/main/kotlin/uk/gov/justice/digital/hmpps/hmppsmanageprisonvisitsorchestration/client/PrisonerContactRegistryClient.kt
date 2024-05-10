package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.PrisonerContactDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.NotFoundException
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
  ): List<PrisonerContactDto> {
    val uri = "/prisoners/$prisonerId/approved/social/contacts"
    return webClient.get().uri(uri) {
      getApprovedSocialContactsUriBuilder(personId, withAddress, hasDateOfBirth, notBannedBeforeDate, it).build()
    }
      .retrieve()
      .bodyToMono<List<PrisonerContactDto>>()
      .onErrorResume {
          e ->
        if (!ClientUtils.isNotFoundError(e)) {
          VisitSchedulerClient.LOG.error("getPrisonersSocialContacts Failed for get request $uri")
          Mono.error(e)
        } else {
          VisitSchedulerClient.LOG.error("getPrisonersSocialContacts NOT_FOUND for get request $uri")
          Mono.error { NotFoundException("Social Contacts for prisonerId - $prisonerId not found on prisoner-contact-registry") }
        }
      }
      .blockOptional(apiTimeout).orElseThrow { NotFoundException("Social Contacts for prisonerId - $prisonerId not found on prisoner-contact-registry") }
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
