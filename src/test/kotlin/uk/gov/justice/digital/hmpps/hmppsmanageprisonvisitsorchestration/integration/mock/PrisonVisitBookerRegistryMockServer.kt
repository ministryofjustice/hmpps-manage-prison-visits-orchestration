package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PERMITTED_PRISONERS
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PERMITTED_VISITORS
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.BookerReference
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.PermittedPrisonerForBookerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.PermittedVisitorsForPermittedPrisonerBookerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.MockUtils.Companion.createJsonResponseBuilder
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.MockUtils.Companion.getJsonString

class PrisonVisitBookerRegistryMockServer : WireMockServer(8098) {

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

  fun stubGetBookersPrisoners(bookerReference: String, bookerPrisoners: List<PermittedPrisonerForBookerDto>?, httpStatus: HttpStatus = HttpStatus.OK) {
    val responseBuilder = createJsonResponseBuilder()
    stubFor(
      WireMock.get(PERMITTED_PRISONERS.replace("{bookerReference}", bookerReference) + "?active=true")
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

  fun stubGetBookersPrisonerVisitors(bookerReference: String, prisonerNumber: String, visitors: List<PermittedVisitorsForPermittedPrisonerBookerDto>?, httpStatus: HttpStatus = HttpStatus.NOT_FOUND) {
    val responseBuilder = createJsonResponseBuilder()
    stubFor(
      WireMock.get(PERMITTED_VISITORS.replace("{bookerReference}", bookerReference).replace("{prisonerId}", prisonerNumber) + "?active=true")
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
}
