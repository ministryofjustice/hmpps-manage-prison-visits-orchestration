package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.ClientUtils.Companion.isNotFoundError
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.ClientUtils.Companion.isUnprocessableEntityError
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonerSearchClient.Companion.logger
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config.ApplicationValidationErrorResponse
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.RestPage
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.ApproveVisitRequestBodyDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.AvailableVisitSessionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.BookingRequestDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.CancelVisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.CreateVisitFromExternalSystemDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.DateRange
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.EventAuditDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.IgnoreVisitNotificationsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.RejectVisitRequestBodyDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionCapacityDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionScheduleDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.UpdateVisitFromExternalSystemDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitPreviewDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitRequestSummaryDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitRequestsCountDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSchedulerPrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSessionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionRestriction
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitRestriction
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitStatus
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.prisons.ExcludeDateDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.CourtVideoAppointmentNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.NonAssociationChangedNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.NotificationCountDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.PersonRestrictionUpsertedNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.PrisonerAlertsAddedNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.PrisonerReceivedNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.PrisonerReleasedNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.PrisonerRestrictionChangeNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.VisitNotificationEventDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.VisitNotificationsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.VisitorApprovedUnapprovedNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.VisitorRestrictionUpsertedNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitor.VisitorLastApprovedDateRequestDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitor.VisitorLastApprovedDatesDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.ApplicationValidationException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.NotFoundException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.filter.VisitSearchRequestFilter
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

const val VISIT_CONTROLLER_PATH: String = "/visits"
const val GET_VISIT_HISTORY_CONTROLLER_PATH: String = "$VISIT_CONTROLLER_PATH/{reference}/history"

const val VISIT_NOTIFICATION_CONTROLLER_PATH: String = "$VISIT_CONTROLLER_PATH/notification"

const val VISIT_NOTIFICATION_COURT_VIDEO_APPOINTMENT_CREATED_PATH: String = "$VISIT_NOTIFICATION_CONTROLLER_PATH/court-video-appointment/created"
const val VISIT_NOTIFICATION_COURT_VIDEO_APPOINTMENT_UPDATED_PATH: String = "$VISIT_NOTIFICATION_CONTROLLER_PATH/court-video-appointment/updated"
const val VISIT_NOTIFICATION_COURT_VIDEO_APPOINTMENT_CANCELLED_DELETED_PATH: String = "$VISIT_NOTIFICATION_CONTROLLER_PATH/court-video-appointment/cancelled-or-deleted"

const val VISIT_NOTIFICATION_NON_ASSOCIATION_CHANGE_PATH: String = "$VISIT_NOTIFICATION_CONTROLLER_PATH/non-association/changed"

const val VISIT_NOTIFICATION_PRISONER_RECEIVED_CHANGE_PATH: String = "$VISIT_NOTIFICATION_CONTROLLER_PATH/prisoner/received"
const val VISIT_NOTIFICATION_PRISONER_RELEASED_CHANGE_PATH: String = "$VISIT_NOTIFICATION_CONTROLLER_PATH/prisoner/released"
const val VISIT_NOTIFICATION_PRISONER_RESTRICTION_CHANGE_PATH: String = "$VISIT_NOTIFICATION_CONTROLLER_PATH/prisoner/restriction/changed"

const val VISIT_NOTIFICATION_PERSON_RESTRICTION_UPSERTED_PATH: String = "$VISIT_NOTIFICATION_CONTROLLER_PATH/person/restriction/upserted"

const val VISIT_NOTIFICATION_VISITOR_RESTRICTION_UPSERTED_PATH: String = "$VISIT_NOTIFICATION_CONTROLLER_PATH/visitor/restriction/upserted"
const val VISIT_NOTIFICATION_VISITOR_APPROVED_PATH: String = "$VISIT_NOTIFICATION_CONTROLLER_PATH/visitor/approved"
const val VISIT_NOTIFICATION_VISITOR_UNAPPROVED_PATH: String = "$VISIT_NOTIFICATION_CONTROLLER_PATH/visitor/unapproved"

