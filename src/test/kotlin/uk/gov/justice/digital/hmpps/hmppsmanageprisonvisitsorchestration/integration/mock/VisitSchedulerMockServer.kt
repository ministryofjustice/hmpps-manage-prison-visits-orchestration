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
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.POST_VISIT_FROM_EXTERNAL_SYSTEM
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config.ApplicationValidationErrorResponse
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller.FUTURE_NOTIFICATION_VISITS
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller.VISIT_REQUESTS_VISITS_FOR_PRISON_PATH
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.RestPage
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.AvailableVisitSessionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.CreateVisitFromExternalSystemDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.DateRange
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.EventAuditDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionCapacityDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionScheduleDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.UpdateVisitFromExternalSystemDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitRequestSummaryDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitRequestsCountDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSchedulerPrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSessionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.application.ApplicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionRestriction
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitRestriction
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.prisons.ExcludeDateDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.NotificationCountDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.NotificationEventType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.VisitNotificationEventDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.VisitNotificationsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase.Companion.getVisitsQueryParams
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.MockUtils.Companion.createJsonResponseBuilder
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.MockUtils.Companion.getJsonString
import java.time.LocalDate
import java.time.LocalTime

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

  fun stubGetVisitByClientRef(clientReference: String, referenceResponse: List<String?>?) {
    val responseBuilder = createJsonResponseBuilder()
    stubFor(
      get("/visits/external-system/$clientReference")
        .willReturn(
          if (referenceResponse.isNullOrEmpty()) {
            responseBuilder.withStatus(HttpStatus.NOT_FOUND.value())
          } else {
            responseBuilder.withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(referenceResponse))
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

  fun stubGetVisitHistory(reference: String, eventsAudit: List<EventAuditDto>, httpStatus: HttpStatus = HttpStatus.NOT_FOUND) {
    val responseBuilder = createJsonResponseBuilder()
    stubFor(
      get("/visits/$reference/history")
        .willReturn(
          if (eventsAudit.isEmpty()) {
            responseBuilder.withStatus(httpStatus.value())
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
    val uri = "/visits/session-template"
    val uriParams = getVisitsBySessionTemplateQueryParams(
      sessionTemplateReference,
      sessionDate,
      visitStatus,
      visitRestrictions,
      prisonCode,
      page,
      size,
    ).joinToString("&")

    stubFor(
      get("$uri?$uriParams")
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

  fun stubUpdateVisit(applicationReference: String, visitDto: VisitDto?) {
    val responseBuilder = createJsonResponseBuilder()

    stubFor(
      put("/visits/$applicationReference/visit/update")
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

  fun stubGetBookedVisitByApplicationReference(applicationReference: String, existingVisitDto: VisitDto?) {
    val responseBuilder = createJsonResponseBuilder()

    stubFor(
      get("/visits/$applicationReference/visit")
        .willReturn(
          if (existingVisitDto == null) {
            responseBuilder.withStatus(HttpStatus.NOT_FOUND.value())
          } else {
            responseBuilder.withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(existingVisitDto))
          },
        ),
    )
  }

  fun stubBookVisitApplicationValidationFailure(applicationReference: String, errorResponse: ApplicationValidationErrorResponse) {
    val responseBuilder = createJsonResponseBuilder()

    stubFor(
      put("/visits/$applicationReference/book")
        .willReturn(
          responseBuilder.withStatus(HttpStatus.UNPROCESSABLE_ENTITY.value())
            .withBody(getJsonString(errorResponse)),
        ),
    )
  }

  fun stubBookVisitApplicationValidationFailureInvalid(applicationReference: String) {
    val responseBuilder = createJsonResponseBuilder()

    stubFor(
      put("/visits/$applicationReference/book")
        .willReturn(
          responseBuilder.withStatus(HttpStatus.UNPROCESSABLE_ENTITY.value())
            .withBody(
              """{
                "status": 422,
                "validationErrors": [
                  "INVALID_APPLICATION_VALIDATION_RESPONSE"
                  ]
                }""",
            ),
        ),
    )
  }

  fun stubUpdateVisitApplicationValidationFailure(applicationReference: String, errorResponse: ApplicationValidationErrorResponse) {
    val responseBuilder = createJsonResponseBuilder()

    stubFor(
      put("/visits/$applicationReference/visit/update")
        .willReturn(
          responseBuilder.withStatus(HttpStatus.UNPROCESSABLE_ENTITY.value())
            .withBody(getJsonString(errorResponse)),
        ),
    )
  }

  fun stubUpdateVisitApplicationValidationFailureInvalid(applicationReference: String) {
    val responseBuilder = createJsonResponseBuilder()

    stubFor(
      put("/visits/$applicationReference/visit/update")
        .willReturn(
          responseBuilder.withStatus(HttpStatus.UNPROCESSABLE_ENTITY.value())
            .withBody(
              """{
                "status": 422,
                "validationErrors": [
                  "INVALID_APPLICATION_VALIDATION_RESPONSE"
                  ]
                }""",
            ),
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

  fun stubGetCountVisitNotificationForPrison(prisonCode: String, notificationEventTypes: List<NotificationEventType>?, count: Int) {
    val responseBuilder = createJsonResponseBuilder()
    var url = "/visits/notification/$prisonCode/count"
    url = if (notificationEventTypes != null) {
      url + "?types=${notificationEventTypes.joinToString(",") { it.name }}"
    } else {
      url
    }

    stubFor(
      get(url)
        .willReturn(
          responseBuilder
            .withStatus(HttpStatus.OK.value())
            .withBody(getJsonString(NotificationCountDto(count))),
        ),
    )
  }

  fun stubGetCountVisitRequestsForPrison(prisonCode: String, count: Int, status: HttpStatus? = null) {
    val responseBuilder = createJsonResponseBuilder()
    val url = "/visits/requests/$prisonCode/count"

    stubFor(
      get(url)
        .willReturn(
          if (status == null) {
            responseBuilder
              .withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(VisitRequestsCountDto(count)))
          } else {
            responseBuilder
              .withStatus(status.value())
              .withBody(getJsonString(VisitRequestsCountDto(count)))
          },
        ),
    )
  }

  fun stubGetFutureVisitsWithNotificationsForPrison(prisonCode: String, notificationEventTypes: List<NotificationEventType>?, visitsWithNotifications: List<VisitNotificationsDto>) {
    val responseBuilder = createJsonResponseBuilder()
    var url = FUTURE_NOTIFICATION_VISITS.replace("{prisonCode}", prisonCode)
    if (!notificationEventTypes.isNullOrEmpty()) {
      url += "?types=${notificationEventTypes.joinToString(",") { it.name }}"
    }
    stubFor(
      get(url)
        .willReturn(
          responseBuilder
            .withStatus(HttpStatus.OK.value())
            .withBody(getJsonString(visitsWithNotifications)),
        ),
    )
  }

  fun stubGetVisitRequestsForPrison(prisonCode: String, visitRequests: List<VisitRequestSummaryDto>) {
    val responseBuilder = createJsonResponseBuilder()
    val url = VISIT_REQUESTS_VISITS_FOR_PRISON_PATH.replace("{prisonCode}", prisonCode)

    stubFor(
      get(url)
        .willReturn(
          responseBuilder
            .withStatus(HttpStatus.OK.value())
            .withBody(getJsonString(visitRequests)),
        ),
    )
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
    username: String? = null,
    userType: UserType,
  ): DateRange {
    val dateRangeToUse = dateRange ?: run {
      val today = LocalDate.now()
      val fromDate = today.plusDays(visitSchedulerPrisonDto.policyNoticeDaysMin.toLong().plus(1))
      val toDate = today.plusDays(visitSchedulerPrisonDto.policyNoticeDaysMax.toLong())
      DateRange(fromDate, toDate)
    }
    stubFor(
      get(
        "/visit-sessions/available?${
          getAvailableVisitSessionQueryParams(
            prisonCode = visitSchedulerPrisonDto.code,
            prisonerId = prisonerId,
            sessionRestriction = sessionRestriction,
            fromDate = dateRangeToUse.fromDate,
            toDate = dateRangeToUse.toDate,
            excludedApplicationReference = excludedApplicationReference,
            username = username,
            userType = userType,
          ).joinToString("&")
        }",
      ).willReturn(
        createJsonResponseBuilder()
          .withStatus(httpStatus.value())
          .withBody(getJsonString(visitSessions)),
      ),
    )

    return dateRangeToUse
  }

  fun stubGetVisitSessions(prisonId: String, prisonerId: String, visitSessions: List<VisitSessionDto>, userType: UserType) {
    stubFor(
      get("/visit-sessions?prisonId=$prisonId&prisonerId=$prisonerId&userType=${userType.name}")
        .willReturn(
          createJsonResponseBuilder()
            .withStatus(HttpStatus.OK.value())
            .withBody(getJsonString(visitSessions)),
        ),
    )
  }
  fun stubGetSupportedPrisons(type: UserType, supportedPrisonsList: List<String>?, httpStatus: HttpStatus = HttpStatus.NOT_FOUND) {
    stubFor(
      get("/config/prisons/user-type/${type.name}/supported")
        .willReturn(
          if (supportedPrisonsList == null) {
            createJsonResponseBuilder()
              .withStatus(httpStatus.value())
          } else {
            createJsonResponseBuilder()
              .withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(supportedPrisonsList))
          },
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

  fun stubGetVisitNotificationEvents(reference: String, notificationEvents: List<VisitNotificationEventDto>?, httpStatus: HttpStatus = HttpStatus.NOT_FOUND) {
    stubFor(
      get("/visits/notification/visit/$reference/events")
        .willReturn(
          if (notificationEvents == null) {
            createJsonResponseBuilder()
              .withStatus(httpStatus.value())
          } else {
            createJsonResponseBuilder()
              .withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(notificationEvents.toList()))
          },
        ),
    )
  }

  fun stubPostVisitFromExternalSystem(createVisitFromExternalSystemDto: CreateVisitFromExternalSystemDto, responseVisitDto: VisitDto, status: HttpStatus = HttpStatus.OK) {
    val responseBuilder = createJsonResponseBuilder()
    stubFor(
      post(POST_VISIT_FROM_EXTERNAL_SYSTEM)
        .withRequestBody(equalToJson(getJsonString(createVisitFromExternalSystemDto)))
        .willReturn(
          responseBuilder
            .withStatus(status.value())
            .withBody(getJsonString(responseVisitDto)),
        ),
    )
  }

  fun stubPutVisitFromExternalSystem(updateVisitFromExternalSystemDto: UpdateVisitFromExternalSystemDto, responseVisitDto: VisitDto, status: HttpStatus = HttpStatus.OK) {
    val responseBuilder = createJsonResponseBuilder()
    stubFor(
      put("/visits/external-system/${updateVisitFromExternalSystemDto.visitReference}")
        .withRequestBody(equalToJson(getJsonString(updateVisitFromExternalSystemDto)))
        .willReturn(
          responseBuilder
            .withStatus(status.value())
            .withBody(getJsonString(responseVisitDto)),
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

  fun stubGetSession(
    prisonCode: String,
    sessionDate: LocalDate,
    sessionTemplateReference: String,
    visitSessionDto: VisitSessionDto?,
  ) {
    val responseBuilder = createJsonResponseBuilder()
    stubFor(
      get("/visit-sessions/session?prisonCode=$prisonCode&sessionDate=$sessionDate&sessionTemplateReference=$sessionTemplateReference")
        .willReturn(
          if (visitSessionDto == null) {
            responseBuilder.withStatus(HttpStatus.NOT_FOUND.value())
          } else {
            responseBuilder.withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(visitSessionDto))
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

  fun stubGetExcludeDates(
    prisonCode: String,
    excludeDates: List<ExcludeDateDto>?,
    httpStatus: HttpStatus = HttpStatus.NOT_FOUND,
  ) {
    val responseBuilder = createJsonResponseBuilder()
    stubFor(
      get("/prisons/prison/$prisonCode/exclude-date")
        .willReturn(
          if (excludeDates != null) {
            responseBuilder.withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(excludeDates))
          } else {
            responseBuilder.withStatus(httpStatus.value())
          },
        ),
    )
  }

  fun stubAddExcludeDate(
    prisonCode: String,
    excludeDates: List<LocalDate>?,
    httpStatus: HttpStatus = HttpStatus.NOT_FOUND,
  ) {
    val responseBuilder = createJsonResponseBuilder()
    stubFor(
      put("/prisons/prison/$prisonCode/exclude-date/add")
        .willReturn(
          if (excludeDates != null) {
            responseBuilder.withStatus(HttpStatus.CREATED.value())
              .withBody(getJsonString(excludeDates))
          } else {
            responseBuilder.withStatus(httpStatus.value())
          },
        ),
    )
  }

  fun stubRemoveExcludeDate(
    prisonCode: String,
    excludeDates: List<LocalDate>?,
    httpStatus: HttpStatus = HttpStatus.NOT_FOUND,
  ) {
    val responseBuilder = createJsonResponseBuilder()
    stubFor(
      put("/prisons/prison/$prisonCode/exclude-date/remove")
        .willReturn(
          if (excludeDates != null) {
            responseBuilder.withStatus(HttpStatus.CREATED.value())
              .withBody(getJsonString(excludeDates))
          } else {
            responseBuilder.withStatus(httpStatus.value())
          },
        ),
    )
  }

  fun stubGetSessionTemplateExcludeDates(
    sessionTemplateReference: String,
    excludeDates: List<ExcludeDateDto>?,
    httpStatus: HttpStatus = HttpStatus.NOT_FOUND,
  ) {
    val responseBuilder = createJsonResponseBuilder()
    stubFor(
      get("/admin/session-templates/template/$sessionTemplateReference/exclude-date")
        .willReturn(
          if (excludeDates != null) {
            responseBuilder.withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(excludeDates))
          } else {
            responseBuilder.withStatus(httpStatus.value())
          },
        ),
    )
  }

  fun stubAddSessionTemplateExcludeDate(
    sessionTemplateReference: String,
    excludeDates: List<LocalDate>?,
    httpStatus: HttpStatus = HttpStatus.NOT_FOUND,
  ) {
    val responseBuilder = createJsonResponseBuilder()
    stubFor(
      put("/admin/session-templates/template/$sessionTemplateReference/exclude-date/add")
        .willReturn(
          if (excludeDates != null) {
            responseBuilder.withStatus(HttpStatus.CREATED.value())
              .withBody(getJsonString(excludeDates))
          } else {
            responseBuilder.withStatus(httpStatus.value())
          },
        ),
    )
  }

  fun stubRemoveSessionTemplateExcludeDate(
    sessionTemplateReference: String,
    excludeDates: List<LocalDate>?,
    httpStatus: HttpStatus = HttpStatus.NOT_FOUND,
  ) {
    val responseBuilder = createJsonResponseBuilder()
    stubFor(
      put("/admin/session-templates/template/$sessionTemplateReference/exclude-date/remove")
        .willReturn(
          if (excludeDates != null) {
            responseBuilder.withStatus(HttpStatus.CREATED.value())
              .withBody(getJsonString(excludeDates))
          } else {
            responseBuilder.withStatus(httpStatus.value())
          },
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

  private fun getAvailableVisitSessionQueryParams(
    prisonCode: String,
    prisonerId: String,
    sessionRestriction: SessionRestriction,
    fromDate: LocalDate,
    toDate: LocalDate,
    excludedApplicationReference: String?,
    username: String?,
    userType: UserType,
  ): List<String> {
    val queryParams = ArrayList<String>()
    queryParams.add("prisonId=$prisonCode")
    queryParams.add("prisonerId=$prisonerId")
    queryParams.add("sessionRestriction=${sessionRestriction.name}")
    queryParams.add("fromDate=$fromDate")
    queryParams.add("toDate=$toDate")
    excludedApplicationReference?.let {
      queryParams.add("excludedApplicationReference=$excludedApplicationReference")
    }
    username?.let {
      queryParams.add("username=$username")
    }
    queryParams.add("userType=${userType.name}")
    return queryParams
  }
}
