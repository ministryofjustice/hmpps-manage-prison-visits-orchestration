package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
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
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config.ApplicationValidationErrorResponse
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.RestPage
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.AvailableVisitSessionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.BookingRequestDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.CancelVisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.DateRange
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.EventAuditDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.IgnoreVisitNotificationsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionCapacityDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionScheduleDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSchedulerPrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSessionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionRestriction
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitRestriction
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitStatus
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.prisons.PrisonExcludeDateDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.NonAssociationChangedNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.NotificationCountDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.NotificationEventType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.NotificationGroupDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.PersonRestrictionUpsertedNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.PrisonerAlertsAddedNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.PrisonerReceivedNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.PrisonerReleasedNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.PrisonerRestrictionChangeNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.VisitorRestrictionUpsertedNotificationDto
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
const val VISIT_NOTIFICATION_NON_ASSOCIATION_CHANGE_PATH: String = "$VISIT_NOTIFICATION_CONTROLLER_PATH/non-association/changed"
const val VISIT_NOTIFICATION_PERSON_RESTRICTION_UPSERTED_PATH: String = "$VISIT_NOTIFICATION_CONTROLLER_PATH/person/restriction/upserted"
const val VISIT_NOTIFICATION_PRISONER_RECEIVED_CHANGE_PATH: String = "$VISIT_NOTIFICATION_CONTROLLER_PATH/prisoner/received"
const val VISIT_NOTIFICATION_PRISONER_RELEASED_CHANGE_PATH: String = "$VISIT_NOTIFICATION_CONTROLLER_PATH/prisoner/released"
const val VISIT_NOTIFICATION_PRISONER_RESTRICTION_CHANGE_PATH: String = "$VISIT_NOTIFICATION_CONTROLLER_PATH/prisoner/restriction/changed"
const val VISIT_NOTIFICATION_VISITOR_RESTRICTION_UPSERTED_PATH: String = "$VISIT_NOTIFICATION_CONTROLLER_PATH/visitor/restriction/upserted"

const val GET_FUTURE_BOOKED_PUBLIC_VISITS_BY_BOOKER_REFERENCE: String = "/public/booker/{bookerReference}/visits/booked/future"
const val GET_CANCELLED_PUBLIC_VISITS_BY_BOOKER_REFERENCE: String = "/public/booker/{bookerReference}/visits/cancelled"
const val GET_PAST_BOOKED_PUBLIC_VISITS_BY_BOOKER_REFERENCE: String = "/public/booker/{bookerReference}/visits/booked/past"

const val VISIT_NOTIFICATION_PRISONER_ALERTS_UPDATED_PATH: String = "$VISIT_NOTIFICATION_CONTROLLER_PATH/prisoner/alerts/updated"

