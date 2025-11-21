package uk.gov.justice.digital.hmpps.visits.orchestration.integration.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.visits.orchestration.client.GovUKHolidayClient.Companion.HOLIDAYS_JSON
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.govuk.holidays.HolidaysDto
import uk.gov.justice.digital.hmpps.visits.orchestration.integration.mock.MockUtils.Companion.createJsonResponseBuilder
import uk.gov.justice.digital.hmpps.visits.orchestration.integration.mock.MockUtils.Companion.getJsonString

class GovUkMockServer : WireMockServer(8100) {

  fun stubGetBankHolidays(
    holidaysDto: HolidaysDto?,
    httpStatus: HttpStatus = HttpStatus.NOT_FOUND,
  ) {
    val responseBuilder = createJsonResponseBuilder()
    stubFor(
      WireMock.get(HOLIDAYS_JSON)
        .willReturn(
          if (holidaysDto == null) {
            responseBuilder
              .withStatus(httpStatus.value())
          } else {
            responseBuilder
              .withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(holidaysDto))
          },
        ),
    )
  }
}
