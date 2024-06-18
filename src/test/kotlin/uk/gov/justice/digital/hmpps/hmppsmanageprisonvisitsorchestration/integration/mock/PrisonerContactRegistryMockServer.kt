package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.PrisonerContactDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.HasClosedRestrictionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.DateRange
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.MockUtils.Companion.createJsonResponseBuilder
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.MockUtils.Companion.getJsonString
import java.time.LocalDate

class PrisonerContactRegistryMockServer : WireMockServer(8095) {
  fun stubGetPrisonerContacts(
    prisonerId: String,
    withAddress: Boolean = false,
    approvedVisitorsOnly: Boolean = true,
    personId: Long? = null,
    hasDateOfBirth: Boolean? = null,
    notBannedBeforeDate: LocalDate? = null,
    contactsList: List<PrisonerContactDto>?,
    httpStatus: HttpStatus = HttpStatus.NOT_FOUND,
  ) {
    val responseBuilder = createJsonResponseBuilder()

    stubFor(
      get("/prisoners/$prisonerId/contacts/social?${getContactsQueryParams(personId, hasDateOfBirth, notBannedBeforeDate, withAddress, approvedVisitorsOnly)}")
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
    val uri = "/prisoners/$prisonerId/approved/social/contacts/restrictions/closed?visitors=$visitorIdsString"

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
      get("/prisoners/$prisonerId/approved/social/contacts/restrictions/banned/dateRange?visitors=$visitorIdsString&fromDate=${dateRange.fromDate}&toDate=${dateRange.toDate}")
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

  private fun getContactsQueryParams(
    personId: Long? = null,
    hasDateOfBirth: Boolean? = null,
    notBannedBeforeDate: LocalDate? = null,
    withAddress: Boolean? = null,
    approvedVisitorsOnly: Boolean? = null,
  ): String {
    val queryParams = ArrayList<String>()
    personId?.let {
      queryParams.add("id=$it")
    }
    hasDateOfBirth?.let {
      queryParams.add("hasDateOfBirth=$it")
    }
    notBannedBeforeDate?.let {
      queryParams.add("notBannedBeforeDate=$it")
    }
    withAddress?.let {
      queryParams.add("withAddress=$it")
    }
    approvedVisitorsOnly?.let {
      queryParams.add("approvedVisitorsOnly=$it")
    }

    return queryParams.joinToString("&")
  }
}
