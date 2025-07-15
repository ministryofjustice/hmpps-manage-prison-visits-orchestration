package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonerContactRegistryClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonerContactRegistryClient.Companion.CONTACT_REGISTRY_REVIEW_RESTRICTIONS_DATE_RANGES_PATH
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.PrisonerContactDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.HasClosedRestrictionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.DateRange
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.MockUtils.Companion.createJsonResponseBuilder
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.MockUtils.Companion.getJsonString

class PrisonerContactRegistryMockServer : WireMockServer(8095) {
  fun stubGetPrisonerContacts(
    prisonerId: String,
    withAddress: Boolean = false,
    hasDateOfBirth: Boolean? = null,
    contactsList: List<PrisonerContactDto>?,
    httpStatus: HttpStatus = HttpStatus.NOT_FOUND,
  ) {
    val responseBuilder = createJsonResponseBuilder()

    stubFor(
      get("/v2/prisoners/$prisonerId/contacts/social?${getContactsQueryParams(hasDateOfBirth, withAddress)}")
        .willReturn(
          if (contactsList == null) {
            responseBuilder
              .withStatus(httpStatus.value())
          } else {
            responseBuilder
              .withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(contactsList))
          },
        ),
    )
  }

  fun stubGetApprovedPrisonerContacts(
    prisonerId: String,
    withAddress: Boolean = false,
    hasDateOfBirth: Boolean? = null,
    contactsList: List<PrisonerContactDto>?,
    httpStatus: HttpStatus = HttpStatus.NOT_FOUND,
  ) {
    val responseBuilder = createJsonResponseBuilder()

    stubFor(
      get("/v2/prisoners/$prisonerId/contacts/social/approved?${getContactsQueryParams(hasDateOfBirth, withAddress)}")
        .willReturn(
          if (contactsList == null) {
            responseBuilder
              .withStatus(httpStatus.value())
          } else {
            responseBuilder
              .withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(contactsList))
          },
        ),
    )
  }

  fun stubDoVisitorsHaveClosedRestrictions(prisonerId: String, visitorIds: List<Long>, result: Boolean? = null) {
    val responseBuilder = createJsonResponseBuilder()

    val visitorIdsString = visitorIds.joinToString(",")
    val uri = "/v2/prisoners/$prisonerId/contacts/social/approved/restrictions/closed?visitors=$visitorIdsString"

    stubFor(
      get(uri)
        .willReturn(
          if (result == null) {
            responseBuilder
              .withStatus(HttpStatus.NOT_FOUND.value())
          } else {
            responseBuilder
              .withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(HasClosedRestrictionDto(result)))
          },
        ),
    )
  }

  fun stubGetBannedRestrictionDateRage(
    prisonerId: String,
    visitorIds: List<Long>,
    dateRange: DateRange,
    result: DateRange? = dateRange,
  ) {
    val visitorIdsString = visitorIds.joinToString(",")

    val responseBuilder = createJsonResponseBuilder()
    stubFor(
      get("/v2/prisoners/$prisonerId/contacts/social/approved/restrictions/banned/dateRange?visitors=$visitorIdsString&fromDate=${dateRange.fromDate}&toDate=${dateRange.toDate}")
        .willReturn(
          if (result == null) {
            responseBuilder
              .withStatus(HttpStatus.NOT_FOUND.value())
          } else {
            responseBuilder
              .withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(result))
          },
        ),
    )
  }

  fun stubGetVisitorRestrictionsDateRanges(
    prisonerId: String,
    visitorIds: List<Long>,
    restrictionsForReview: List<String>,
    dateRange: DateRange,
    result: List<DateRange>?,
    httpStatus: HttpStatus = HttpStatus.NOT_FOUND,
  ) {
    val visitorRestrictionDateRangeRequestDto = PrisonerContactRegistryClient.VisitorRestrictionDateRangeRequestDto(
      prisonerId,
      visitorIds.map { it.toString() },
      restrictionsForReview,
      dateRange,
    )
    val responseBuilder = createJsonResponseBuilder()
    stubFor(
      post(CONTACT_REGISTRY_REVIEW_RESTRICTIONS_DATE_RANGES_PATH.replace("{prisonerId}", prisonerId))
        .withRequestBody(equalToJson(getJsonString(visitorRestrictionDateRangeRequestDto)))
        .willReturn(
          if (result == null) {
            responseBuilder
              .withStatus(httpStatus.value())
          } else {
            responseBuilder
              .withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(result))
          },
        ),
    )
  }

  private fun getContactsQueryParams(
    hasDateOfBirth: Boolean? = null,
    withAddress: Boolean? = null,
  ): String {
    val queryParams = ArrayList<String>()
    hasDateOfBirth?.let {
      queryParams.add("hasDateOfBirth=$it")
    }
    withAddress?.let {
      queryParams.add("withAddress=$it")
    }

    return queryParams.joinToString("&")
  }
}
