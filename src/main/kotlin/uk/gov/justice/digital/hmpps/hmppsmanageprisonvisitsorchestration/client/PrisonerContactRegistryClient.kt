package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.ClientUtils.Companion.isNotFoundError
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.PrisonerContactDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.HasClosedRestrictionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.DateRange
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.DateRangeNotFoundException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.NotFoundException
import java.net.URI
import java.time.Duration
import java.util.*

@Component
class PrisonerContactRegistryClient(
  @param:Qualifier("prisonerContactRegistryWebClient")
  private val webClient: WebClient,
  @param:Value("\${prisoner-contact.registry.timeout:10s}")
  private val apiTimeout: Duration,
) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
    const val CONTACT_REGISTRY_CONTACTS_PATH: String = "/v2/prisoners/{prisonerId}/contacts"
    const val CONTACT_REGISTRY_APPROVED_SOCIAL_CONTACTS_PATH: String = "$CONTACT_REGISTRY_CONTACTS_PATH/social/approved"
    const val CONTACT_REGISTRY_SOCIAL_CONTACTS_PATH: String = "$CONTACT_REGISTRY_CONTACTS_PATH/social"
    const val CONTACT_REGISTRY_APPROVED_SOCIAL_CONTACTS_RESTRICTIONS_PATH: String = "$CONTACT_REGISTRY_APPROVED_SOCIAL_CONTACTS_PATH/restrictions"
    const val CONTACT_REGISTRY_BANNED_RESTRICTION_DATE_RANGE_PATH: String = "$CONTACT_REGISTRY_APPROVED_SOCIAL_CONTACTS_RESTRICTIONS_PATH/banned/dateRange"
    const val CONTACT_REGISTRY_HAS_CLOSED_RESTRICTIONS_PATH: String = "$CONTACT_REGISTRY_APPROVED_SOCIAL_CONTACTS_RESTRICTIONS_PATH/closed"
    const val CONTACT_REGISTRY_REVIEW_RESTRICTIONS_DATE_RANGES_PATH: String = "$CONTACT_REGISTRY_APPROVED_SOCIAL_CONTACTS_RESTRICTIONS_PATH/visit-request/date-ranges"
  }

  fun getPrisonersApprovedSocialContacts(
    prisonerId: String,
    withAddress: Boolean,
    hasDateOfBirth: Boolean? = null,
  ): List<PrisonerContactDto> {
    val uri = CONTACT_REGISTRY_APPROVED_SOCIAL_CONTACTS_PATH.replace("{prisonerId}", prisonerId)
    return webClient.get().uri(uri) {
      getSocialContactsUriBuilder(withAddress, hasDateOfBirth, it).build()
    }.retrieve()
      .bodyToMono<List<PrisonerContactDto>>()
      .onErrorResume { e ->
        if (!isNotFoundError(e)) {
          LOG.error("getPrisonersApprovedSocialContacts Failed for get request $uri")
          Mono.error(e)
        } else {
          LOG.error("getPrisonersApprovedSocialContacts NOT_FOUND for get request $uri")
          Mono.error { NotFoundException("Approved social contacts for prisonerId - $prisonerId not found on prisoner-contact-registry") }
        }
      }
      .blockOptional(apiTimeout).orElseThrow { NotFoundException("Approved social contacts for prisonerId - $prisonerId not found on prisoner-contact-registry") }
  }

  fun getPrisonersSocialContacts(
    prisonerId: String,
    withAddress: Boolean,
    hasDateOfBirth: Boolean? = null,
  ): List<PrisonerContactDto> {
    val uri = CONTACT_REGISTRY_SOCIAL_CONTACTS_PATH.replace("{prisonerId}", prisonerId)
    return getPrisonersSocialContactsAsMono(prisonerId, withAddress, hasDateOfBirth)
      .onErrorResume { e ->
        if (!isNotFoundError(e)) {
          LOG.error("getPrisonersSocialContacts Failed for get request $uri")
          Mono.error(e)
        } else {
          LOG.error("getPrisonersSocialContacts NOT_FOUND for get request $uri")
          Mono.error { NotFoundException("Social Contacts for prisonerId - $prisonerId not found on prisoner-contact-registry") }
        }
      }
      .blockOptional(apiTimeout).orElseThrow { NotFoundException("Social Contacts for prisonerId - $prisonerId not found on prisoner-contact-registry") }
  }

  fun getPrisonersSocialContactsAsMono(
    prisonerId: String,
    withAddress: Boolean,
    hasDateOfBirth: Boolean? = null,
  ): Mono<List<PrisonerContactDto>> {
    val uri = CONTACT_REGISTRY_SOCIAL_CONTACTS_PATH.replace("{prisonerId}", prisonerId)
    return webClient.get().uri(uri) {
      getSocialContactsUriBuilder(withAddress, hasDateOfBirth, it).build()
    }
      .retrieve()
      .bodyToMono<List<PrisonerContactDto>>()
  }

  private fun getSocialContactsUriBuilder(
    withAddress: Boolean,
    hasDateOfBirth: Boolean? = null,
    uriBuilder: UriBuilder,
  ): UriBuilder {
    uriBuilder.queryParamIfPresent("hasDateOfBirth", Optional.ofNullable(hasDateOfBirth))
    uriBuilder.queryParam("withAddress", withAddress)
    return uriBuilder
  }

  fun getBannedRestrictionDateRange(prisonerId: String, visitors: List<Long>, prisonDateRange: DateRange): DateRange {
    val uri = CONTACT_REGISTRY_BANNED_RESTRICTION_DATE_RANGE_PATH.replace("{prisonerId}", prisonerId)

    return webClient.get()
      .uri(uri) {
        getBannedRestrictionDateRangeParams(visitors, prisonDateRange, it)
      }
      .retrieve()
      .bodyToMono<DateRange>()
      .onErrorResume { e ->
        if (!isNotFoundError(e)) {
          LOG.error("getBannedRestrictionDateRage Failed get request $uri")
          Mono.error(e)
        } else {
          val message = "getBannedRestrictionDateRage NOT FOUND get request $uri"
          LOG.error("getBannedRestrictionDateRage NOT FOUND get request $uri")
          Mono.error(DateRangeNotFoundException(message, e))
        }
      }
      .blockOptional(apiTimeout).get()
  }

  fun doVisitorsHaveClosedRestrictions(prisonerId: String, visitors: List<Long>): Boolean {
    val uri = CONTACT_REGISTRY_HAS_CLOSED_RESTRICTIONS_PATH.replace("{prisonerId}", prisonerId)

    return webClient.get()
      .uri(uri) {
        getVisitorsHaveClosedRestrictionsParams(visitors, it)
      }
      .retrieve()
      .bodyToMono<HasClosedRestrictionDto>()
      .onErrorResume { e ->
        if (!isNotFoundError(e)) {
          LOG.error("doVisitorsHaveClosedRestrictions Failed get request $uri")
        } else {
          LOG.error("doVisitorsHaveClosedRestrictions NOT FOUND get request $uri")
        }
        Mono.error(e)
      }
      .blockOptional(apiTimeout).get().value
  }

  fun getVisitorRestrictionDateRanges(prisonerId: String, visitors: List<Long>, restrictionCodesForReview: List<String>, sessionDateRange: DateRange): List<DateRange>? {
    val uri = CONTACT_REGISTRY_REVIEW_RESTRICTIONS_DATE_RANGES_PATH.replace("{prisonerId}", prisonerId)
    val visitorRestrictionDateRangeRequestDto = VisitorRestrictionDateRangeRequestDto(
      prisonerId = prisonerId,
      visitors = visitors.map { it.toString() },
      restrictionCodesForReview = restrictionCodesForReview,
      sessionDateRange = sessionDateRange,
    )

    return webClient.post()
      .uri(uri)
      .body(BodyInserters.fromValue(visitorRestrictionDateRangeRequestDto))
      .retrieve()
      .bodyToMono<List<DateRange>>()
      .onErrorResume { e ->
        if (!isNotFoundError(e)) {
          LOG.error("getVisitorRestrictionDateRanges failed for request $uri")
        } else {
          LOG.error("getVisitorRestrictionDateRanges failed with NOT_FOUND error for request $uri")
        }
        return@onErrorResume Mono.justOrEmpty(null)
      }.block(apiTimeout)
  }

  private fun getVisitorsHaveClosedRestrictionsParams(visitorIds: List<Long>, uriBuilder: UriBuilder): URI {
    uriBuilder.queryParam("visitors", visitorIds.joinToString(","))
    return uriBuilder.build()
  }

  private fun getBannedRestrictionDateRangeParams(visitorIds: List<Long>, dateRange: DateRange, uriBuilder: UriBuilder): URI {
    uriBuilder.queryParam("visitors", visitorIds.joinToString(","))
    uriBuilder.queryParam("fromDate", dateRange.fromDate)
    uriBuilder.queryParam("toDate", dateRange.toDate)
    return uriBuilder.build()
  }

  data class VisitorRestrictionDateRangeRequestDto(
    val prisonerId: String,
    val visitors: List<String>,
    val restrictionCodesForReview: List<String>,
    val sessionDateRange: DateRange,
  )
}
