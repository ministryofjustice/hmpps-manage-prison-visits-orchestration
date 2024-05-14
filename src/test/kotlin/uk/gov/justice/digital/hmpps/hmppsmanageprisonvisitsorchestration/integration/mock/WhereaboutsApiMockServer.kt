package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.whereabouts.ScheduledEventDto

class WhereaboutsApiMockServer(@Autowired private val objectMapper: ObjectMapper) : WireMockServer(8099) {

  fun stubGetEvents(prisonerNumber: String, events: List<ScheduledEventDto>?, httpStatus: HttpStatus = HttpStatus.OK) {
    val responseBuilder = createJsonResponseBuilder()

    stubFor(
      WireMock.put("/events/$prisonerNumber")
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

  private fun createJsonResponseBuilder(): ResponseDefinitionBuilder {
    return aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
  }

  private fun getJsonString(obj: Any): String {
    return objectMapper.writer().withDefaultPrettyPrinter().writeValueAsString(obj)
  }
}
