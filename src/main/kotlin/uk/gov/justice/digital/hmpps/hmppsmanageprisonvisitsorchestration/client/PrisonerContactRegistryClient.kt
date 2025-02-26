package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
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
import java.util.Optional

@Component
class PrisonerContactRegistryClient(
  @Qualifier("prisonerContactRegistryWebClient") private val webClient: WebClient,
  @Value("\${prisoner-contact.registry.timeout:10s}") private val apiTimeout: Duration,
) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getPrisonersSocialContacts(
    prisonerId: String,
    withAddress: Boolean,
    approvedVisitorsOnly: Boolean,
    personId: Long? = null,
    hasDateOfBirth: Boolean? = null,
  ): List<PrisonerContactDto> {
    val uri = "/prisoners/$prisonerId/contacts/social"
    return getPrisonersSocialContactsAsMono(prisonerId, withAddress, approvedVisitorsOnly, personId, hasDateOfBirth)
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
    approvedVisitorsOnly: Boolean,
    personId: Long? = null,
    hasDateOfBirth: Boolean? = null,
  ): Mono<List<PrisonerContactDto>> {
    val uri = "/prisoners/$prisonerId/contacts/social"
    return webClient.get().uri(uri) {
      getSocialContactsUriBuilder(personId, withAddress, hasDateOfBirth, approvedVisitorsOnly, it).build()
    }
      .retrieve()
      .bodyToMono<List<PrisonerContactDto>>()
  }

  private fun getSocialContactsUriBuilder(
    personId: Long?,
    withAddress: Boolean,
    hasDateOfBirth: Boolean? = null,
    approvedVisitorsOnly: Boolean,
    uriBuilder: UriBuilder,
  ): UriBuilder {
    uriBuilder.queryParamIfPresent("id", Optional.ofNullable(personId))
    uriBuilder.queryParamIfPresent("hasDateOfBirth", Optional.ofNullable(hasDateOfBirth))
    uriBuilder.queryParam("withAddress", withAddress)
    uriBuilder.queryParam("approvedVisitorsOnly", approvedVisitorsOnly)
    return uriBuilder
  }

  fun getBannedRestrictionDateRange(prisonerId: String, visitors: List<Long>, prisonDateRange: DateRange): DateRange {
    val uri = "/prisoners/$prisonerId/approved/social/contacts/restrictions/banned/dateRange"

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
    val uri = "/prisoners/$prisonerId/approved/social/contacts/restrictions/closed"

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
}
