package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonerContactRegistryClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitSchedulerClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.alerts.api.enums.RestrictionsForReview
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.AvailableVisitSessionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.AvailableVisitSessionRestrictionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.DateRange
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.IndefiniteDateRange
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionCapacityDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionScheduleDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSessionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionRestriction
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionRestriction.CLOSED
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionRestriction.OPEN
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.prisons.ExcludeDateDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.prisons.IsExcludeDateDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.whereabouts.ScheduledEventDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.DateRangeNotFoundException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.PrisonService.Companion.logger
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.utils.DateUtils
import java.time.LocalDate
import java.time.LocalTime

@Service
class VisitSchedulerSessionsService(
  private val visitSchedulerClient: VisitSchedulerClient,
  private val appointmentsService: AppointmentsService,
  private val prisonerProfileService: PrisonerProfileService,
  private val prisonService: PrisonService,
  private val excludeDatesService: ExcludeDatesService,
  private val dateUtils: DateUtils,
  private val prisonApiClient: PrisonApiClient,
  private val prisonerContactRegistryClient: PrisonerContactRegistryClient,

  @param:Value("\${public.service.from-date-override: 2}")
  private val publicServiceFromDateOverride: Long,
  @param:Value("\${public.service.to-date-override: 28}")
  private val publicServiceToDateOverride: Long,
) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
    const val CLASHING_APPOINTMENT_LOG_MSG =
      "Visit session for prisonerId - {}, date - {}, start time - {}, end time - {} is unavailable as it clashes with {} medical / legal appointment(s), appointment details - {}"

    val availableVisitSessionsSortOrder: Comparator<AvailableVisitSessionDto> = compareBy(
      { it.sessionDate },
      { it.sessionTimeSlot.startTime },
      { it.sessionTimeSlot.endTime },
    )
  }

  fun getVisitSessions(
    prisonCode: String,
    prisonerId: String?,
    min: Int?,
    max: Int?,
    username: String?,
    userType: UserType,
  ): List<VisitSessionDto>? = visitSchedulerClient.getVisitSessions(prisonCode, prisonerId, min, max, username, userType)

  fun getAvailableVisitSessions(
    prisonCode: String,
    prisonerId: String,
    requestedSessionRestriction: SessionRestriction?,
    visitors: List<Long>?,
    withAppointmentsCheck: Boolean,
    excludedApplicationReference: String? = null,
    pvbAdvanceFromDateByDays: Int,
    fromDateOverride: Int? = null,
    toDateOverride: Int? = null,
    username: String? = null,
    userType: UserType,
  ): List<AvailableVisitSessionDto> {
    val sessionRestriction = updateRequestedRestriction(requestedSessionRestriction, prisonerId, visitors)

    // advance from date by n days
    var dateRange = prisonService.getToDaysBookableDateRange(prisonCode = prisonCode, fromDateOverride = fromDateOverride, toDateOverride = toDateOverride)
    dateRange = dateUtils.advanceFromDate(dateRange, pvbAdvanceFromDateByDays)

    var availableVisitSessions = getAvailableVisitSessionsForDateRange(
      prisonCode = prisonCode,
      prisonerId = prisonerId,
      visitors = visitors,
      excludedApplicationReference = excludedApplicationReference,
      username = username,
      userType = userType,
      dateRange = dateRange,
      sessionRestriction = sessionRestriction,
    )

    if (withAppointmentsCheck && availableVisitSessions.isNotEmpty()) {
      availableVisitSessions = filterAvailableVisitsByHigherPriorityAppointments(prisonerId, dateRange, availableVisitSessions)
    }

    return availableVisitSessions.sortedWith(availableVisitSessionsSortOrder)
  }

  // this method is used by the V2 endpoint
  fun getAvailableVisitSessions(
    prisonCode: String,
    prisonerId: String,
    requestedSessionRestriction: SessionRestriction?,
    visitors: List<Long>?,
    excludedApplicationReference: String? = null,
    username: String? = null,
    userType: UserType,
  ): List<AvailableVisitSessionDto> {
    val sessionRestriction = updateRequestedRestriction(requestedSessionRestriction, prisonerId, visitors)

    val dateRange = prisonService.getToDaysBookableDateRange(prisonCode = prisonCode, fromDateOverride = publicServiceFromDateOverride.toInt(), toDateOverride = publicServiceToDateOverride.toInt())

    val availableVisitSessions = getAvailableVisitSessionsForDateRange(
      prisonCode = prisonCode,
      prisonerId = prisonerId,
      visitors = visitors,
      excludedApplicationReference = excludedApplicationReference,
      username = username,
      userType = userType,
      dateRange = dateRange,
      sessionRestriction = sessionRestriction,
    ).takeIf { it.isNotEmpty() }?.let {
      filterAvailableVisitsByHigherPriorityAppointments(prisonerId, dateRange, it)
    }?.takeIf { it.isNotEmpty() }?.also {
      setSessionForReview(prisonerId, visitors, dateRange, it)
    } ?: emptyList()

    return availableVisitSessions.sortedWith(availableVisitSessionsSortOrder)
  }

  private fun getAvailableVisitSessionsForDateRange(
    prisonCode: String,
    prisonerId: String,
    visitors: List<Long>?,
    excludedApplicationReference: String? = null,
    username: String? = null,
    userType: UserType,
    dateRange: DateRange,
    sessionRestriction: SessionRestriction,
  ): List<AvailableVisitSessionDto> {
    LOG.debug("getting available visit sessions for prisonerId - {}, dateRange - {}, sessionRestriction - {}, excludedApplicationReference - {}, username - {}, userType - {}, dateRange - {}, sessionRestriction - {}", prisonerId, dateRange, sessionRestriction, excludedApplicationReference, username, userType, dateRange, sessionRestriction)
    val sessions = try {
      val updatedDateRange =
        visitors?.let { prisonerProfileService.getBannedRestrictionDateRage(prisonerId, visitors, dateRange) }
          ?: dateRange
      visitSchedulerClient.getAvailableVisitSessions(
        prisonId = prisonCode,
        prisonerId = prisonerId,
        sessionRestriction = sessionRestriction,
        dateRange = updatedDateRange,
        excludedApplicationReference = excludedApplicationReference,
        username = username,
        userType = userType,
      )
    } catch (e: DateRangeNotFoundException) {
      LOG.error("getAvailableVisitSessions range is not returned therefore we do not have a valid date range and should return an empty list")
      emptyList()
    }

    return sessions
  }

  fun getSessionCapacity(
    prisonCode: String,
    sessionDate: LocalDate,
    sessionStartTime: LocalTime,
    sessionEndTime: LocalTime,
  ): SessionCapacityDto? = visitSchedulerClient.getSessionCapacity(prisonCode, sessionDate, sessionStartTime, sessionEndTime)

  fun getSession(
    prisonCode: String,
    sessionDate: LocalDate,
    sessionTemplateReference: String,
  ): VisitSessionDto? = visitSchedulerClient.getSession(prisonCode, sessionDate, sessionTemplateReference)

  fun getSessionSchedule(prisonCode: String, sessionDate: LocalDate): List<SessionScheduleDto>? = visitSchedulerClient.getSessionSchedule(prisonCode, sessionDate)

  private fun updateRequestedRestriction(
    requestedSessionRestriction: SessionRestriction?,
    prisonerId: String,
    visitors: List<Long>?,
  ): SessionRestriction = if (prisonerProfileService.hasPrisonerGotClosedRestrictions(prisonerId) ||
    (if (visitors != null) prisonerProfileService.hasVisitorsGotClosedRestrictions(prisonerId, visitors) else false)
  ) {
    CLOSED
  } else {
    requestedSessionRestriction ?: OPEN
  }

  fun getAvailableVisitSessionsRestriction(
    prisonerId: String,
    visitors: List<Long>?,
  ): AvailableVisitSessionRestrictionDto {
    val sessionRestriction = updateRequestedRestriction(requestedSessionRestriction = null, prisonerId, visitors)
    return AvailableVisitSessionRestrictionDto(sessionRestriction)
  }

  fun getFutureExcludeDatesForSessionTemplate(sessionTemplateReference: String): List<ExcludeDateDto> {
    val excludeDates = getExcludeDatesForSessionTemplate(sessionTemplateReference)
    return excludeDatesService.getFutureExcludeDates(excludeDates)
  }

  fun getPastExcludeDatesForSessionTemplate(sessionTemplateReference: String): List<ExcludeDateDto> {
    val excludeDates = getExcludeDatesForSessionTemplate(sessionTemplateReference)
    return excludeDatesService.getPastExcludeDates(excludeDates)
  }

  fun isDateExcludedForSessionTemplateVisits(sessionTemplateReference: String, date: LocalDate): IsExcludeDateDto {
    logger.trace("isDateExcluded - session template - {}, date - {}", sessionTemplateReference, date)
    val excludeDates = getExcludeDatesForSessionTemplate(sessionTemplateReference)
    val isExcluded = excludeDatesService.isDateExcluded(excludeDates, date)
    logger.trace("isDateExcluded - session template - {}, date - {}, isExcluded - {}", sessionTemplateReference, date, isExcluded)
    return isExcluded
  }

  fun addExcludeDateForSessionTemplate(sessionTemplateReference: String, sessionExcludeDate: ExcludeDateDto): List<LocalDate> = visitSchedulerClient.addSessionTemplateExcludeDate(sessionTemplateReference, sessionExcludeDate)?.sortedByDescending { it } ?: emptyList()

  fun removeExcludeDateForSessionTemplate(sessionTemplateReference: String, sessionExcludeDate: ExcludeDateDto): List<LocalDate> = visitSchedulerClient.removeSessionTemplateExcludeDate(sessionTemplateReference, sessionExcludeDate)?.sortedByDescending { it } ?: emptyList()

  private fun filterAvailableVisitsByHigherPriorityAppointments(prisonerId: String, dateRange: DateRange, availableVisitSessions: List<AvailableVisitSessionDto>): List<AvailableVisitSessionDto> {
    val higherPriorityAppointments = appointmentsService.getHigherPriorityAppointments(prisonerId, dateRange.fromDate, dateRange.toDate)
    return availableVisitSessions.filterNot { availableSession ->
      val higherPriorityAppointmentsForDate = getHigherPriorityAppointmentsForDate(higherPriorityAppointments, availableSession.sessionDate)
      val overridingAppointments = getOverridingAppointments(availableSession, higherPriorityAppointmentsForDate)
      overridingAppointments.isNotEmpty().also { hasOverridingAppointment ->
        if (hasOverridingAppointment) {
          logClashingAppointment(prisonerId, availableSession, overridingAppointments)
        }
      }
    }
  }

  private fun getHigherPriorityAppointmentsForDate(
    higherPriorityAppointments: List<ScheduledEventDto>,
    sessionDate: LocalDate,
  ): List<ScheduledEventDto> = higherPriorityAppointments.filter { it.eventDate == sessionDate }

  private fun getOverridingAppointments(
    visitSession: AvailableVisitSessionDto,
    higherPriorityEvents: List<ScheduledEventDto>,
  ): List<ScheduledEventDto> = higherPriorityEvents.filter { hasOverridingAppointment(visitSession, it) }

  private fun hasOverridingAppointment(
    visitSession: AvailableVisitSessionDto,
    scheduledEvent: ScheduledEventDto,
  ): Boolean {
    val visitDate = visitSession.sessionDate
    val visitStartTime = visitSession.sessionTimeSlot.startTime
    val visitEndTime = visitSession.sessionTimeSlot.endTime

    return (
      (scheduledEvent.eventDate != null && visitDate == scheduledEvent.eventDate) &&
        hasOverlappingTimes(
          scheduledEvent.startTime?.toLocalTime() ?: LocalTime.MIN,
          scheduledEvent.endTime?.toLocalTime() ?: LocalTime.MAX,
          visitStartTime,
          visitEndTime,
        )
      )
  }

  private fun getOverridingEventDetails(events: List<ScheduledEventDto>): String = events.joinToString(" and ")

  private fun hasOverlappingTimes(
    scheduledEventStartTime: LocalTime,
    scheduledEventEndTime: LocalTime,
    visitSessionStartTime: LocalTime,
    visitSessionEndTime: LocalTime,
  ): Boolean = (
    visitSessionStartTime.isBefore(scheduledEventEndTime) &&
      visitSessionEndTime.isAfter(scheduledEventStartTime)
    )

  private fun logClashingAppointment(prisonerId: String, availableSession: AvailableVisitSessionDto, overridingAppointments: List<ScheduledEventDto>) {
    LOG.info(
      CLASHING_APPOINTMENT_LOG_MSG,
      prisonerId,
      availableSession.sessionDate,
      availableSession.sessionTimeSlot.startTime,
      availableSession.sessionTimeSlot.endTime,
      overridingAppointments.size,
      getOverridingEventDetails(overridingAppointments),
    )
  }

  private fun getExcludeDatesForSessionTemplate(sessionTemplateReference: String): List<ExcludeDateDto> = visitSchedulerClient.getSessionTemplateExcludeDates(sessionTemplateReference) ?: emptyList()

  private fun setSessionForReview(
    prisonerId: String,
    visitors: List<Long>?,
    dateRange: DateRange,
    availableSessions: List<AvailableVisitSessionDto>,
  ) {
    val restrictionCodesForReview = RestrictionsForReview.entries.map { it.name }
    val prisonerRestrictionDateRanges =
      getPrisonerRestrictionDateRanges(prisonerId, restrictionCodesForReview).let { restrictionDateRanges ->
        dateUtils.getUniqueDateRanges(restrictionDateRanges = restrictionDateRanges ?: emptyList(), dateRange)
          .sortedBy { it.fromDate }
      }

    availableSessions.filter { dateUtils.isDateBetweenDateRanges(prisonerRestrictionDateRanges, it.sessionDate) }.forEach {
      it.isSessionForReview = true
    }

    if (visitors != null && availableSessions.any { !it.isSessionForReview }) {
      val visitorRestrictionDateRanges = getVisitorsRestrictionDateRanges(prisonerId, visitors, restrictionCodesForReview, dateRange)

      // only for any sessions that are not for review - if the session date falls within visitor restriction date range set the session for review
      availableSessions.filter { !it.isSessionForReview.and(dateUtils.isDateBetweenDateRanges(visitorRestrictionDateRanges, it.sessionDate)) }.forEach {
        it.isSessionForReview = true
      }
    }
  }

  private fun getPrisonerRestrictionDateRanges(
    prisonerId: String,
    requestRestrictions: List<String>,
  ): List<IndefiniteDateRange>? = prisonApiClient.getPrisonerRestrictions(prisonerId).offenderRestrictions?.let { offenderRestrictions ->
    offenderRestrictions.filter { it.restrictionType in requestRestrictions }.map { offenderRestriction ->
      IndefiniteDateRange(
        fromDate = offenderRestriction.startDate,
        toDate = offenderRestriction.expiryDate,
      )
    }
  }

  private fun getVisitorsRestrictionDateRanges(
    prisonerId: String,
    visitors: List<Long>,
    restrictionCodesForReview: List<String>,
    dateRange: DateRange,
  ): List<DateRange> {
    val dateRanges = prisonerContactRegistryClient.getVisitorRestrictionDateRanges(prisonerId, visitors, restrictionCodesForReview, dateRange)
    return dateRanges ?: emptyList()
  }
}
