package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerDto

class PrisonOffenderSearchMockServer(@Autowired private val objectMapper: ObjectMapper) : WireMockServer(8094) {
  fun stubGetPrisonerById(prisonerId: String, prisoner: PrisonerDto?) {
    val responseBuilder = aResponse()
      .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)

    stubFor(
      get("/prisoner/$prisonerId")
        .willReturn(
          if (prisoner == null) {
            responseBuilder
              .withStatus(HttpStatus.NOT_FOUND.value())
          } else {
            responseBuilder
              .withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(prisoner))
          },
        ),
    )
  }

  private fun getJsonString(obj: Any): String {
    return objectMapper.writer().withDefaultPrettyPrinter().writeValueAsString(obj)
  }
}
