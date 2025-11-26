package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.RestPage
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.MockUtils.Companion.createJsonResponseBuilder
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.MockUtils.Companion.getJsonString

class PrisonOffenderSearchMockServer : WireMockServer(8094) {
  fun stubGetPrisonerById(prisonerId: String, prisoner: PrisonerDto?, httpStatus: HttpStatus = HttpStatus.NOT_FOUND) {
    val responseBuilder = createJsonResponseBuilder()
    stubFor(
      get("/prisoner/$prisonerId")
        .willReturn(
          if (prisoner == null) {
            responseBuilder
              .withStatus(httpStatus.value())
          } else {
            responseBuilder
              .withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(prisoner))
          },
        ),
    )
  }

  fun stubGetPrisonersByPrisonerIds(
    prisonerIds: List<String>,
    prisoners: List<PrisonerDto>?,
    httpStatus: HttpStatus = HttpStatus.NOT_FOUND,
  ) {
    val responseFields = listOf("prisonerNumber", "firstName", "lastName").joinToString(",")
    val responseBuilder = createJsonResponseBuilder()
    stubFor(
      post("/attribute-search?size=10000&responseFields=$responseFields")
        .willReturn(
          if (prisoners == null) {
            responseBuilder
              .withStatus(httpStatus.value())
          } else {
            responseBuilder
              .withStatus(HttpStatus.OK.value())
              .withBody(
                getJsonString(
                  RestPage(
                    content = prisoners,
                    size = prisoners.size,
                    total = prisoners.size.toLong(),
                    page = 0,
                  ),
                ),
              )
          },
        ),
    )
  }
}
