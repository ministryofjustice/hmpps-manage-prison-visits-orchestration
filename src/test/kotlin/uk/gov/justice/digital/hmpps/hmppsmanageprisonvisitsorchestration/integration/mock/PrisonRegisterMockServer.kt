package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.register.PrisonNameDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.register.PrisonRegisterContactDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.register.PrisonRegisterPrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.MockUtils.Companion.createJsonResponseBuilder
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.MockUtils.Companion.getJsonString

class PrisonRegisterMockServer : WireMockServer(8096) {
  fun stubGetPrisons(prisons: List<PrisonNameDto>?, httpStatus: HttpStatus = HttpStatus.NOT_FOUND) {
    val responseBuilder = createJsonResponseBuilder()

    stubFor(
      get("/prisons/names")
        .willReturn(
          if (prisons == null) {
            responseBuilder
              .withStatus(httpStatus.value())
          } else {
            responseBuilder
              .withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(prisons))
          },
        ),
    )
  }

  fun stubGetPrison(prisonCode: String, prisonDto: PrisonRegisterPrisonDto?, httpStatus: HttpStatus = HttpStatus.NOT_FOUND) {
    val responseBuilder = createJsonResponseBuilder()

    stubFor(
      get("/prisons/id/$prisonCode")
        .willReturn(
          if (prisonDto == null) {
            responseBuilder
              .withStatus(httpStatus.value())
          } else {
            responseBuilder
              .withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(prisonDto))
          },
        ),
    )
  }

  fun stubGetPrisonContactDetails(prisonCode: String, prisonRegisterContactDetailsDto: PrisonRegisterContactDetailsDto?, httpStatus: HttpStatus = HttpStatus.NOT_FOUND) {
    val responseBuilder = createJsonResponseBuilder()

    stubFor(
      get("/secure/prisons/id/$prisonCode/department/contact-details?departmentType=SOCIAL_VISIT")
        .willReturn(
          if (prisonRegisterContactDetailsDto == null) {
            responseBuilder
              .withStatus(httpStatus.value())
          } else {
            responseBuilder
              .withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(prisonRegisterContactDetailsDto))
          },
        ),
    )
  }
}
