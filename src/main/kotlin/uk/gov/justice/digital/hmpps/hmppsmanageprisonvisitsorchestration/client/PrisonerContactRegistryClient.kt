package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.PrisonerContactDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.HasClosedRestrictionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.DateRange
import java.net.URI
import java.time.Duration

@Component
class PrisonerContactRegistryClient(
  @Qualifier("prisonerContactRegistryWebClient") private val webClient: WebClient,
  @Value("\${prisoner-contact.registry.timeout:10s}") private val apiTimeout: Duration,
) {
  companion object {
    const val SOCIAL_VISITOR_TYPE = "S"
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getPrisonersSocialContacts(prisonerId: String, withAddress: Boolean): List<PrisonerContactDto>? {
    return webClient.get().uri("/prisoners/$prisonerId/contacts") {
      it.queryParam("type", SOCIAL_VISITOR_TYPE)
        .queryParam("withAddress", withAddress).build()
    }
      .retrieve()
      .bodyToMono<List<PrisonerContactDto>>()
      .block(apiTimeout)
  }

  fun getBannedRestrictionDateRange(prisonerId: String, visitors: List<Long>, prisonDateRange: DateRange): DateRange {
    val uri = "/prisoners/$prisonerId/contacts/restrictions/banned/dateRange"

    return webClient.get()
      .uri(uri) {
        getBannedRestrictionDateRangeParams(visitors, prisonDateRange, it)
      }
      .retrieve()
      .bodyToMono<DateRange>()
      .onErrorResume {
          e ->
        if (!isNotFoundError(e)) {
          LOG.error("getBannedRestrictionDateRage Failed get request $uri")
        } else {
          LOG.error("getBannedRestrictionDateRage NOT FOUND get request $uri")
        }
        Mono.error(e)
      }
      .block(apiTimeout)
  }

  fun doVisitorsHaveClosedRestrictions(prisonerId: String, visitors: List<Long>): Boolean {
    val uri = "/prisoners/$prisonerId/contacts/restrictions/closed"

    return webClient.get()
      .uri(uri) {
        getVisitorsHaveClosedRestrictionsParams(prisonerId, visitors, it)
      }
      .retrieve()
      .bodyToMono<HasClosedRestrictionDto>()
      .onErrorResume {
          e ->
        if (!isNotFoundError(e)) {
          LOG.error("doVisitorsHaveClosedRestrictions Failed get request $uri")
        } else {
          LOG.error("doVisitorsHaveClosedRestrictions NOT FOUND get request $uri")
        }
        Mono.error(e)
      }
      .block(apiTimeout).value
  }

  private fun getVisitorsHaveClosedRestrictionsParams(prisonerId: String, visitorIds: List<Long>, uriBuilder: UriBuilder): URI {
    uriBuilder.queryParam("prisonerId", prisonerId)
    uriBuilder.queryParam("visitors", visitorIds.joinToString(","))
    return uriBuilder.build()
  }

  private fun getBannedRestrictionDateRangeParams(visitorIds: List<Long>, dateRange: DateRange, uriBuilder: UriBuilder): URI {
    uriBuilder.queryParam("visitors", visitorIds.joinToString(","))
    uriBuilder.queryParam("fromDate", dateRange.fromDate)
    uriBuilder.queryParam("toDate", dateRange.toDate)
    return uriBuilder.build()
  }

  fun isNotFoundError(e: Throwable?) =
    e is WebClientResponseException && e.statusCode == NOT_FOUND
}
