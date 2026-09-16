package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.AlertsApiClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonerContactRegistryClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitSchedulerClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.RestPage
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.OffenderRestrictionsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.AvailableVisitSessionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.DateRange
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionTimeSlotDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSessionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionRestriction
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionTemplateVisitOrderRestrictionType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.sessions.VisitSessionV2Dto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.utils.DateUtils
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@ExtendWith(MockitoExtension::class)
class VisitSchedulerSessionServiceTest {
  private val today = LocalDate.of(2026, 7, 16)

  private val visitSchedulerClient: VisitSchedulerClient = mock<VisitSchedulerClient>()
  private val appointmentsService: AppointmentsService = mock<AppointmentsService>()
  private val prisonerProfileService: PrisonerProfileService = mock<PrisonerProfileService>()
  private val prisonService: PrisonService = mock<PrisonService>()
  private val excludeDatesService: ExcludeDatesService = mock<ExcludeDatesService>()
  private val govUkHolidayService: GovUkHolidayService = mock<GovUkHolidayService>()
  private val dateUtils: DateUtils = mock<DateUtils>()
  private val prisonApiClient: PrisonApiClient = mock<PrisonApiClient>()
  private val prisonerContactRegistryClient: PrisonerContactRegistryClient = mock<PrisonerContactRegistryClient>()
  private val alertsApiClient: AlertsApiClient = mock<AlertsApiClient>()
  private val publicServiceFromDateOverride = 0L
  private val publicServiceToDateOverride = 2L

  private val visitSchedulerSessionsService = VisitSchedulerSessionsService(
    visitSchedulerClient = visitSchedulerClient,
    appointmentsService = appointmentsService,
    prisonerProfileService = prisonerProfileService,
    prisonService = prisonService,
    excludeDatesService = excludeDatesService,
    govUkHolidayService = govUkHolidayService,
    dateUtils = dateUtils,
    prisonApiClient = prisonApiClient,
    prisonerContactRegistryClient = prisonerContactRegistryClient,
    alertsApiClient = alertsApiClient,
    publicServiceFromDateOverride = publicServiceFromDateOverride,
    publicServiceToDateOverride = publicServiceToDateOverride,
  )

  @Test
  fun `when get available visit sessions are called then any available sessions that started before current date and time are not returned`() {
    // When
    // the current time is 10:30
    val now = today.atTime(10, 30)

    // first session starts at 10:00
    val session1 = getAvailableVisitSessionDto(sessionDate = today, sessionTemplateReference = "ref-1", startTime = LocalTime.of(10, 0), endTime = LocalTime.of(11, 0))

    // all other sessions are after current time
    val session2 = getAvailableVisitSessionDto(sessionDate = today, sessionTemplateReference = "ref-2", startTime = LocalTime.of(12, 0), endTime = LocalTime.of(13, 0))
    val session3 = getAvailableVisitSessionDto(sessionDate = today.plusDays(1), sessionTemplateReference = "ref-3", startTime = LocalTime.of(10, 0), endTime = LocalTime.of(11, 0))
    val session4 = getAvailableVisitSessionDto(sessionDate = today.plusDays(1), sessionTemplateReference = "ref-4", startTime = LocalTime.of(12, 0), endTime = LocalTime.of(13, 0))
    val availableSessionsReturned = listOf(session1, session2, session3, session4)
    val dateRange = DateRange(today, today.plusDays(1))

    whenever(prisonService.getToDaysBookableDateRange(any(), anyOrNull(), anyOrNull(), any())).thenReturn(dateRange)
    whenever(dateUtils.advanceFromDate(any(), anyOrNull())).thenReturn(dateRange)
    whenever(dateUtils.now()).thenReturn(now)
    whenever(visitSchedulerClient.getAvailableVisitSessions(any(), any(), anyOrNull(), any(), anyOrNull(), anyOrNull(), any())).thenReturn(availableSessionsReturned)

    // call available sessions
    val availableSessions = visitSchedulerSessionsService.getAvailableVisitSessions(
      prisonCode = "MDI",
      userType = UserType.PUBLIC,
      prisonerId = "A",
      requestedSessionRestriction = null,
      visitors = listOf(1),
      withAppointmentsCheck = false,
      excludedApplicationReference = null,
      pvbAdvanceFromDateByDays = 0,
      fromDateOverride = null,
      toDateOverride = null,
      username = null,
    )

    // all sessions starting after current time are returned
    assertThat(availableSessions).hasSize(3)
    assertThat(availableSessions[0]).isEqualTo(session2)
    assertThat(availableSessions[1]).isEqualTo(session3)
    assertThat(availableSessions[2]).isEqualTo(session4)
  }

