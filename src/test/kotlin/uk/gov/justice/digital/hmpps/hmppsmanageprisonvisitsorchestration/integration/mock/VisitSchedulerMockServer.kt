package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.http.RequestMethod
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.GET_CANCELLED_PUBLIC_VISITS_BY_BOOKER_REFERENCE
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.GET_FUTURE_BOOKED_PUBLIC_VISITS_BY_BOOKER_REFERENCE
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.GET_PAST_BOOKED_PUBLIC_VISITS_BY_BOOKER_REFERENCE
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.RestPage
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.AvailableVisitSessionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.DateRange
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionCapacityDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionScheduleDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSchedulerPrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSessionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.application.ApplicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionRestriction
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitRestriction
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.NotificationCountDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.NotificationEventType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.NotificationEventType.NON_ASSOCIATION_EVENT
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.NotificationGroupDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.PrisonerVisitsNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase.Companion.getVisitsQueryParams
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.MockUtils.Companion.createJsonResponseBuilder
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.MockUtils.Companion.getJsonString
import uk.gov.justice.digital.hmpps.visitscheduler.dto.audit.EventAuditDto
import java.time.LocalDate
import java.time.LocalTime
import java.util.ArrayList

class VisitSchedulerMockServer : WireMockServer(8092) {
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

  fun stubPostNotification(notificationEndPoint: String) {
    val responseBuilder = createJsonResponseBuilder()
    stubFor(
      post(notificationEndPoint)
        .willReturn(
          responseBuilder.withStatus(
            HttpStatus.OK.value(),
          ),
        ),
    )
  }

  fun verifyPost(notificationEndPoint: String, any: Any? = null) {
    val builder = RequestPatternBuilder(RequestMethod.POST, WireMock.urlEqualTo(notificationEndPoint))
      .withPort(8092)
      .withUrl(notificationEndPoint)
      .withHeader("Content-Type", containing("application/json"))

    any?.let {
      builder.withRequestBody(equalToJson(getJsonString(any)))
    }

    client.verifyThat(1, builder)
  }

