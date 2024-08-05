package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.RestPage
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.alerts.api.AlertResponseDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.MockUtils.Companion.getJsonString

class AlertsApiMockServer : WireMockServer(9000) {
  fun stubGetPrisonerAlerts(prisonerId: String, alertResponseDto: List<AlertResponseDto>) {
    val totalElements = alertResponseDto.size
    val restPage = RestPage(alertResponseDto, 1, 100, totalElements.toLong())
    stubFor(
      get("/prisoners/$prisonerId/alerts")
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
}