  @Test
  fun `when get available visit sessions are called then any available sessions that started at the same time as current time are returned`() {
    // When
    // the current time is 10:00
    val now = today.atTime(10, 0)

    // first session starts at 10:00 - hence should be returned
    val session1 = getAvailableVisitSessionDto(sessionDate = today, sessionTemplateReference = "ref-1", startTime = LocalTime.of(10, 0), endTime = LocalTime.of(11, 0))

    // all other sessions are after current time
    val session2 = getAvailableVisitSessionDto(sessionDate = today, sessionTemplateReference = "ref-2", startTime = LocalTime.of(12, 0), endTime = LocalTime.of(13, 0))
    val session3 = getAvailableVisitSessionDto(sessionDate = today.plusDays(1), sessionTemplateReference = "ref-3", startTime = LocalTime.of(10, 0), endTime = LocalTime.of(11, 0))
    val session4 = getAvailableVisitSessionDto(sessionDate = today.plusDays(1), sessionTemplateReference = "ref-4", startTime = LocalTime.of(12, 0), endTime = LocalTime.of(13, 0))
    val availableSessionsReturned = listOf(session1, session2, session3, session4)
    val dateRange = DateRange(today, today.plusDays(1))

    whenever(prisonService.getToDaysBookableDateRange(any(), anyOrNull(), anyOrNull(), any())).thenReturn(dateRange)
    whenever(dateUtils.advanceFromDate(any(), anyOrNull())).thenReturn(dateRange)
    whenever(dateUtils.now()).thenReturn(now)
    whenever(visitSchedulerClient.getAvailableVisitSessions(any(), any(), anyOrNull(), any(), anyOrNull(), anyOrNull(), any())).thenReturn(availableSessionsReturned)

    // call available sessions
    val availableSessions = visitSchedulerSessionsService.getAvailableVisitSessions(
      prisonCode = "MDI",
      userType = UserType.PUBLIC,
      prisonerId = "A",
      requestedSessionRestriction = null,
      visitors = listOf(1),
      withAppointmentsCheck = false,
      excludedApplicationReference = null,
      pvbAdvanceFromDateByDays = 0,
      fromDateOverride = null,
      toDateOverride = null,
      username = null,
    )

    // all sessions starting after current time are returned
    assertThat(availableSessions).hasSize(4)
    assertThat(availableSessions[0]).isEqualTo(session1)
    assertThat(availableSessions[1]).isEqualTo(session2)
    assertThat(availableSessions[2]).isEqualTo(session3)
    assertThat(availableSessions[3]).isEqualTo(session4)
  }

  @Test
  fun `when get available visit sessions are called then any available sessions that started after the same time as current time are returned`() {
    // When
    // the current time is 09:59
    val now = today.atTime(9, 59)

    // first session starts at 10:00 - hence should be returned
    val session1 = getAvailableVisitSessionDto(sessionDate = today, sessionTemplateReference = "ref-1", startTime = LocalTime.of(10, 0), endTime = LocalTime.of(11, 0))
    // all other sessions are well after current time
    val session2 = getAvailableVisitSessionDto(sessionDate = today, sessionTemplateReference = "ref-2", startTime = LocalTime.of(12, 0), endTime = LocalTime.of(13, 0))
    val session3 = getAvailableVisitSessionDto(sessionDate = today.plusDays(1), sessionTemplateReference = "ref-3", startTime = LocalTime.of(10, 0), endTime = LocalTime.of(11, 0))
    val session4 = getAvailableVisitSessionDto(sessionDate = today.plusDays(1), sessionTemplateReference = "ref-4", startTime = LocalTime.of(12, 0), endTime = LocalTime.of(13, 0))
    val availableSessionsReturned = listOf(session1, session2, session3, session4)
    val dateRange = DateRange(today, today.plusDays(1))

    whenever(prisonService.getToDaysBookableDateRange(any(), anyOrNull(), anyOrNull(), any())).thenReturn(dateRange)
    whenever(dateUtils.advanceFromDate(any(), anyOrNull())).thenReturn(dateRange)
    whenever(dateUtils.now()).thenReturn(now)
    whenever(visitSchedulerClient.getAvailableVisitSessions(any(), any(), anyOrNull(), any(), anyOrNull(), anyOrNull(), any())).thenReturn(availableSessionsReturned)

    // call available sessions
    val availableSessions = visitSchedulerSessionsService.getAvailableVisitSessions(
      prisonCode = "MDI",
      userType = UserType.PUBLIC,
      prisonerId = "A",
      requestedSessionRestriction = null,
      visitors = listOf(1),
      withAppointmentsCheck = false,
      excludedApplicationReference = null,
      pvbAdvanceFromDateByDays = 0,
      fromDateOverride = null,
      toDateOverride = null,
      username = null,
    )

    // all sessions starting after current time are returned
    assertThat(availableSessions).hasSize(4)
    assertThat(availableSessions[0]).isEqualTo(session1)
    assertThat(availableSessions[1]).isEqualTo(session2)
    assertThat(availableSessions[2]).isEqualTo(session3)
    assertThat(availableSessions[3]).isEqualTo(session4)
  }

