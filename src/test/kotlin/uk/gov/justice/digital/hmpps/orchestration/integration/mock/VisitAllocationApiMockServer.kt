package uk.gov.justice.digital.hmpps.orchestration.integration.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.orchestration.dto.visit.allocation.PrisonerVOBalanceDto
import uk.gov.justice.digital.hmpps.orchestration.dto.visit.allocation.VisitOrderHistoryDto
import uk.gov.justice.digital.hmpps.orchestration.integration.mock.MockUtils.Companion.createJsonResponseBuilder
import uk.gov.justice.digital.hmpps.orchestration.integration.mock.MockUtils.Companion.getJsonString
import java.time.LocalDate

class VisitAllocationApiMockServer : WireMockServer(8101) {

  fun stubGetPrisonerVOBalance(
    prisonerId: String,
    prisonerVOBalanceDto: PrisonerVOBalanceDto?,
    httpStatus: HttpStatus = HttpStatus.NOT_FOUND,
  ) {
    val responseBuilder = createJsonResponseBuilder()
    stubFor(
      WireMock.get("/visits/allocation/prisoner/$prisonerId/balance/detailed")
        .willReturn(
          if (prisonerVOBalanceDto == null) {
            responseBuilder
              .withStatus(httpStatus.value())
          } else {
            responseBuilder
              .withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(prisonerVOBalanceDto))
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
}
