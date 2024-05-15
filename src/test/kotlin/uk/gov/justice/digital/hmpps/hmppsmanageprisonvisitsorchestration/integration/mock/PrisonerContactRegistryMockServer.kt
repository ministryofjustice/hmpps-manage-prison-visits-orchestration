package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.PrisonerContactDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.HasClosedRestrictionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.DateRange
import java.time.LocalDate

class PrisonerContactRegistryMockServer(@Autowired private val objectMapper: ObjectMapper) : WireMockServer(8095) {
  fun stubGetPrisonerContacts(
    prisonerId: String,
    withAddress: Boolean = false,
    personId: Long? = null,
    hasDateOfBirth: Boolean? = null,
    notBannedBeforeDate: LocalDate? = null,
    contactsList: List<PrisonerContactDto>?,
    httpStatus: HttpStatus = HttpStatus.NOT_FOUND,
  ) {
    val responseBuilder = aResponse()
      .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)

    stubFor(
      get("/prisoners/$prisonerId/approved/social/contacts?${getContactsQueryParams(personId, hasDateOfBirth, notBannedBeforeDate, withAddress)}")
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
    val responseBuilder = aResponse()
      .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)

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

    val responseBuilder = aResponse()
      .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
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

  private fun getJsonString(obj: Any): String {
    return objectMapper.writer().withDefaultPrettyPrinter().writeValueAsString(obj)
  }

  private fun getContactsQueryParams(
    personId: Long? = null,
    hasDateOfBirth: Boolean? = null,
    notBannedBeforeDate: LocalDate? = null,
    withAddress: Boolean? = null,
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

    return queryParams.joinToString("&")
  }
}
