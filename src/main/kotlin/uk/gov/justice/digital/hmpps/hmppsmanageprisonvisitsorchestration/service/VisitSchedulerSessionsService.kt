package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitSchedulerClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.AvailableVisitSessionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.AvailableVisitSessionRestrictionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.DateRange
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

// TODO: Request a visit - Sessions flagged as request sessions ticket - Orchestration changes:
//  1. New endpoint, takes similar arguments as the getAvailableSessions endpoint but excludes PVB arguments (public session only endpoint)
//  2. Uses same flow as the VisitSchedulerSessionsService.kt -> getAvailableVisitSessions() method with a few changes, after
//     getting adjusted DateRange from the getBannedRestrictionDateRage() and getting sessions via visit-scheduler call, do the following:
//  3.
//    3a. Make a call to get all supported prisoner restrictions and capture their date range in a DateRange DTO.
//        If any of the supported prisoner restrictions contains a NULL expiry date, skip doing visitor check and set all sessions isRequest flag to TRUE.
//    3b. If all found supported restrictions contain an expiry, capture their to / from date in a List<DateRangeDto>.
//    3c. Call the prisoner-contact-registry new endpoint (Aaron to make this), which will also return a List<DateRangeDto>
//    3d.  Loop through all sessions returned from visit-scheduler, and check if their sessionDate is in any of the date ranges in our list.
//         IF Yes -> Set that session's isRequest flag to TRUE.
//         IF No -> Set that session's isRequest flag to FALSE.
//  4. Return the list to frontend.
//  -
//  Additional notes:
//  - Keep the try catch logic, as it's needed in case BAN date range can't be found, we return an emptyList() (current functionality).

@Service
class VisitSchedulerSessionsService(
  private val visitSchedulerClient: VisitSchedulerClient,
  private val appointmentsService: AppointmentsService,
  private val prisonerProfileService: PrisonerProfileService,
  private val prisonService: PrisonService,
  private val excludeDatesService: ExcludeDatesService,
  private val dateUtils: DateUtils,
) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
    const val CLASHING_APPOINTMENT_LOG_MSG =
      "Visit session for prisonerId - {}, date - {}, start time - {}, end time - {} is unavailable as it clashes with {} medical / legal appointment(s), appointment details - {}"
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
    fromDateOverride: Int? = null,
    toDateOverride: Int? = null,
    username: String? = null,
    userType: UserType,
  ): List<AvailableVisitSessionDto> {
    val sessionRestriction = updateRequestedRestriction(requestedSessionRestriction, prisonerId, visitors)

    // advance from date by n days
    var dateRange = prisonService.getToDaysBookableDateRange(prisonCode = prisonCode, fromDateOverride = fromDateOverride, toDateOverride = toDateOverride)

    var availableVisitSessions = try {
      val updatedDateRange =
        visitors?.let { prisonerProfileService.getBannedRestrictionDateRage(prisonerId, visitors, dateRange) } ?: dateRange
      visitSchedulerClient.getAvailableVisitSessions(prisonCode, prisonerId, sessionRestriction, updatedDateRange, excludedApplicationReference, username, userType)

      // Visitor 1 = Restriction (02/07 - 05/07) / Visitor 2 = Restriction (07/07 - 21/07)
      // Call to prisoner-contact-registry returns a List<DateRange>

      // Call to get prisoner restriction List<DateRange> and combine with the visitor List<DateRange>

      // Do Comparison:
      // For all availableVisitSessions, loop through, and if the returned prisoner-contact-registry contains that date within
      // the list of date ranges, we set a new flag on the AvailableVisitSessionDto isRequestSession to true
      // Finally return updated sessions list.
    } catch (e: DateRangeNotFoundException) {
      LOG.error("getAvailableVisitSessions range is not returned therefore we do not have a valid date range and should return an empty list")
      emptyList()
    }

    if (withAppointmentsCheck && availableVisitSessions.isNotEmpty()) {
      availableVisitSessions = filterAvailableVisitsByHigherPriorityAppointments(prisonerId, dateRange, availableVisitSessions)
    }

    return availableVisitSessions.sortedWith(getAvailableVisitSessionsSortOrder())
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

  private fun getAvailableVisitSessionsSortOrder(): Comparator<AvailableVisitSessionDto> = compareBy(
    { it.sessionDate },
    { it.sessionTimeSlot.startTime },
    { it.sessionTimeSlot.endTime },
  )

  private fun getExcludeDatesForSessionTemplate(sessionTemplateReference: String): List<ExcludeDateDto> = visitSchedulerClient.getSessionTemplateExcludeDates(sessionTemplateReference) ?: emptyList()
}
