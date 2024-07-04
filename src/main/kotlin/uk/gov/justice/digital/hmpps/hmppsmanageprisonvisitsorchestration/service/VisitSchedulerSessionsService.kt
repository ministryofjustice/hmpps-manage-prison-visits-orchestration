package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitSchedulerClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.AvailableVisitSessionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.DateRange
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionCapacityDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionScheduleDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSessionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionRestriction
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionRestriction.CLOSED
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionRestriction.OPEN
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.whereabouts.ScheduledEventDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.DateRangeNotFoundException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.utils.DateUtils
import java.time.LocalDate
import java.time.LocalTime

@Service
class VisitSchedulerSessionsService(
  private val visitSchedulerClient: VisitSchedulerClient,
  private val appointmentsService: AppointmentsService,
  private val prisonerProfileService: PrisonerProfileService,
  private val prisonService: PrisonService,
  private val dateUtils: DateUtils,
) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
    const val CLASHING_APPOINTMENT_LOG_MSG =
      "Visit session for prisonerId - {}, date - {}, start time - {}, end time - {} is unavailable as it clashes with {} medical / legal appointment(s), appointment details - {}"
  }

  fun getVisitSessions(prisonCode: String, prisonerId: String?, min: Int?, max: Int?): List<VisitSessionDto>? {
    return visitSchedulerClient.getVisitSessions(prisonCode, prisonerId, min, max)
  }

  fun getAvailableVisitSessions(
    prisonCode: String,
    prisonerId: String,
    requestedSessionRestriction: SessionRestriction?,
    visitors: List<Long>?,
    withAppointmentsCheck: Boolean,
    excludedApplicationReference: String? = null,
    advanceFromDateByDays: Int = 0,
  ): List<AvailableVisitSessionDto> {
    val sessionRestriction = updateRequestedRestriction(requestedSessionRestriction, prisonerId, visitors)

    val dateRange = prisonService.getToDaysBookableDateRange(prisonCode).also {
      // advance from date by n days
      dateUtils.advanceFromDate(it, advanceFromDateByDays)
    }

    var availableVisitSessions = try {
      val updatedDateRange =
        visitors?.let { prisonerProfileService.getBannedRestrictionDateRage(prisonerId, visitors, dateRange) } ?: dateRange
      visitSchedulerClient.getAvailableVisitSessions(prisonCode, prisonerId, sessionRestriction, updatedDateRange, excludedApplicationReference)
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
  ): SessionCapacityDto? {
    return visitSchedulerClient.getSessionCapacity(prisonCode, sessionDate, sessionStartTime, sessionEndTime)
  }

  fun getSessionSchedule(prisonCode: String, sessionDate: LocalDate): List<SessionScheduleDto>? {
    return visitSchedulerClient.getSessionSchedule(prisonCode, sessionDate)
  }

  private fun updateRequestedRestriction(
    requestedSessionRestriction: SessionRestriction?,
    prisonerId: String,
    visitors: List<Long>?,
  ): SessionRestriction {
    return if (prisonerProfileService.hasPrisonerGotClosedRestrictions(prisonerId) ||
      (if (visitors != null) prisonerProfileService.hasVisitorsGotClosedRestrictions(prisonerId, visitors) else false)
    ) {
      CLOSED
    } else {
      requestedSessionRestriction ?: OPEN
    }
  }

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
  ): List<ScheduledEventDto> {
    return higherPriorityAppointments.filter { it.eventDate == sessionDate }
  }

  private fun getOverridingAppointments(
    visitSession: AvailableVisitSessionDto,
    higherPriorityEvents: List<ScheduledEventDto>,
  ): List<ScheduledEventDto> {
    return higherPriorityEvents.filter { hasOverridingAppointment(visitSession, it) }
  }

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

  private fun getOverridingEventDetails(events: List<ScheduledEventDto>): String {
    return events.joinToString(" and ")
  }

  private fun hasOverlappingTimes(
    scheduledEventStartTime: LocalTime,
    scheduledEventEndTime: LocalTime,
    visitSessionStartTime: LocalTime,
    visitSessionEndTime: LocalTime,
  ): Boolean {
    return (
      visitSessionStartTime.isBefore(scheduledEventEndTime) &&
        visitSessionEndTime.isAfter(scheduledEventStartTime)
      )
  }

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

  private fun getAvailableVisitSessionsSortOrder(): Comparator<AvailableVisitSessionDto> {
    return compareBy(
      { it.sessionDate },
      { it.sessionTimeSlot.startTime },
      { it.sessionTimeSlot.endTime },
    )
  }
}
