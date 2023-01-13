package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.put
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.RestPage
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.SupportTypeDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.VisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.VisitSessionDto

class VisitSchedulerMockServer(@Autowired private val objectMapper: ObjectMapper) : WireMockServer(8092) {

  fun stubGetVisit(reference: String, visitDto: VisitDto?) {
    if (visitDto == null) {
      stubFor(
        get("/visits/$reference")
          .willReturn(
            aResponse().withStatus(HttpStatus.NOT_FOUND.value())
          )
      )
    } else {
      stubFor(
        get("/visits/$reference")
          .willReturn(
            aResponse()
              .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
              .withStatus(HttpStatus.OK.value())
              .withBody(
                getJsonString(visitDto)
              )
          )
      )
    }
  }

  fun stubGetVisits(visitStatus: String, prisonerId: String, page: Int, size: Int, visits: List<VisitDto>) {
    val restPage = RestPage(content = visits, page = 0, size = size, total = visits.size.toLong())
    stubFor(
      get("/visits/search?prisonerId=$prisonerId&visitStatus=$visitStatus&page=$page&size=$size")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .withStatus(HttpStatus.OK.value()).withBody(
              getJsonString(restPage)
            )
        )
    )
  }

  fun stubReserveVisitSlot(visitDto: VisitDto?) {
    if (visitDto == null) {
      stubFor(
        post("/visits/slot/reserve")
          .willReturn(
            aResponse()
              .withStatus(HttpStatus.BAD_REQUEST.value())

          )
      )
    } else {
      stubFor(
        post("/visits/slot/reserve")
          .willReturn(
            aResponse()
              .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
              .withStatus(HttpStatus.OK.value())
              .withBody(
                getJsonString(visitDto)
              )
          )
      )
    }
  }

  fun stubBookVisit(applicationReference: String, visitDto: VisitDto?) {
    if (visitDto == null) {
      stubFor(
        put("/visits/$applicationReference/book")
          .willReturn(
            aResponse()
              .withStatus(HttpStatus.NOT_FOUND.value())
          )
      )
    } else {
      stubFor(
        put("/visits/$applicationReference/book")
          .willReturn(
            aResponse()
              .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
              .withStatus(HttpStatus.OK.value())
              .withBody(
                getJsonString(visitDto)
              )
          )
      )
    }
  }

  fun stubCancelVisit(reference: String, visitDto: VisitDto?) {
    if (visitDto == null) {
      stubFor(
        put("/visits/$reference/cancel")
          .willReturn(
            aResponse()
              .withStatus(HttpStatus.NOT_FOUND.value())
          )
      )
    } else {
      stubFor(
        put("/visits/$reference/cancel")
          .willReturn(
            aResponse()
              .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
              .withStatus(HttpStatus.OK.value())
              .withBody(
                getJsonString(visitDto)
              )
          )
      )
    }
  }

  fun stubChangeBookedVisit(reference: String, visitDto: VisitDto?) {
    if (visitDto == null) {
      stubFor(
        put("/visits/$reference/change")
          .willReturn(
            aResponse()
              .withStatus(HttpStatus.NOT_FOUND.value())
          )
      )
    } else {
      stubFor(
        put("/visits/$reference/change")
          .willReturn(
            aResponse()
              .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
              .withStatus(HttpStatus.OK.value())
              .withBody(
                getJsonString(visitDto)
              )
          )
      )
    }
  }

  fun stubChangeReservedVisitSlot(applicationReference: String, visitDto: VisitDto?) {
    if (visitDto == null) {
      stubFor(
        put("/visits/$applicationReference/slot/change")
          .willReturn(
            aResponse()
              .withStatus(HttpStatus.NOT_FOUND.value())
          )
      )
    } else {
      stubFor(
        put("/visits/$applicationReference/slot/change")
          .willReturn(
            aResponse()
              .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
              .withStatus(HttpStatus.OK.value())
              .withBody(
                getJsonString(visitDto)
              )
          )
      )
    }
  }

  fun stubGetVisitSessions(prisonId: String, prisonerId: String, visitSessions: List<VisitSessionDto>) {
    stubFor(
      get("/visit-sessions?prisonId=$prisonId&prisonerId=$prisonerId")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .withStatus(HttpStatus.OK.value())
            .withBody(
              getJsonString(visitSessions)
            )
        )
    )
  }

  fun stubGetVisitSupport(visitSupportList: List<SupportTypeDto>) {
    stubFor(
      get("/visit-support")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .withStatus(HttpStatus.OK.value())
            .withBody(
              getJsonString(visitSupportList)
            )
        )
    )
  }

  fun stubGetSupportedPrisons(supportedPrisonsList: List<String>) {
    stubFor(
      get("/config/prisons/supported")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .withStatus(HttpStatus.OK.value())
            .withBody(
              getJsonString(supportedPrisonsList)
            )
        )
    )
  }

  private fun getJsonString(obj: Any): String {
    return objectMapper.writer().withDefaultPrettyPrinter().writeValueAsString(obj)
  }
}