  @Test
  fun `when get available visit sessions for public user are called then any available sessions that started before current date and time are not returned`() {
    // When
    // the current time is 10:30
    val now = today.atTime(10, 30)

    // first session starts at 10:00
    val session1 = getAvailableVisitSessionDto(sessionDate = today, sessionTemplateReference = "ref-1", startTime = LocalTime.of(10, 0), endTime = LocalTime.of(11, 0))

    // all other sessions are after current time
    val session2 = getAvailableVisitSessionDto(sessionDate = today, sessionTemplateReference = "ref-2", startTime = LocalTime.of(12, 0), endTime = LocalTime.of(13, 0))
    val session3 = getAvailableVisitSessionDto(sessionDate = today.plusDays(1), sessionTemplateReference = "ref-3", startTime = LocalTime.of(10, 0), endTime = LocalTime.of(11, 0))
    val session4 = getAvailableVisitSessionDto(sessionDate = today.plusDays(1), sessionTemplateReference = "ref-4", startTime = LocalTime.of(12, 0), endTime = LocalTime.of(13, 0))
    val availableSessionsReturned = listOf(session1, session2, session3, session4)
    val dateRange = DateRange(today, today.plusDays(1))

    whenever(prisonService.getToDaysBookableDateRange(any(), anyOrNull(), anyOrNull(), any())).thenReturn(dateRange)
    whenever(dateUtils.advanceFromDate(any(), anyOrNull())).thenReturn(dateRange)
    whenever(dateUtils.now()).thenReturn(now)
    whenever(visitSchedulerClient.getAvailableVisitSessions(any(), any(), anyOrNull(), any(), anyOrNull(), anyOrNull(), any())).thenReturn(availableSessionsReturned)
    whenever(alertsApiClient.getPrisonerAlerts(any())).thenReturn(RestPage.empty())
    whenever(prisonApiClient.getPrisonerRestrictions(any())).thenReturn(OffenderRestrictionsDto(null, null))

    // call available sessions
    val availableSessions = visitSchedulerSessionsService.getAvailableVisitSessionsForPublicUser(
      prisonCode = "MDI",
      userType = UserType.PUBLIC,
      prisonerId = "A",
      visitors = listOf(1),
      excludedApplicationReference = null,
      username = null,
    )

    // all sessions starting after current time are returned
    assertThat(availableSessions).hasSize(3)
    assertThat(availableSessions[0]).isEqualTo(session2)
    assertThat(availableSessions[1]).isEqualTo(session3)
    assertThat(availableSessions[2]).isEqualTo(session4)
  }

