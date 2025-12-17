package uk.gov.justice.digital.hmpps.orchestration.integration.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.orchestration.dto.RestPage
import uk.gov.justice.digital.hmpps.orchestration.dto.alerts.api.AlertResponseDto
import uk.gov.justice.digital.hmpps.orchestration.integration.mock.MockUtils.Companion.createJsonResponseBuilder
import uk.gov.justice.digital.hmpps.orchestration.integration.mock.MockUtils.Companion.getJsonString

class AlertsApiMockServer : WireMockServer(9000) {
  fun stubGetPrisonerAlertsMono(
    prisonerId: String,
    alertResponseDto: List<AlertResponseDto>? = null,
    httpStatus: HttpStatus = HttpStatus.NOT_FOUND,
  ) {
    val responseBuilder = createJsonResponseBuilder()

    stubFor(
      get("/prisoners/$prisonerId/alerts?isActive=true")
        .willReturn(
          if (alertResponseDto == null) {
            responseBuilder.withStatus(httpStatus.value())
          } else {
            val totalElements = alertResponseDto.size
            val restPage = RestPage(alertResponseDto, 1, 100, totalElements.toLong())
            responseBuilder
              .withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(restPage))
          },
        ),
    )
  }
}