  fun stubGetVisitHistory(reference: String, eventsAudit: List<EventAuditDto>) {
    val responseBuilder = createJsonResponseBuilder()
    stubFor(
      get("/visits/$reference/history")
        .willReturn(
          if (eventsAudit.isEmpty()) {
            responseBuilder.withStatus(HttpStatus.NOT_FOUND.value())
          } else {
            responseBuilder.withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(eventsAudit))
          },
        ),
    )
  }

  fun stubGetVisits(
    prisonCode: String? = null,
    prisonerId: String,
    visitStatus: List<String>,
    startDate: LocalDate?,
    endDate: LocalDate?,
    page: Int,
    size: Int,
    visits: List<VisitDto>,
  ) {
    val restPage = RestPage(content = visits, page = 0, size = size, total = visits.size.toLong())
    stubFor(
      get("/visits/search?${getVisitsQueryParams(prisonCode, prisonerId, visitStatus, startDate, endDate, page, size).joinToString("&")}")
        .willReturn(
          createJsonResponseBuilder()
            .withStatus(HttpStatus.OK.value()).withBody(
              getJsonString(restPage),
            ),
        ),
    )
  }

  fun stubGetVisitsBySessionTemplate(
    sessionTemplateReference: String,
    sessionDate: LocalDate,
    visitStatus: List<String>,
    visitRestrictions: List<VisitRestriction>,
    prisonCode: String,
    page: Int,
    size: Int,
    visits: List<VisitDto>,
  ) {
    val restPage = RestPage(content = visits, page = 0, size = size, total = visits.size.toLong())
    stubFor(
      get(
        "/visits/session-template?${
          getVisitsBySessionTemplateQueryParams(
            sessionTemplateReference,
            sessionDate,
            visitStatus,
            visitRestrictions,
            prisonCode,
            page,
            size,
          ).joinToString("&")
        }",
      )
        .willReturn(
          createJsonResponseBuilder()
            .withStatus(HttpStatus.OK.value()).withBody(
              getJsonString(restPage),
            ),
        ),
    )
  }

  fun stubGetFutureVisits(
    prisonerId: String,
    visits: List<VisitDto>,
  ) {
    stubFor(
      get("/visits/search/future/$prisonerId")
        .willReturn(
          createJsonResponseBuilder()
            .withStatus(HttpStatus.OK.value()).withBody(
              getJsonString(visits),
            ),
        ),
    )
  }

  fun stubPublicFutureVisitsByBookerReference(
    bookerReference: String,
    visits: List<VisitDto>,
  ) {
    stubFor(
      get(GET_FUTURE_BOOKED_PUBLIC_VISITS_BY_BOOKER_REFERENCE.replace("{bookerReference}", bookerReference))
        .willReturn(
          createJsonResponseBuilder()
            .withStatus(HttpStatus.OK.value()).withBody(
              getJsonString(visits),
            ),
        ),
    )
  }

  fun stubPublicPastVisitsByBookerReference(
    bookerReference: String,
    visits: List<VisitDto>,
  ) {
    stubFor(
      get(GET_PAST_BOOKED_PUBLIC_VISITS_BY_BOOKER_REFERENCE.replace("{bookerReference}", bookerReference))
        .willReturn(
          createJsonResponseBuilder()
            .withStatus(HttpStatus.OK.value()).withBody(
              getJsonString(visits),
            ),
        ),
    )
  }

  fun stubPublicCancelledVisitsByBookerReference(
    bookerReference: String,
    visits: List<VisitDto>,
  ) {
    stubFor(
      get(GET_CANCELLED_PUBLIC_VISITS_BY_BOOKER_REFERENCE.replace("{bookerReference}", bookerReference))
        .willReturn(
          createJsonResponseBuilder()
            .withStatus(HttpStatus.OK.value()).withBody(
              getJsonString(visits),
            ),
        ),
    )
  }

  fun stubCreateApplication(applicationDto: ApplicationDto?) {
    val responseBuilder = createJsonResponseBuilder()

    stubFor(
      post("/visits/application/slot/reserve")
        .willReturn(
          if (applicationDto == null) {
            responseBuilder
              .withStatus(HttpStatus.BAD_REQUEST.value())
          } else {
            responseBuilder
              .withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(applicationDto))
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

  fun stubIgnoreVisitNotifications(reference: String, visitDto: VisitDto?, status: HttpStatus? = null) {
    val responseBuilder = createJsonResponseBuilder()

    stubFor(
      put("/visits/notification/visit/$reference/ignore")
        .willReturn(
          if (visitDto == null) {
            responseBuilder.withStatus(status?.value() ?: HttpStatus.NOT_FOUND.value())
          } else {
            responseBuilder
              .withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(visitDto))
          },
        ),
    )
  }

  fun stubGetCountVisitNotificationForPrison(prisonCode: String) {
    val responseBuilder = createJsonResponseBuilder()

    stubFor(
      get("/visits/notification/$prisonCode/count")
        .willReturn(
          responseBuilder
            .withStatus(HttpStatus.OK.value())
            .withBody(getJsonString(NotificationCountDto(1))),
        ),
    )
  }

  fun stubGetCountVisitNotification() {
    val responseBuilder = createJsonResponseBuilder()

    stubFor(
      get("/visits/notification/count")
        .willReturn(
          responseBuilder
            .withStatus(HttpStatus.OK.value())
            .withBody(getJsonString(NotificationCountDto(2))),
        ),
    )
  }

  fun stubFutureNotificationVisitGroups(prisonCode: String): NotificationGroupDto {
    val responseBuilder = createJsonResponseBuilder()

    val now = LocalDate.now()

    val dto = NotificationGroupDto(
      "v7*d7*ed*7u",
      NON_ASSOCIATION_EVENT,
      listOf(
        PrisonerVisitsNotificationDto("AF34567G", "Username1", now, "v1-d7-ed-7u"),
        PrisonerVisitsNotificationDto("BF34567G", "Username2", now.plusDays(1), "v2-d7-ed-7u"),
      ),
    )

    stubFor(
      get("/visits/notification/$prisonCode/groups")
        .willReturn(
          responseBuilder
            .withStatus(HttpStatus.OK.value())
            .withBody(getJsonString(listOf(dto))),
        ),
    )

    return dto
  }

  fun stubChangeBookedVisit(bookingReference: String, applicationDto: ApplicationDto?) {
    val responseBuilder = createJsonResponseBuilder()

    stubFor(
      put("/visits/application/$bookingReference/change")
        .willReturn(
          if (applicationDto == null) {
            responseBuilder
              .withStatus(HttpStatus.NOT_FOUND.value())
          } else {
            responseBuilder
              .withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(applicationDto))
          },
        ),
    )
  }

  fun stubChangeReservedVisitSlot(applicationReference: String, applicationDto: ApplicationDto?) {
    val responseBuilder = createJsonResponseBuilder()

    stubFor(
      put("/visits/application/$applicationReference/slot/change")
        .willReturn(
          if (applicationDto == null) {
            responseBuilder.withStatus(HttpStatus.NOT_FOUND.value())
          } else {
            responseBuilder.withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(applicationDto))
          },
        ),
    )
  }

  fun stubGetAvailableVisitSessions(
    visitSchedulerPrisonDto: VisitSchedulerPrisonDto,
    prisonerId: String,
    sessionRestriction: SessionRestriction,
    visitSessions: List<AvailableVisitSessionDto>,
    httpStatus: HttpStatus = HttpStatus.OK,
    dateRange: DateRange? = null,
    excludedApplicationReference: String? = null,
  ): DateRange {
    val dateRangeToUse = dateRange ?: run {
      val today = LocalDate.now()
      val fromDate = today.plusDays(visitSchedulerPrisonDto.policyNoticeDaysMin.toLong())
      val toDate = today.plusDays(visitSchedulerPrisonDto.policyNoticeDaysMax.toLong())
      DateRange(fromDate, toDate)
    }
    stubFor(
      get("/visit-sessions/available?prisonId=${visitSchedulerPrisonDto.code}&prisonerId=$prisonerId&sessionRestriction=${sessionRestriction.name}&fromDate=${dateRangeToUse.fromDate}&toDate=${dateRangeToUse.toDate}&excludedApplicationReference=$excludedApplicationReference")
        .willReturn(
          createJsonResponseBuilder()
            .withStatus(httpStatus.value())
            .withBody(getJsonString(visitSessions)),
        ),
    )

    return dateRangeToUse
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
  fun stubGetSupportedPrisons(type: UserType, supportedPrisonsList: List<String>) {
    stubFor(
      get("/config/prisons/user-type/${type.name}/supported")
        .willReturn(
          createJsonResponseBuilder()
            .withStatus(HttpStatus.OK.value())
            .withBody(getJsonString(supportedPrisonsList)),
        ),
    )
  }

  fun stubGetVisitNotificationTypes(reference: String, vararg type: NotificationEventType) {
    stubFor(
      get("/visits/notification/visit/$reference/types")
        .willReturn(
          createJsonResponseBuilder()
            .withStatus(HttpStatus.OK.value())
            .withBody(getJsonString(type.toList())),
        ),
    )
  }

  fun stubGetPrison(prisonCode: String, visitSchedulerPrisonDto: VisitSchedulerPrisonDto?, httpStatus: HttpStatus = HttpStatus.NOT_FOUND) {
    stubFor(
      get("/admin/prisons/prison/$prisonCode")
        .willReturn(
          if (visitSchedulerPrisonDto == null) {
            createJsonResponseBuilder()
              .withStatus(httpStatus.value())
          } else {
            createJsonResponseBuilder()
              .withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(visitSchedulerPrisonDto))
          },
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

  private fun getVisitsBySessionTemplateQueryParams(
    sessionTemplateReference: String?,
    sessionDate: LocalDate,
    visitStatus: List<String>,
    visitRestrictions: List<VisitRestriction>?,
    prisonCode: String,
    page: Int,
    size: Int,
  ): List<String> {
    val queryParams = ArrayList<String>()
    sessionTemplateReference?.let {
      queryParams.add("sessionTemplateReference=$sessionTemplateReference")
    }
    queryParams.add("fromDate=$sessionDate")
    queryParams.add("toDate=$sessionDate")
    visitRestrictions?.let {
      visitRestrictions.forEach {
        queryParams.add("visitRestrictions=$it")
      }
    }
    visitStatus.forEach {
      queryParams.add("visitStatus=$it")
    }
    queryParams.add("prisonCode=$prisonCode")
    queryParams.add("page=$page")
    queryParams.add("size=$size")
    return queryParams
  }
}
