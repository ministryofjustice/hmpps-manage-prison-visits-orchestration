package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.put
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.RestPage
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionCapacityDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionScheduleDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SupportTypeDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSessionDto
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.collections.ArrayList

class VisitSchedulerMockServer(@Autowired private val objectMapper: ObjectMapper) : WireMockServer(8092) {
  fun stubGetVisit(reference: String, visitDto: VisitDto?) {
    val responseBuilder = createJsonResponseBuilder()
    stubFor(
      get("/visits/$reference")
        .willReturn(
          if (visitDto == null) {
            responseBuilder.withStatus(HttpStatus.NOT_FOUND.value())
          } else {
            responseBuilder.withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(visitDto))
          },
        ),
    )
  }

  fun stubGetVisits(
    prisonerId: String,
    visitStatus: List<String>,
    startDateTime: LocalDateTime?,
    endDateTime: LocalDateTime?,
    page: Int,
    size: Int,
    visits: List<VisitDto>
  ) {
    val restPage = RestPage(content = visits, page = 0, size = size, total = visits.size.toLong())
    stubFor(
      get("/visits/search?${getVisitsQueryParams(prisonerId, visitStatus, startDateTime, endDateTime, page, size).joinToString("&")}")
        .willReturn(
          createJsonResponseBuilder()
            .withStatus(HttpStatus.OK.value()).withBody(
              getJsonString(restPage),
            ),
        ),
    )
  }

  private fun getVisitsQueryParams(
    prisonerId: String,
    visitStatus: List<String>,
    startDateTime: LocalDateTime? = null,
    endDateTime: LocalDateTime? = null,
    page: Int,
    size: Int
  ): List<String> {
    val queryParams = ArrayList<String>()
    queryParams.add("prisonerId=$prisonerId")
    visitStatus.forEach {
      queryParams.add("visitStatus=$it")
    }
    startDateTime?.let {
      queryParams.add("startDateTime=$it")
    }
    endDateTime?.let {
      queryParams.add("endDateTime=$it")
    }
    queryParams.add("page=$page")
    queryParams.add("size=$size")
    return queryParams
  }

  fun stubReserveVisitSlot(visitDto: VisitDto?) {
    val responseBuilder = createJsonResponseBuilder()

    stubFor(
      post("/visits/slot/reserve")
        .willReturn(
          if (visitDto == null) {
            responseBuilder
              .withStatus(HttpStatus.BAD_REQUEST.value())
          } else {
            responseBuilder
              .withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(visitDto))
          },
        ),
    )
  }

  fun stubBookVisit(applicationReference: String, visitDto: VisitDto?) {
    val responseBuilder = createJsonResponseBuilder()

    stubFor(
      put("/visits/$applicationReference/book")
        .willReturn(
          if (visitDto == null) {
            responseBuilder.withStatus(HttpStatus.NOT_FOUND.value())
          } else {
            responseBuilder.withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(visitDto))
          },
        ),
    )
  }

  fun stubCancelVisit(reference: String, visitDto: VisitDto?) {
    val responseBuilder = createJsonResponseBuilder()

    stubFor(
      put("/visits/$reference/cancel")
        .willReturn(
          if (visitDto == null) {
            responseBuilder.withStatus(HttpStatus.NOT_FOUND.value())
          } else {
            responseBuilder
              .withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(visitDto))
          },
        ),
    )
  }

  fun stubChangeBookedVisit(reference: String, visitDto: VisitDto?) {
    val responseBuilder = createJsonResponseBuilder()

    stubFor(
      put("/visits/$reference/change")
        .willReturn(
          if (visitDto == null) {
            responseBuilder
              .withStatus(HttpStatus.NOT_FOUND.value())
          } else {
            responseBuilder
              .withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(visitDto))
          },
        ),
    )
  }

  fun stubChangeReservedVisitSlot(applicationReference: String, visitDto: VisitDto?) {
    val responseBuilder = createJsonResponseBuilder()

    stubFor(
      put("/visits/$applicationReference/slot/change")
        .willReturn(
          if (visitDto == null) {
            responseBuilder.withStatus(HttpStatus.NOT_FOUND.value())
          } else {
            responseBuilder.withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(visitDto))
          },
        ),
    )
  }

  fun stubGetVisitSessions(prisonId: String, prisonerId: String, visitSessions: List<VisitSessionDto>) {
    stubFor(
      get("/visit-sessions?prisonId=$prisonId&prisonerId=$prisonerId")
        .willReturn(
          createJsonResponseBuilder()
            .withStatus(HttpStatus.OK.value())
            .withBody(getJsonString(visitSessions)),
        ),
    )
  }

  fun stubGetVisitSupport(visitSupportList: List<SupportTypeDto>) {
    stubFor(
      get("/visit-support")
        .willReturn(
          createJsonResponseBuilder()
            .withStatus(HttpStatus.OK.value())
            .withBody(getJsonString(visitSupportList)),
        ),
    )
  }

  fun stubGetSupportedPrisons(supportedPrisonsList: List<String>) {
    stubFor(
      get("/config/prisons/supported")
        .willReturn(
          createJsonResponseBuilder()
            .withStatus(HttpStatus.OK.value())
            .withBody(getJsonString(supportedPrisonsList)),
        ),
    )
  }

  fun stubGetSessionCapacity(
    prisonCode: String,
    sessionDate: LocalDate,
    sessionStartTime: LocalTime,
    sessionEndTime: LocalTime,
    sessionCapacityDto: SessionCapacityDto?,
  ) {
    val responseBuilder = createJsonResponseBuilder()
    stubFor(
      get("/visit-sessions/capacity?prisonId=$prisonCode&sessionDate=$sessionDate&sessionStartTime=$sessionStartTime&sessionEndTime=$sessionEndTime")
        .willReturn(
          if (sessionCapacityDto == null) {
            responseBuilder.withStatus(HttpStatus.NOT_FOUND.value())
          } else {
            responseBuilder.withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(sessionCapacityDto))
          },
        ),
    )
  }

  fun stubGetSessionSchedule(
    prisonCode: String,
    sessionDate: LocalDate,
    sessionSchedules: List<SessionScheduleDto>,
  ) {
    val responseBuilder = createJsonResponseBuilder()
    stubFor(
      get("/visit-sessions/schedule?prisonId=$prisonCode&date=$sessionDate")
        .willReturn(
          responseBuilder.withStatus(HttpStatus.OK.value())
            .withBody(getJsonString(sessionSchedules)),
        ),
    )
  }

  private fun getJsonString(obj: Any): String {
    return objectMapper.writer().withDefaultPrettyPrinter().writeValueAsString(obj)
  }

  private fun createJsonResponseBuilder(): ResponseDefinitionBuilder {
    return aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
  }
}
