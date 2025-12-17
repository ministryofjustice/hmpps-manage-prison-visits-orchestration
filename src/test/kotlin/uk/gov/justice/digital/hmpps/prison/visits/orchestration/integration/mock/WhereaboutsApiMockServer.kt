package uk.gov.justice.digital.hmpps.prison.visits.orchestration.integration.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.whereabouts.ScheduledEventDto
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.integration.mock.MockUtils.Companion.createJsonResponseBuilder
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.integration.mock.MockUtils.Companion.getJsonString
import java.time.LocalDate

class WhereaboutsApiMockServer : WireMockServer(8099) {

  fun stubGetEvents(
    prisonerId: String,
    fromDate: LocalDate,
    toDate: LocalDate,
    events: List<ScheduledEventDto>?,
    httpStatus: HttpStatus = HttpStatus.NOT_FOUND,
  ) {
    val responseBuilder = createJsonResponseBuilder()
    stubFor(
      WireMock.get("/events/$prisonerId?fromDate=$fromDate&toDate=$toDate")
        .willReturn(
          if (events == null) {
            responseBuilder
              .withStatus(httpStatus.value())
          } else {
            responseBuilder
              .withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(events))
          },
        ),
    )
  }
}