@Component
class VisitSchedulerClient(
  val objectMapper: ObjectMapper,
  @Qualifier("visitSchedulerWebClient") private val webClient: WebClient,
  @Value("\${visit-scheduler.api.timeout:10s}") val apiTimeout: Duration,
) {

  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getPastPublicBookedVisitsByBookerReference(bookerReference: String): List<VisitDto> {
    return webClient.get()
      .uri(GET_PAST_BOOKED_PUBLIC_VISITS_BY_BOOKER_REFERENCE.replace("{bookerReference}", bookerReference))
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<List<VisitDto>>()
      .blockOptional(apiTimeout).orElseGet { listOf<VisitDto>() }
  }

  fun getCancelledPublicVisitsByBookerReference(bookerReference: String): List<VisitDto> {
    return webClient.get()
      .uri(GET_CANCELLED_PUBLIC_VISITS_BY_BOOKER_REFERENCE.replace("{bookerReference}", bookerReference))
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<List<VisitDto>>()
      .blockOptional(apiTimeout).orElseGet { listOf<VisitDto>() }
  }

  fun getFuturePublicBookedVisitsByBookerReference(bookerReference: String): List<VisitDto> {
    return webClient.get()
      .uri(GET_FUTURE_BOOKED_PUBLIC_VISITS_BY_BOOKER_REFERENCE.replace("{bookerReference}", bookerReference))
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<List<VisitDto>>()
      .blockOptional(apiTimeout).orElseGet { listOf() }
  }

  fun getVisitByReference(reference: String): VisitDto? {
    return webClient.get()
      .uri("/visits/$reference")
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<VisitDto>().block(apiTimeout)
  }

  fun getVisitHistoryByReference(reference: String): List<EventAuditDto>? {
    return webClient.get()
      .uri(GET_VISIT_HISTORY_CONTROLLER_PATH.replace("{reference}", reference))
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<List<EventAuditDto>>().block(apiTimeout)
  }

  fun getVisits(visitSearchRequestFilter: VisitSearchRequestFilter): RestPage<VisitDto>? {
    return getVisitsAsMono(visitSearchRequestFilter).block(apiTimeout)
  }

  fun getFutureVisitsForPrisoner(prisonerId: String): List<VisitDto>? {
    return webClient.get()
      .uri("/visits/search/future/$prisonerId")
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<List<VisitDto>>().block(apiTimeout)
  }

  fun getVisitsForSessionTemplateAndDate(
    sessionTemplateReference: String?,
    sessionDate: LocalDate,
    visitStatusList: List<VisitStatus>,
    visitRestrictions: List<VisitRestriction>?,
    prisonCode: String,
    page: Int,
    size: Int,
  ): RestPage<VisitDto>? {
    return webClient.get()
      .uri("/visits/session-template") {
        it.queryParamIfPresent("sessionTemplateReference", Optional.ofNullable(sessionTemplateReference))
          .queryParam("fromDate", sessionDate)
          .queryParam("toDate", sessionDate)
          .queryParamIfPresent("visitRestrictions", Optional.ofNullable(visitRestrictions))
          .queryParam("visitStatus", visitStatusList)
          .queryParam("prisonCode", prisonCode)
          .queryParam("page", page)
          .queryParam("size", size)
          .build()
      }
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<RestPage<VisitDto>>()
      .block(apiTimeout)
  }

  fun getVisitsAsMono(visitSearchRequestFilter: VisitSearchRequestFilter): Mono<RestPage<VisitDto>> {
    return webClient.get()
      .uri("/visits/search") {
        visitSearchUriBuilder(visitSearchRequestFilter, it).build()
      }
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono()
  }

  fun bookVisitSlot(applicationReference: String, requestDto: BookingRequestDto): VisitDto? {
    return webClient.put()
      .uri("/visits/$applicationReference/book")
      .body(BodyInserters.fromValue(requestDto))
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<VisitDto>().onErrorResume {
          e ->
        if (isUnprocessableEntityError(e)) {
          val exception = getBookVisitApplicationValidationErrorResponse(e)
          Mono.error(exception)
        } else {
          Mono.error(e)
        }
      }.block(apiTimeout)
  }

  fun updateBookedVisit(applicationReference: String, requestDto: BookingRequestDto): VisitDto? {
    return webClient.put()
      .uri("/visits/$applicationReference/visit/update")
      .body(BodyInserters.fromValue(requestDto))
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<VisitDto>().onErrorResume {
          e ->
        if (isUnprocessableEntityError(e)) {
          val exception = getBookVisitApplicationValidationErrorResponse(e)
          Mono.error(exception)
        } else {
          Mono.error(e)
        }
      }.block(apiTimeout)
  }

  fun getBookedVisitByApplicationReference(applicationReference: String): VisitDto? {
    return webClient.get()
      .uri("/visits/$applicationReference/visit")
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<VisitDto>()
      .onErrorResume {
          e ->
        if (!isNotFoundError(e)) {
          LOG.error("getBookedVisitByApplicationReference Failed to search for existing booking")
          Mono.error(e)
        } else {
          LOG.info("getBookedVisitByApplicationReference NOT FOUND response - no existing booking")
          Mono.empty()
        }
      }
      .block(apiTimeout)
  }

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

  fun cancelVisit(reference: String, cancelVisitDto: CancelVisitDto): VisitDto? {
    return webClient.put()
      .uri("/visits/$reference/cancel")
      .body(BodyInserters.fromValue(cancelVisitDto))
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<VisitDto>().block(apiTimeout)
  }

  fun ignoreVisitNotification(reference: String, ignoreVisitNotifications: IgnoreVisitNotificationsDto): VisitDto? {
    return webClient.put()
      .uri("/visits/notification/visit/$reference/ignore")
      .body(BodyInserters.fromValue(ignoreVisitNotifications))
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<VisitDto>().block(apiTimeout)
  }

  fun getVisitSessions(prisonId: String, prisonerId: String?, min: Int?, max: Int?, username: String?): List<VisitSessionDto>? {
    return webClient.get()
      .uri("/visit-sessions") {
        visitSessionsUriBuilder(prisonId, prisonerId, min, max, username, it).build()
      }
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<List<VisitSessionDto>>().block(apiTimeout)
  }

  fun getAvailableVisitSessions(
    prisonId: String,
    prisonerId: String,
    sessionRestriction: SessionRestriction,
    dateRange: DateRange,
    excludedApplicationReference: String? = null,
    username: String? = null,
  ): List<AvailableVisitSessionDto> {
    val uri = "/visit-sessions/available"

    return webClient.get()
      .uri(uri) {
        visitAvailableSessionsUriBuilder(it, prisonId, prisonerId, sessionRestriction, dateRange, excludedApplicationReference, username).build()
      }
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<List<AvailableVisitSessionDto>>()
      .onErrorResume {
          e ->
        if (!isNotFoundError(e)) {
          LOG.error("getAvailableVisitSessions Failed for get request $uri")
          Mono.error(e)
        } else {
          LOG.error("getAvailableVisitSessions returned NOT_FOUND for get request $uri")
          Mono.error { NotFoundException("getAvailableVisitSessions not found for - $prisonId on prison-api", e) }
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
      .onErrorResume {
          e ->
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
  ): SessionCapacityDto? {
    return webClient.get()
      .uri("/visit-sessions/capacity") {
        visitSessionsCapacityUriBuilder(prisonCode, sessionDate, sessionStartTime, sessionEndTime, it).build()
      }
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<SessionCapacityDto>().block(apiTimeout)
  }

  fun getSessionSchedule(
    prisonCode: String,
    sessionDate: LocalDate,
  ): List<SessionScheduleDto>? {
    return webClient.get()
      .uri("/visit-sessions/schedule") {
        it.queryParam("prisonId", prisonCode)
          .queryParam("date", sessionDate)
          .build()
      }
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<List<SessionScheduleDto>>().block(apiTimeout)
  }

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

  fun processPrisonerAlertsUpdated(sendDto: PrisonerAlertsAddedNotificationDto) {
    webClient.post()
      .uri(VISIT_NOTIFICATION_PRISONER_ALERTS_UPDATED_PATH)
      .body(BodyInserters.fromValue(sendDto))
      .retrieve()
      .toBodilessEntity()
      .doOnError { e -> LOG.error("Could not process prisoner alert updates :", e) }
      .block(apiTimeout)
  }

  fun getNotificationCountForPrison(prisonCode: String): NotificationCountDto? {
    return webClient.get()
      .uri("/visits/notification/$prisonCode/count")
      .retrieve()
      .bodyToMono<NotificationCountDto>().block(apiTimeout)
  }

  fun getFutureNotificationVisitGroups(prisonCode: String): List<NotificationGroupDto>? {
    return webClient.get()
      .uri("/visits/notification/$prisonCode/groups")
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<List<NotificationGroupDto>>().block(apiTimeout)
  }

  fun getNotificationCount(): NotificationCountDto? {
    return webClient.get()
      .uri("/visits/notification/count")
      .retrieve()
      .bodyToMono<NotificationCountDto>().block(apiTimeout)
  }
  fun getPrison(prisonCode: String): VisitSchedulerPrisonDto {
    val uri = "/admin/prisons/prison/$prisonCode"

    return webClient.get()
      .uri(uri)
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<VisitSchedulerPrisonDto>()
      .onErrorResume {
          e ->
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

  fun getPrisonAsMono(prisonCode: String): Mono<Optional<VisitSchedulerPrisonDto>> {
    val uri = "/admin/prisons/prison/$prisonCode"

    return webClient.get()
      .uri(uri)
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<Optional<VisitSchedulerPrisonDto>>()
      .onErrorResume { e ->
        if (e is WebClientResponseException && e.statusCode == HttpStatus.NOT_FOUND) {
          // return an Optional.empty element if 404 is thrown
          return@onErrorResume Mono.just(Optional.empty())
        } else {
          Mono.error(e)
        }
      }
  }

  fun getNotificationsTypesForBookingReference(reference: String): List<NotificationEventType>? {
    return webClient.get()
      .uri("/visits/notification/visit/$reference/types")
      .retrieve()
      .bodyToMono<List<NotificationEventType>>().block(apiTimeout)
  }

  fun getPrisonExcludeDates(prisonCode: String): List<PrisonExcludeDateDto>? {
    return webClient.get()
      .uri("/prisons/prison/$prisonCode/exclude-date")
      .retrieve()
      .bodyToMono<List<PrisonExcludeDateDto>>().block(apiTimeout)
  }

  fun addPrisonExcludeDate(prisonCode: String, prisonExcludeDate: PrisonExcludeDateDto): List<LocalDate>? {
    return webClient.put()
      .uri("/prisons/prison/$prisonCode/exclude-date/add")
      .body(BodyInserters.fromValue(prisonExcludeDate))
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<List<LocalDate>>().block(apiTimeout)
  }

  private fun visitSearchUriBuilder(visitSearchRequestFilter: VisitSearchRequestFilter, uriBuilder: UriBuilder): UriBuilder {
    uriBuilder.queryParamIfPresent("prisonId", Optional.ofNullable(visitSearchRequestFilter.prisonCode))
    uriBuilder.queryParamIfPresent("prisonerId", Optional.ofNullable(visitSearchRequestFilter.prisonerId))
    uriBuilder.queryParam("visitStatus", visitSearchRequestFilter.visitStatusList)
    uriBuilder.queryParamIfPresent("visitStartDate", Optional.ofNullable(visitSearchRequestFilter.visitStartDate))
    uriBuilder.queryParamIfPresent("visitEndDate", Optional.ofNullable(visitSearchRequestFilter.visitEndDate))
    uriBuilder.queryParam("page", visitSearchRequestFilter.page)
    uriBuilder.queryParam("size", visitSearchRequestFilter.size)
    return uriBuilder
  }

  private fun visitSessionsUriBuilder(prisonId: String, prisonerId: String?, min: Int?, max: Int?, username: String?, uriBuilder: UriBuilder): UriBuilder {
    uriBuilder.queryParam("prisonId", prisonId)
    uriBuilder.queryParamIfPresent("prisonerId", Optional.ofNullable(prisonerId))
    uriBuilder.queryParamIfPresent("min", Optional.ofNullable(min))
    uriBuilder.queryParamIfPresent("max", Optional.ofNullable(max))
    uriBuilder.queryParamIfPresent("username", Optional.ofNullable(username))
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
}
