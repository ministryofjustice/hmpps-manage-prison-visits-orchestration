package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.RestPage
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.InmateDetailDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.PrisonerBookingSummaryDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.VisitBalancesDto

class PrisonApiMockServer(@Autowired private val objectMapper: ObjectMapper) : WireMockServer(8093) {
  fun stubGetInmateDetails(inmateDetail: InmateDetailDto) {

    stubFor(
      get("/api/offenders/${inmateDetail.offenderNo}")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .withStatus(200)
            .withBody(
              getJsonString(inmateDetail)
            )
        )
    )
  }

  fun stubGetBookings(prisonId: String, offenderNo: String, prisonerBookingSummaryList: List<PrisonerBookingSummaryDto>) {
    val totalElements = prisonerBookingSummaryList.size
    val restPage = RestPage(prisonerBookingSummaryList, 1, 100, totalElements.toLong())
    stubFor(
      get("/api/bookings/v2?prisonId=$prisonId&offenderNo=$offenderNo&legalInfo=true")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .withStatus(200)
            .withBody(
              getJsonString(restPage)
            )
        )
    )
  }

  fun stubGetVisitBalances(offenderNo: String, visitBalances: VisitBalancesDto) {

    stubFor(
      get("/api/bookings/offenderNo/$offenderNo/visit/balances")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .withStatus(200)
            .withBody(
              getJsonString(visitBalances)
            )
        )
    )
  }

  private fun getJsonString(obj: Any): String {
    return objectMapper.writer().withDefaultPrettyPrinter().writeValueAsString(obj)
  }
}
