package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.put
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config.PrisonerBalanceAdjustmentValidationErrorResponse
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation.PrisonerVOBalanceDetailedDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation.VisitOrderHistoryDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation.VisitOrderPrisonerBalanceDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.MockUtils.Companion.createJsonResponseBuilder
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.MockUtils.Companion.getJsonString
import java.time.LocalDate

class VisitAllocationApiMockServer : WireMockServer(8101) {

  fun stubGetPrisonerVOBalanceDetailed(
    prisonerId: String,
    prisonerVOBalanceDetailedDto: PrisonerVOBalanceDetailedDto?,
    httpStatus: HttpStatus = HttpStatus.NOT_FOUND,
  ) {
    val responseBuilder = createJsonResponseBuilder()
    stubFor(
      WireMock.get("/visits/allocation/prisoner/$prisonerId/balance/detailed")
        .willReturn(
          if (prisonerVOBalanceDetailedDto == null) {
            responseBuilder
              .withStatus(httpStatus.value())
          } else {
            responseBuilder
              .withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(prisonerVOBalanceDetailedDto))
          },
        ),
    )
  }

  fun stubGetPrisonerVOBalance(
    prisonerId: String,
    prisonerBalance: VisitOrderPrisonerBalanceDto?,
    httpStatus: HttpStatus = HttpStatus.NOT_FOUND,
  ) {
    val responseBuilder = createJsonResponseBuilder()
    stubFor(
      WireMock.get("/visits/allocation/prisoner/$prisonerId/balance")
        .willReturn(
          if (prisonerBalance == null) {
            responseBuilder
              .withStatus(httpStatus.value())
          } else {
            responseBuilder
              .withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(prisonerBalance))
          },
        ),
    )
  }

  fun stubGetVisitOrderHistory(
    prisonerId: String,
    fromDate: LocalDate,
    visitOrderHistoryList: List<VisitOrderHistoryDto>?,
    httpStatus: HttpStatus = HttpStatus.NOT_FOUND,
  ) {
    val responseBuilder = createJsonResponseBuilder()
    stubFor(
      WireMock.get("/visits/allocation/prisoner/$prisonerId/visit-order-history?fromDate=$fromDate")
        .willReturn(
          if (visitOrderHistoryList == null) {
            responseBuilder
              .withStatus(httpStatus.value())
          } else {
            responseBuilder
              .withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(visitOrderHistoryList))
          },
        ),
    )
  }

  fun stubAdjustPrisonersVisitOrderBalance(prisonerId: String, response: VisitOrderPrisonerBalanceDto?, httpStatus: HttpStatus = HttpStatus.OK) {
    val responseBuilder = createJsonResponseBuilder()

    stubFor(
      put("/visits/allocation/prisoner/$prisonerId/balance")
        .willReturn(
          if (response == null) {
            responseBuilder.withStatus(httpStatus.value())
          } else {
            responseBuilder.withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(response))
          },
        ),
    )
  }

  fun stubAdjustPrisonersVisitOrderBalanceValidationFailure(prisonerId: String, errorResponse: PrisonerBalanceAdjustmentValidationErrorResponse) {
    val responseBuilder = createJsonResponseBuilder()
    stubFor(
      WireMock.put("/visits/allocation/prisoner/$prisonerId/balance")
        .willReturn(
          responseBuilder
            .withStatus(HttpStatus.UNPROCESSABLE_ENTITY.value())
            .withBody(getJsonString(errorResponse)),
        ),
    )
  }
}
