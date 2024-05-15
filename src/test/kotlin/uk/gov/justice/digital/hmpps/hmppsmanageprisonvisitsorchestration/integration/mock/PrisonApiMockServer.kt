package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.RestPage
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.InmateDetailDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.OffenderRestrictionsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.PrisonerBookingSummaryDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.VisitBalancesDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.MockUtils.Companion.createJsonResponseBuilder
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.MockUtils.Companion.getJsonString

class PrisonApiMockServer : WireMockServer(8093) {
  fun stubGetInmateDetails(prisonerId: String, inmateDetail: InmateDetailDto?) {
    val responseBuilder = createJsonResponseBuilder()

    stubFor(
      get("/api/offenders/$prisonerId")
        .willReturn(
          if (inmateDetail == null) {
            responseBuilder
              .withStatus(HttpStatus.NOT_FOUND.value())
          } else {
            responseBuilder
              .withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(inmateDetail))
          },
        ),
    )
  }

  fun stubGetBookings(prisonId: String, prisonerId: String, prisonerBookingSummaryList: List<PrisonerBookingSummaryDto>) {
    val totalElements = prisonerBookingSummaryList.size
    val restPage = RestPage(prisonerBookingSummaryList, 1, 100, totalElements.toLong())
    stubFor(
      get("/api/bookings/v2?prisonId=$prisonId&offenderNo=$prisonerId&legalInfo=true")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .withStatus(200)
            .withBody(
              getJsonString(restPage),
            ),
        ),
    )
  }

  fun stubGetVisitBalances(prisonerId: String, visitBalances: VisitBalancesDto?) {
    val responseBuilder = aResponse()
      .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)

    stubFor(
      get("/api/bookings/offenderNo/$prisonerId/visit/balances")
        .willReturn(
          if (visitBalances == null) {
            responseBuilder
              .withStatus(HttpStatus.NOT_FOUND.value())
          } else {
            responseBuilder
              .withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(visitBalances))
          },
        ),
    )
  }

  fun stubGetPrisonerRestrictions(prisonerId: String, offenderRestrictionsDto: OffenderRestrictionsDto? = null) {
    val responseBuilder = createJsonResponseBuilder()

    stubFor(
      get("/api/offenders/$prisonerId/offender-restrictions?activeRestrictionsOnly=true")
        .willReturn(
          offenderRestrictionsDto?.let {
            responseBuilder
              .withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(it))
          } ?: run {
            responseBuilder
              .withStatus(HttpStatus.NOT_FOUND.value())
          },
        ),
    )
  }
}
