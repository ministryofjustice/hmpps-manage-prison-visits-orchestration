package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.BookerPrisonerVisitorsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.BookerPrisonersDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.BookerReference

class PrisonVisitBookerRegistryMockServer(@Autowired private val objectMapper: ObjectMapper) : WireMockServer(8098) {

  fun stubBookerAuthorisation(bookerReference: BookerReference, httpStatus: HttpStatus = HttpStatus.OK) {
    val responseBuilder = createJsonResponseBuilder()

    stubFor(
      WireMock.put("/register/auth")
        .willReturn(
          responseBuilder.withStatus(httpStatus.value())
            .withBody(getJsonString(bookerReference)),
        ),
    )
  }

  fun stubGetBookersPrisoners(bookerReference: String, bookerPrisoners: List<BookerPrisonersDto>?, httpStatus: HttpStatus = HttpStatus.OK) {
    val responseBuilder = createJsonResponseBuilder()
    stubFor(
      WireMock.get("/public/booker/$bookerReference/prisoners?active=true")
        .willReturn(
          if (bookerPrisoners == null) {
            responseBuilder
              .withStatus(httpStatus.value())
          } else {
            responseBuilder
              .withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(bookerPrisoners))
          },
        ),
    )
  }

  fun stubGetBookersPrisonerVisitors(bookerReference: String, prisonerNumber: String, visitors: List<BookerPrisonerVisitorsDto>?, httpStatus: HttpStatus = HttpStatus.NOT_FOUND) {
    val responseBuilder = createJsonResponseBuilder()
    stubFor(
      WireMock.get("/public/booker/$bookerReference/prisoners/$prisonerNumber/visitors?active=true")
        .willReturn(
          if (visitors == null) {
            responseBuilder
              .withStatus(httpStatus.value())
          } else {
            responseBuilder
              .withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(visitors))
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