  @Test
  fun `when get available visit sessions for public user are called then any available sessions that started at the same time as current time are returned`() {
    // When
    // the current time is 10:00
    val now = today.atTime(10, 0)

    // first session starts at 10:00 - hence should be returned
    val session1 = getAvailableVisitSessionDto(sessionDate = today, sessionTemplateReference = "ref-1", startTime = LocalTime.of(10, 0), endTime = LocalTime.of(11, 0))

    // all other sessions are after current time
    val session2 = getAvailableVisitSessionDto(sessionDate = today, sessionTemplateReference = "ref-2", startTime = LocalTime.of(12, 0), endTime = LocalTime.of(13, 0))
    val session3 = getAvailableVisitSessionDto(sessionDate = today.plusDays(1), sessionTemplateReference = "ref-3", startTime = LocalTime.of(10, 0), endTime = LocalTime.of(11, 0))
    val session4 = getAvailableVisitSessionDto(sessionDate = today.plusDays(1), sessionTemplateReference = "ref-4", startTime = LocalTime.of(12, 0), endTime = LocalTime.of(13, 0))
    val availableSessionsReturned = listOf(session1, session2, session3, session4)
    val dateRange = DateRange(today, today.plusDays(1))

    whenever(prisonService.getToDaysBookableDateRange(any(), anyOrNull(), anyOrNull(), any())).thenReturn(dateRange)
    whenever(dateUtils.advanceFromDate(any(), anyOrNull())).thenReturn(dateRange)
    whenever(dateUtils.now()).thenReturn(now)
    whenever(visitSchedulerClient.getAvailableVisitSessions(any(), any(), anyOrNull(), any(), anyOrNull(), anyOrNull(), any())).thenReturn(availableSessionsReturned)
    whenever(alertsApiClient.getPrisonerAlerts(any())).thenReturn(RestPage.empty())
    whenever(prisonApiClient.getPrisonerRestrictions(any())).thenReturn(OffenderRestrictionsDto(null, null))

    // call available sessions
    val availableSessions = visitSchedulerSessionsService.getAvailableVisitSessionsForPublicUser(
      prisonCode = "MDI",
      userType = UserType.PUBLIC,
      prisonerId = "A",
      visitors = listOf(1),
      excludedApplicationReference = null,
      username = null,
    )

    // all sessions starting after current time are returned
    assertThat(availableSessions).hasSize(4)
    assertThat(availableSessions[0]).isEqualTo(session1)
    assertThat(availableSessions[1]).isEqualTo(session2)
    assertThat(availableSessions[2]).isEqualTo(session3)
    assertThat(availableSessions[3]).isEqualTo(session4)
  }

  @Test
  fun `when get available visit sessions for public user are called then any available sessions that started after the same time as current time are returned`() {
    // When
    // the current time is 09:59
    val now = today.atTime(9, 59)

    // first session starts at 10:00 - hence should be returned
    val session1 = getAvailableVisitSessionDto(sessionDate = today, sessionTemplateReference = "ref-1", startTime = LocalTime.of(10, 0), endTime = LocalTime.of(11, 0))
    // all other sessions are well after current time
    val session2 = getAvailableVisitSessionDto(sessionDate = today, sessionTemplateReference = "ref-2", startTime = LocalTime.of(12, 0), endTime = LocalTime.of(13, 0))
    val session3 = getAvailableVisitSessionDto(sessionDate = today.plusDays(1), sessionTemplateReference = "ref-3", startTime = LocalTime.of(10, 0), endTime = LocalTime.of(11, 0))
    val session4 = getAvailableVisitSessionDto(sessionDate = today.plusDays(1), sessionTemplateReference = "ref-4", startTime = LocalTime.of(12, 0), endTime = LocalTime.of(13, 0))
    val availableSessionsReturned = listOf(session1, session2, session3, session4)
    val dateRange = DateRange(today, today.plusDays(1))

    whenever(prisonService.getToDaysBookableDateRange(any(), anyOrNull(), anyOrNull(), any())).thenReturn(dateRange)
    whenever(dateUtils.advanceFromDate(any(), anyOrNull())).thenReturn(dateRange)
    whenever(dateUtils.now()).thenReturn(now)
    whenever(visitSchedulerClient.getAvailableVisitSessions(any(), any(), anyOrNull(), any(), anyOrNull(), anyOrNull(), any())).thenReturn(availableSessionsReturned)
    whenever(alertsApiClient.getPrisonerAlerts(any())).thenReturn(RestPage.empty())
    whenever(prisonApiClient.getPrisonerRestrictions(any())).thenReturn(OffenderRestrictionsDto(null, null))

    // call available sessions
    val availableSessions = visitSchedulerSessionsService.getAvailableVisitSessionsForPublicUser(
      prisonCode = "MDI",
      userType = UserType.PUBLIC,
      prisonerId = "A",
      visitors = listOf(1),
      excludedApplicationReference = null,
      username = null,
    )

    // all sessions starting after current time are returned
    assertThat(availableSessions).hasSize(4)
    assertThat(availableSessions[0]).isEqualTo(session1)
    assertThat(availableSessions[1]).isEqualTo(session2)
    assertThat(availableSessions[2]).isEqualTo(session3)
    assertThat(availableSessions[3]).isEqualTo(session4)
  }