const val VISIT_NOTIFICATION_PRISONER_ALERTS_UPDATED_PATH: String = "$VISIT_NOTIFICATION_CONTROLLER_PATH/prisoner/alerts/updated"

const val GET_FUTURE_BOOKED_PUBLIC_VISITS_BY_BOOKER_REFERENCE: String = "/public/booker/{bookerReference}/visits/booked/future"
const val GET_CANCELLED_PUBLIC_VISITS_BY_BOOKER_REFERENCE: String = "/public/booker/{bookerReference}/visits/cancelled"
const val GET_PAST_BOOKED_PUBLIC_VISITS_BY_BOOKER_REFERENCE: String = "/public/booker/{bookerReference}/visits/booked/past"
const val GET_VISIT_EVENTS_BY_BOOKER_REFERENCE: String = "/public/booker/{bookerReference}/visits/events"

const val POST_VISIT_FROM_EXTERNAL_SYSTEM: String = "$VISIT_CONTROLLER_PATH/external-system"
const val FIND_LAST_APPROVED_DATE_FOR_VISITORS_BY_PRISONER: String = "/visits/prisoner/{prisonerNumber}/visitors/last-approved-date"

@Component
class VisitSchedulerClient(
  val objectMapper: ObjectMapper,
  @param:Qualifier("visitSchedulerWebClient") private val webClient: WebClient,
  @param:Value("\${visit-scheduler.api.timeout:10s}") val apiTimeout: Duration,
) {

  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getPastPublicBookedVisitsByBookerReference(bookerReference: String): List<VisitDto> = webClient.get()
    .uri(GET_PAST_BOOKED_PUBLIC_VISITS_BY_BOOKER_REFERENCE.replace("{bookerReference}", bookerReference))
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono<List<VisitDto>>()
    .blockOptional(apiTimeout).orElseGet { listOf() }

  fun getCancelledPublicVisitsByBookerReference(bookerReference: String): List<VisitDto> = webClient.get()
    .uri(GET_CANCELLED_PUBLIC_VISITS_BY_BOOKER_REFERENCE.replace("{bookerReference}", bookerReference))
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono<List<VisitDto>>()
    .blockOptional(apiTimeout).orElseGet { listOf() }

  fun getFuturePublicBookedVisitsByBookerReference(bookerReference: String): List<VisitDto> = webClient.get()
    .uri(GET_FUTURE_BOOKED_PUBLIC_VISITS_BY_BOOKER_REFERENCE.replace("{bookerReference}", bookerReference))
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono<List<VisitDto>>()
    .blockOptional(apiTimeout).orElseGet { listOf() }

  fun getVisitByReference(reference: String): VisitDto? = webClient.get()
    .uri("/visits/$reference")
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono<VisitDto>().block(apiTimeout)

  fun getVisitReferenceByClientReference(clientReference: String): List<String?>? = webClient.get()
    .uri("/visits/external-system/$clientReference")
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono<List<String?>>().block(apiTimeout)

  fun getVisitHistoryByReferenceAsMono(reference: String): Mono<List<EventAuditDto>> = webClient.get()
    .uri(GET_VISIT_HISTORY_CONTROLLER_PATH.replace("{reference}", reference))
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono<List<EventAuditDto>>()

  fun getVisits(visitSearchRequestFilter: VisitSearchRequestFilter): RestPage<VisitDto>? = getVisitsAsMono(visitSearchRequestFilter).block(apiTimeout)

  fun getFutureVisitsForPrisoner(prisonerId: String): List<VisitDto>? = webClient.get()
    .uri("/visits/search/future/$prisonerId")
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono<List<VisitDto>>().block(apiTimeout)

  fun getVisitsForSessionTemplateAndDate(
    sessionTemplateReference: String?,
    sessionDate: LocalDate,
    visitStatusList: List<VisitStatus>,
    visitRestrictions: List<VisitRestriction>?,
    prisonCode: String,
    page: Int,
    size: Int,
  ): RestPage<VisitPreviewDto>? = webClient.get()
    .uri("/visits/session-template") {
      it.queryParamIfPresent("sessionTemplateReference", Optional.ofNullable(sessionTemplateReference))
        .queryParam("fromDate", sessionDate)
        .queryParam("toDate", sessionDate)
        .queryParamIfPresent("visitRestrictions", Optional.ofNullable(visitRestrictions))
        .queryParam("visitStatus", visitStatusList.toTypedArray())
        .queryParam("prisonCode", prisonCode)
        .queryParam("page", page)
        .queryParam("size", size)
        .build()
    }
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono<RestPage<VisitPreviewDto>>()
    .block(apiTimeout)

  fun getVisitsAsMono(visitSearchRequestFilter: VisitSearchRequestFilter): Mono<RestPage<VisitDto>> = webClient.get()
    .uri("/visits/search") {
      visitSearchUriBuilder(visitSearchRequestFilter, it).build()
    }
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono()

  fun bookVisitSlot(applicationReference: String, requestDto: BookingRequestDto): VisitDto? = webClient.put()
    .uri("/visits/$applicationReference/book")
    .body(BodyInserters.fromValue(requestDto))
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono<VisitDto>().onErrorResume { e ->
      if (isUnprocessableEntityError(e)) {
        val exception = getBookVisitApplicationValidationErrorResponse(e)
        Mono.error(exception)
      } else {
        Mono.error(e)
      }
    }.block(apiTimeout)

  fun updateBookedVisit(applicationReference: String, requestDto: BookingRequestDto): VisitDto? = webClient.put()
    .uri("/visits/$applicationReference/visit/update")
    .body(BodyInserters.fromValue(requestDto))
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono<VisitDto>().onErrorResume { e ->
      if (isUnprocessableEntityError(e)) {
        val exception = getBookVisitApplicationValidationErrorResponse(e)
        Mono.error(exception)
      } else {
        Mono.error(e)
      }
    }.block(apiTimeout)

  fun updateVisitFromExternalSystem(requestDto: UpdateVisitFromExternalSystemDto): VisitDto? = webClient.put()
    .uri("/visits/external-system/${requestDto.visitReference}")
    .body(BodyInserters.fromValue(requestDto))
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono<VisitDto>()
    .onErrorResume { e ->
      LOG.error("Could not update visit from external system :", e)
      Mono.error(e)
    }
    .block(apiTimeout)

  fun getBookedVisitByApplicationReference(applicationReference: String): VisitDto? = webClient.get()
    .uri("/visits/$applicationReference/visit")
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono<VisitDto>()
    .onErrorResume { e ->
      if (!isNotFoundError(e)) {
        LOG.error("getBookedVisitByApplicationReference Failed to search for existing booking")
        Mono.error(e)
      } else {
        LOG.info("getBookedVisitByApplicationReference NOT FOUND response - no existing booking")
        Mono.empty()
      }
    }
    .block(apiTimeout)

  private fun getBookVisitApplicationValidationErrorResponse(e: Throwable): Throwable {
    if (e is WebClientResponseException && isUnprocessableEntityError(e)) {
      try {
        val errorResponse = objectMapper.readValue(e.responseBodyAsString, ApplicationValidationErrorResponse::class.java)
        return ApplicationValidationException(errorResponse.validationErrors)
      } catch (jsonProcessingException: Exception) {
        LOG.error("An error occurred processing the application validation error response - ${e.stackTraceToString()}")
        throw jsonProcessingException
      }
    }

    return e
  }

  fun cancelVisit(reference: String, cancelVisitDto: CancelVisitDto): VisitDto? = webClient.put()
    .uri("/visits/$reference/cancel")
    .body(BodyInserters.fromValue(cancelVisitDto))
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono<VisitDto>().block(apiTimeout)

  fun ignoreVisitNotification(reference: String, ignoreVisitNotifications: IgnoreVisitNotificationsDto): VisitDto? = webClient.put()
    .uri("/visits/notification/visit/$reference/ignore")
    .body(BodyInserters.fromValue(ignoreVisitNotifications))
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono<VisitDto>().block(apiTimeout)

  fun getVisitSessions(
    prisonId: String,
    prisonerId: String?,
    min: Int?,
    max: Int?,
    username: String?,
    userType: UserType,
  ): List<VisitSessionDto>? = webClient.get()
    .uri("/visit-sessions") {
      visitSessionsUriBuilder(prisonId, prisonerId, min, max, username, userType, it).build()
    }
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono<List<VisitSessionDto>>().block(apiTimeout)

  fun getAvailableVisitSessions(
    prisonId: String,
    prisonerId: String,
    sessionRestriction: SessionRestriction,
    dateRange: DateRange,
    excludedApplicationReference: String? = null,
    username: String? = null,
    userType: UserType,
  ): List<AvailableVisitSessionDto> {
    val uri = "/visit-sessions/available"

    return webClient.get()
      .uri(uri) {
        visitAvailableSessionsUriBuilder(it, prisonId, prisonerId, sessionRestriction, dateRange, excludedApplicationReference, username, userType).build()
      }
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<List<AvailableVisitSessionDto>>()
      .onErrorResume { e ->
        if (!isNotFoundError(e)) {
          LOG.error("getAvailableVisitSessions Failed for get request $uri ")
          Mono.error(e)
        } else {
          LOG.error("getAvailableVisitSessions returned NOT_FOUND for get request $uri")
          Mono.error { NotFoundException("getAvailableVisitSessions not found for get request $uri", e) }
        }
      }
      .blockOptional(apiTimeout).get()
  }

  fun getSupportedPrisons(type: UserType): List<String> {
    val uri = "/config/prisons/user-type/${type.name}/supported"
    return webClient.get()
      .uri(uri)
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<List<String>>()
      .onErrorResume { e ->
        if (!isNotFoundError(e)) {
          LOG.error("getSupportedPrisons Failed for get request $uri")
          Mono.error(e)
        } else {
          LOG.error("getSupportedPrisons NOT_FOUND for get request $uri")
          Mono.error { NotFoundException("No Supported prisons found for UserType - $type on visit-scheduler") }
        }
      }
      .blockOptional(apiTimeout).orElseThrow { NotFoundException("No Supported prisons found for UserType - $type on visit-scheduler") }
  }

  fun getSessionCapacity(
    prisonCode: String,
    sessionDate: LocalDate,
    sessionStartTime: LocalTime,
    sessionEndTime: LocalTime,
  ): SessionCapacityDto? = webClient.get()
    .uri("/visit-sessions/capacity") {
      visitSessionsCapacityUriBuilder(prisonCode, sessionDate, sessionStartTime, sessionEndTime, it).build()
    }
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono<SessionCapacityDto>().block(apiTimeout)

  fun getSession(
    prisonCode: String,
    sessionDate: LocalDate,
    sessionTemplateReference: String,
  ): VisitSessionDto? = webClient.get()
    .uri("/visit-sessions/session") {
      it.queryParam("prisonCode", prisonCode)
        .queryParam("sessionDate", sessionDate)
        .queryParam("sessionTemplateReference", sessionTemplateReference)
        .build()
    }
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono<VisitSessionDto>().block(apiTimeout)

  fun getSessionSchedule(
    prisonCode: String,
    sessionDate: LocalDate,
  ): List<SessionScheduleDto>? = webClient.get()
    .uri("/visit-sessions/schedule") {
      it.queryParam("prisonId", prisonCode)
        .queryParam("date", sessionDate)
        .build()
    }
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono<List<SessionScheduleDto>>().block(apiTimeout)

  fun processNonAssociations(sendDto: NonAssociationChangedNotificationDto) {
    webClient.post()
      .uri(VISIT_NOTIFICATION_NON_ASSOCIATION_CHANGE_PATH)
      .body(BodyInserters.fromValue(sendDto))
      .retrieve()
      .toBodilessEntity()
      .doOnError { e -> LOG.error("Could not processNonAssociations :", e) }
      .block(apiTimeout)
  }

  fun processPrisonerReceived(sendDto: PrisonerReceivedNotificationDto) {
    webClient.post()
      .uri(VISIT_NOTIFICATION_PRISONER_RECEIVED_CHANGE_PATH)
      .body(BodyInserters.fromValue(sendDto))
      .retrieve()
      .toBodilessEntity()
      .doOnError { e -> LOG.error("Could not processPrisonerReceived :", e) }
      .block(apiTimeout)
  }

  fun processPrisonerReleased(sendDto: PrisonerReleasedNotificationDto) {
    webClient.post()
      .uri(VISIT_NOTIFICATION_PRISONER_RELEASED_CHANGE_PATH)
      .body(BodyInserters.fromValue(sendDto))
      .retrieve()
      .toBodilessEntity()
      .doOnError { e -> LOG.error("Could not processPrisonerReleased :", e) }
      .block(apiTimeout)
  }

  fun processPersonRestrictionUpserted(sendDto: PersonRestrictionUpsertedNotificationDto) {
    webClient.post()
      .uri(VISIT_NOTIFICATION_PERSON_RESTRICTION_UPSERTED_PATH)
      .body(BodyInserters.fromValue(sendDto))
      .retrieve()
      .toBodilessEntity()
      .doOnError { e -> LOG.error("Could not processPersonRestrictionUpserted :", e) }
      .block(apiTimeout)
  }

  fun processPrisonerRestrictionChange(sendDto: PrisonerRestrictionChangeNotificationDto) {
    webClient.post()
      .uri(VISIT_NOTIFICATION_PRISONER_RESTRICTION_CHANGE_PATH)
      .body(BodyInserters.fromValue(sendDto))
      .retrieve()
      .toBodilessEntity()
      .doOnError { e -> LOG.error("Could not processPrisonerRestrictionChange :", e) }
      .block(apiTimeout)
  }

  fun processVisitorRestrictionUpserted(sendDto: VisitorRestrictionUpsertedNotificationDto) {
    webClient.post()
      .uri(VISIT_NOTIFICATION_VISITOR_RESTRICTION_UPSERTED_PATH)
      .body(BodyInserters.fromValue(sendDto))
      .retrieve()
      .toBodilessEntity()
      .doOnError { e -> LOG.error("Could not processVisitorRestrictionUpserted :", e) }
      .block(apiTimeout)
  }

  fun processVisitorUnapproved(sendDto: VisitorApprovedUnapprovedNotificationDto) {
    webClient.post()
      .uri(VISIT_NOTIFICATION_VISITOR_UNAPPROVED_PATH)
      .body(BodyInserters.fromValue(sendDto))
      .retrieve()
      .toBodilessEntity()
      .doOnError { e -> LOG.error("Could not processVisitorUnapproved :", e) }
      .block(apiTimeout)
  }

  fun processCourtVideoAppointmentCreated(sendDto: CourtVideoAppointmentNotificationDto) {
    webClient.post()
      .uri(VISIT_NOTIFICATION_COURT_VIDEO_APPOINTMENT_CREATED_PATH)
      .body(BodyInserters.fromValue(sendDto))
      .retrieve()
      .toBodilessEntity()
      .doOnError { e -> LOG.error("Could not processCourtVideoAppointmentCreated :", e) }
      .block(apiTimeout)
  }

  fun processCourtVideoAppointmentUpdated(sendDto: CourtVideoAppointmentNotificationDto) {
    webClient.post()
      .uri(VISIT_NOTIFICATION_COURT_VIDEO_APPOINTMENT_UPDATED_PATH)
      .body(BodyInserters.fromValue(sendDto))
      .retrieve()
      .toBodilessEntity()
      .doOnError { e -> LOG.error("Could not processCourtVideoAppointmentUpdated :", e) }
      .block(apiTimeout)
  }

  fun processCourtVideoAppointmentCancelledDeleted(sendDto: CourtVideoAppointmentNotificationDto) {
    webClient.post()
      .uri(VISIT_NOTIFICATION_COURT_VIDEO_APPOINTMENT_CANCELLED_DELETED_PATH)
      .body(BodyInserters.fromValue(sendDto))
      .retrieve()
      .toBodilessEntity()
      .doOnError { e -> LOG.error("Could not processCourtVideoAppointmentCancelledDeleted :", e) }
      .block(apiTimeout)
  }

  fun processVisitorApproved(sendDto: VisitorApprovedUnapprovedNotificationDto) {
    webClient.post()
      .uri(VISIT_NOTIFICATION_VISITOR_APPROVED_PATH)
      .body(BodyInserters.fromValue(sendDto))
      .retrieve()
      .toBodilessEntity()
      .doOnError { e -> LOG.error("Could not processVisitorApproved :", e) }
      .block(apiTimeout)
  }

  fun processPrisonerAlertsUpdated(sendDto: PrisonerAlertsAddedNotificationDto) {
    webClient.post()
      .uri(VISIT_NOTIFICATION_PRISONER_ALERTS_UPDATED_PATH)
      .body(BodyInserters.fromValue(sendDto))
      .retrieve()
      .toBodilessEntity()
      .doOnError { e -> LOG.error("Could not process prisoner alert updates :", e) }
      .block(apiTimeout)
  }

  fun getNotificationCountForPrison(prisonCode: String, notificationEventTypes: List<String>?): NotificationCountDto? = webClient.get()
    .uri("/visits/notification/$prisonCode/count") {
      visitNotificationTypesUriBuilder(notificationEventTypes, it).build()
    }
    .retrieve()
    .bodyToMono<NotificationCountDto>().block(apiTimeout)

  fun getVisitRequestsCountForPrison(prisonCode: String): VisitRequestsCountDto? = webClient.get()
    .uri("/visits/requests/$prisonCode/count")
    .retrieve()
    .bodyToMono<VisitRequestsCountDto>()
    .block(apiTimeout)

  fun getFutureVisitsWithNotifications(prisonCode: String, notificationEventTypes: List<String>?): List<VisitNotificationsDto>? = webClient.get()
    .uri("/visits/notification/$prisonCode/visits") {
      visitNotificationTypesUriBuilder(notificationEventTypes, it).build()
    }
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono<List<VisitNotificationsDto>>().block(apiTimeout)

  fun getVisitRequestsForPrison(prisonCode: String): List<VisitRequestSummaryDto>? = webClient.get()
    .uri("/visits/requests/$prisonCode")
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono<List<VisitRequestSummaryDto>>().block(apiTimeout)

  fun approveVisitRequestByReference(approveVisitRequestResponseDto: ApproveVisitRequestBodyDto): VisitDto? = webClient.put()
    .uri("/visits/requests/${approveVisitRequestResponseDto.visitReference}/approve")
    .body(BodyInserters.fromValue(approveVisitRequestResponseDto))
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono<VisitDto>()
    .block(apiTimeout)

  fun rejectVisitRequestByReference(rejectVisitRequestBodyDto: RejectVisitRequestBodyDto): VisitDto? = webClient.put()
    .uri("/visits/requests/${rejectVisitRequestBodyDto.visitReference}/reject")
    .body(BodyInserters.fromValue(rejectVisitRequestBodyDto))
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono<VisitDto>()
    .block(apiTimeout)

  fun getPrison(prisonCode: String): VisitSchedulerPrisonDto {
    val uri = "/admin/prisons/prison/$prisonCode"

    return webClient.get()
      .uri(uri)
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<VisitSchedulerPrisonDto>()
      .onErrorResume { e ->
        if (!isNotFoundError(e)) {
          LOG.error("getPrison Failed for get request $uri")
          Mono.error(e)
        } else {
          LOG.error("getPrison NOT_FOUND for get request $uri")
          Mono.error { NotFoundException("Prison with prison code - $prisonCode not found on visit-scheduler") }
        }
      }
      .blockOptional(apiTimeout).orElseThrow { NotFoundException("Prison with prison code - $prisonCode not found on visit-scheduler") }
  }

  fun getNotificationEventsForBookingReferenceAsMono(reference: String): Mono<List<VisitNotificationEventDto>> = webClient.get()
    .uri("/visits/notification/visit/$reference/events")
    .retrieve()
    .bodyToMono<List<VisitNotificationEventDto>>()

  fun getPrisonExcludeDates(prisonCode: String): List<ExcludeDateDto>? = webClient.get()
    .uri("/prisons/prison/$prisonCode/exclude-date")
    .retrieve()
    .bodyToMono<List<ExcludeDateDto>>().block(apiTimeout)

  fun addPrisonExcludeDate(prisonCode: String, prisonExcludeDate: ExcludeDateDto): List<LocalDate>? = webClient.put()
    .uri("/prisons/prison/$prisonCode/exclude-date/add")
    .body(BodyInserters.fromValue(prisonExcludeDate))
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono<List<LocalDate>>().block(apiTimeout)

  fun removePrisonExcludeDate(prisonCode: String, prisonExcludeDate: ExcludeDateDto): List<LocalDate>? = webClient.put()
    .uri("/prisons/prison/$prisonCode/exclude-date/remove")
    .body(BodyInserters.fromValue(prisonExcludeDate))
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono<List<LocalDate>>().block(apiTimeout)

  fun getSessionTemplateExcludeDates(sessionTemplateReference: String): List<ExcludeDateDto>? = webClient.get()
    .uri("/admin/session-templates/template/$sessionTemplateReference/exclude-date")
    .retrieve()
    .bodyToMono<List<ExcludeDateDto>>().block(apiTimeout)

  fun addSessionTemplateExcludeDate(sessionTemplateReference: String, sessionExcludeDate: ExcludeDateDto): List<LocalDate>? = webClient.put()
    .uri("/admin/session-templates/template/$sessionTemplateReference/exclude-date/add")
    .body(BodyInserters.fromValue(sessionExcludeDate))
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono<List<LocalDate>>().block(apiTimeout)

  fun removeSessionTemplateExcludeDate(sessionTemplateReference: String, sessionExcludeDate: ExcludeDateDto): List<LocalDate>? = webClient.put()
    .uri("/admin/session-templates/template/$sessionTemplateReference/exclude-date/remove")
    .body(BodyInserters.fromValue(sessionExcludeDate))
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono<List<LocalDate>>().block(apiTimeout)

  fun createVisitFromExternalSystem(createVisitFromExternalSystemDto: CreateVisitFromExternalSystemDto): VisitDto? = webClient.post()
    .uri(POST_VISIT_FROM_EXTERNAL_SYSTEM)
    .body(BodyInserters.fromValue(createVisitFromExternalSystemDto))
    .retrieve()
    .bodyToMono<VisitDto>()
    .onErrorResume { e ->
      LOG.error("Could not create visit from external system :", e)
      Mono.error(e)
    }
    .block(apiTimeout)

  fun getBookerHistoryAsMono(bookerReference: String): Mono<List<EventAuditDto>> = webClient.get()
    .uri(GET_VISIT_EVENTS_BY_BOOKER_REFERENCE.replace("{bookerReference}", bookerReference))
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono<List<EventAuditDto>>()

  fun findLastApprovedDateForVisitor(prisonerId: String, nomisPersonIds: List<Long>): List<VisitorLastApprovedDatesDto>? = webClient.post()
    .uri(FIND_LAST_APPROVED_DATE_FOR_VISITORS_BY_PRISONER.replace("{prisonerNumber}", prisonerId))
    .body(BodyInserters.fromValue(VisitorLastApprovedDateRequestDto(nomisPersonIds)))
    .retrieve()
    .bodyToMono<List<VisitorLastApprovedDatesDto>>()
    .onErrorResume { e ->
      if (!isNotFoundError(e)) {
        logger.error("findLastApprovedDateForVisitor - failed with unrecoverable error, visitors with NOMIS person ids - $nomisPersonIds, prisoner - $prisonerId")
        Mono.error(e)
      } else {
        logger.error("findLastApprovedDateForVisitor - failed with NOT_FOUND error, visitors with NOMIS person ids - $nomisPersonIds, prisoner - $prisonerId")
        Mono.empty()
      }
    }
    .block(apiTimeout)

  private fun visitSearchUriBuilder(visitSearchRequestFilter: VisitSearchRequestFilter, uriBuilder: UriBuilder): UriBuilder {
    uriBuilder.queryParamIfPresent("prisonId", Optional.ofNullable(visitSearchRequestFilter.prisonCode))
    uriBuilder.queryParamIfPresent("prisonerId", Optional.ofNullable(visitSearchRequestFilter.prisonerId))
    uriBuilder.queryParam("visitStatus", visitSearchRequestFilter.visitStatusList.toTypedArray())
    uriBuilder.queryParamIfPresent("visitStartDate", Optional.ofNullable(visitSearchRequestFilter.visitStartDate))
    uriBuilder.queryParamIfPresent("visitEndDate", Optional.ofNullable(visitSearchRequestFilter.visitEndDate))
    uriBuilder.queryParam("page", visitSearchRequestFilter.page)
    uriBuilder.queryParam("size", visitSearchRequestFilter.size)
    return uriBuilder
  }

  private fun visitSessionsUriBuilder(prisonId: String, prisonerId: String?, min: Int?, max: Int?, username: String?, userType: UserType, uriBuilder: UriBuilder): UriBuilder {
    uriBuilder.queryParam("prisonId", prisonId)
    uriBuilder.queryParamIfPresent("prisonerId", Optional.ofNullable(prisonerId))
    uriBuilder.queryParamIfPresent("min", Optional.ofNullable(min))
    uriBuilder.queryParamIfPresent("max", Optional.ofNullable(max))
    uriBuilder.queryParamIfPresent("username", Optional.ofNullable(username))
    uriBuilder.queryParam("userType", userType.name)
    return uriBuilder
  }

  private fun visitAvailableSessionsUriBuilder(
    uriBuilder: UriBuilder,
    prisonId: String,
    prisonerId: String,
    sessionRestriction: SessionRestriction,
    dateRange: DateRange,
    excludedApplicationReference: String? = null,
    username: String? = null,
    userType: UserType,
  ): UriBuilder {
    uriBuilder.queryParam("prisonId", prisonId)
    uriBuilder.queryParam("prisonerId", prisonerId)
    uriBuilder.queryParam("sessionRestriction", sessionRestriction.name)
    uriBuilder.queryParam("fromDate", dateRange.fromDate)
    uriBuilder.queryParam("toDate", dateRange.toDate)
    excludedApplicationReference?.let {
      uriBuilder.queryParam("excludedApplicationReference", it)
    }
    username?.let {
      uriBuilder.queryParam("username", it)
    }
    uriBuilder.queryParam("userType", userType.name)
    return uriBuilder
  }

  private fun visitSessionsCapacityUriBuilder(
    prisonCode: String,
    sessionDate: LocalDate,
    sessionStartTime: LocalTime,
    sessionEndTime: LocalTime,
    uriBuilder: UriBuilder,
  ): UriBuilder {
    uriBuilder.queryParam("prisonId", prisonCode)
    uriBuilder.queryParam("sessionDate", sessionDate)
    uriBuilder.queryParam("sessionStartTime", sessionStartTime)
    uriBuilder.queryParam("sessionEndTime", sessionEndTime)

    return uriBuilder
  }

  private fun visitNotificationTypesUriBuilder(
    notificationEventTypes: List<String>?,
    uriBuilder: UriBuilder,
  ): UriBuilder {
    uriBuilder.queryParamIfPresent("types", Optional.ofNullable(notificationEventTypes))
      .build()
    return uriBuilder
  }
}