  @Test
  fun `when get visit sessions and schedule are called then any available sessions that started before current date and time are not returned`() {
    // When
    // the current time is 10:30
    val now = today.atTime(10, 30)

    // first session starts at 10:00
    val session1 = getVisitSessionDto(sessionTemplateReference = "ref-1", prisonCode = "MDI", startTime = today.atTime(10, 0))

    // all other sessions are after current time
    val session2 = getVisitSessionDto(sessionTemplateReference = "ref-2", prisonCode = "MDI", startTime = today.atTime(11, 0))
    val session3 = getVisitSessionDto(sessionTemplateReference = "ref-3", prisonCode = "MDI", startTime = today.plusDays(1).atTime(10, 0))
    val session4 = getVisitSessionDto(sessionTemplateReference = "ref-4", prisonCode = "MDI", startTime = today.plusDays(1).atTime(11, 0))
    val availableSessionsReturned = listOf(session1, session2, session3, session4)
    val dateRange = DateRange(today, today.plusDays(1))

    whenever(prisonService.getToDaysBookableDateRange(any(), anyOrNull(), anyOrNull(), any())).thenReturn(dateRange)
    whenever(dateUtils.now()).thenReturn(now)
    whenever(dateUtils.today()).thenReturn(today)
    whenever(visitSchedulerClient.getVisitSessions(any(), any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(availableSessionsReturned)

    // call available sessions
    val visitSessionsAndSchedule = visitSchedulerSessionsService.getVisitSessionsAndSchedule(
      prisonCode = "MDI",
      prisonerId = "A",
      min = null,
      username = null,
    )

    // all sessions starting after current time are returned
    val sessions = visitSessionsAndSchedule.sessionsAndSchedule.flatMap { it.visitSessions }
    assertThat(sessions).hasSize(3)
    assertThat(sessions[0]).isEqualTo(VisitSessionV2Dto(session2, emptyList()))
    assertThat(sessions[1]).isEqualTo(VisitSessionV2Dto(session3, emptyList()))
    assertThat(sessions[2]).isEqualTo(VisitSessionV2Dto(session4, emptyList()))
  }

  @Test
  fun `when get visit sessions and schedule are called then any available sessions that started at the same time as current time are returned`() {
    // When
    // the current time is 10:00
    val now = today.atTime(10, 0)

    // first session starts at 10:00 - hence should be returned
    val session1 = getVisitSessionDto(sessionTemplateReference = "ref-1", prisonCode = "MDI", startTime = today.atTime(10, 0))

    // all other sessions are after current time
    val session2 = getVisitSessionDto(sessionTemplateReference = "ref-2", prisonCode = "MDI", startTime = today.atTime(11, 0))
    val session3 = getVisitSessionDto(sessionTemplateReference = "ref-3", prisonCode = "MDI", startTime = today.plusDays(1).atTime(10, 0))
    val session4 = getVisitSessionDto(sessionTemplateReference = "ref-4", prisonCode = "MDI", startTime = today.plusDays(1).atTime(11, 0))
    val availableSessionsReturned = listOf(session1, session2, session3, session4)
    val dateRange = DateRange(today, today.plusDays(1))

    whenever(prisonService.getToDaysBookableDateRange(any(), anyOrNull(), anyOrNull(), any())).thenReturn(dateRange)
    whenever(dateUtils.now()).thenReturn(now)
    whenever(dateUtils.today()).thenReturn(today)
    whenever(visitSchedulerClient.getVisitSessions(any(), any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(availableSessionsReturned)

    // call available sessions
    val visitSessionsAndSchedule = visitSchedulerSessionsService.getVisitSessionsAndSchedule(
      prisonCode = "MDI",
      prisonerId = "A",
      min = null,
      username = null,
    )

    // all sessions starting after current time are returned
    val sessions = visitSessionsAndSchedule.sessionsAndSchedule.flatMap { it.visitSessions }
    assertThat(sessions).hasSize(4)
    assertThat(sessions[0]).isEqualTo(VisitSessionV2Dto(session1, emptyList()))
    assertThat(sessions[1]).isEqualTo(VisitSessionV2Dto(session2, emptyList()))
    assertThat(sessions[2]).isEqualTo(VisitSessionV2Dto(session3, emptyList()))
    assertThat(sessions[3]).isEqualTo(VisitSessionV2Dto(session4, emptyList()))
  }

  @Test
  fun `when get visit sessions and schedule are called then any available sessions that started after the same time as current time are returned`() {
    // When
    // the current time is 09:59
    val now = today.atTime(9, 59)

    // first session starts at 10:00 - hence should be returned
    val session1 = getVisitSessionDto(sessionTemplateReference = "ref-1", prisonCode = "MDI", startTime = today.atTime(10, 0))

    // all other sessions are after current time
    val session2 = getVisitSessionDto(sessionTemplateReference = "ref-2", prisonCode = "MDI", startTime = today.atTime(11, 0))
    val session3 = getVisitSessionDto(sessionTemplateReference = "ref-3", prisonCode = "MDI", startTime = today.plusDays(1).atTime(10, 0))
    val session4 = getVisitSessionDto(sessionTemplateReference = "ref-4", prisonCode = "MDI", startTime = today.plusDays(1).atTime(11, 0))
    val availableSessionsReturned = listOf(session1, session2, session3, session4)
    val dateRange = DateRange(today, today.plusDays(1))

    whenever(prisonService.getToDaysBookableDateRange(any(), anyOrNull(), anyOrNull(), any())).thenReturn(dateRange)
    whenever(dateUtils.now()).thenReturn(now)
    whenever(dateUtils.today()).thenReturn(today)
    whenever(visitSchedulerClient.getVisitSessions(any(), any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(availableSessionsReturned)

    // call available sessions
    val visitSessionsAndSchedule = visitSchedulerSessionsService.getVisitSessionsAndSchedule(
      prisonCode = "MDI",
      prisonerId = "A",
      min = null,
      username = null,
    )

    // all sessions starting after current time are returned
    val sessions = visitSessionsAndSchedule.sessionsAndSchedule.flatMap { it.visitSessions }
    assertThat(sessions).hasSize(4)
    assertThat(sessions[0]).isEqualTo(VisitSessionV2Dto(session1, emptyList()))
    assertThat(sessions[1]).isEqualTo(VisitSessionV2Dto(session2, emptyList()))
    assertThat(sessions[2]).isEqualTo(VisitSessionV2Dto(session3, emptyList()))
    assertThat(sessions[3]).isEqualTo(VisitSessionV2Dto(session4, emptyList()))
  }

  private fun getAvailableVisitSessionDto(
    sessionDate: LocalDate,
    sessionTemplateReference: String,
    startTime: LocalTime,
    endTime: LocalTime,
  ): AvailableVisitSessionDto = AvailableVisitSessionDto(
    sessionDate = sessionDate,
    sessionTemplateReference = sessionTemplateReference,
    sessionTimeSlot = SessionTimeSlotDto(startTime, endTime),
    sessionRestriction = SessionRestriction.OPEN,
    sessionForReview = false,
    visitOrderRestriction = SessionTemplateVisitOrderRestrictionType.NONE,
  )

  private fun getVisitSessionDto(
    sessionTemplateReference: String,
    prisonCode: String,
    startTime: LocalDateTime,
  ): VisitSessionDto = VisitSessionDto(
    sessionTemplateReference = sessionTemplateReference,
    visitRoom = "Room-1",
    visitType = VisitType.SOCIAL,
    visitOrderRestriction = SessionTemplateVisitOrderRestrictionType.NONE,
    prisonCode = prisonCode,
    openVisitCapacity = 25,
    openVisitBookedCount = 0,
    closedVisitCapacity = 5,
    closedVisitBookedCount = 0,
    startTimestamp = startTime,
    endTimestamp = startTime.plusMinutes(30),
    sessionConflicts = emptyList(),
  )
}
