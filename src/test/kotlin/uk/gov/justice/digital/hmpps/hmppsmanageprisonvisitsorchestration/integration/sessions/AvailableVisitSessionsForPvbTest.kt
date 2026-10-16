package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.sessions

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config.CacheNames
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.govuk.holidays.HolidayEventByDivisionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.govuk.holidays.HolidaysDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.OffenderRestrictionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.OffenderRestrictionsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.AvailableVisitSessionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.DateRange
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionTimeSlotDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.PrisonClientType.PUBLIC
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionRestriction.CLOSED
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionRestriction.OPEN
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionTemplateVisitOrderRestrictionType.NONE
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionTemplateVisitOrderRestrictionType.PVO
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionTemplateVisitOrderRestrictionType.VO
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionTemplateVisitOrderRestrictionType.VO_PVO
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.whereabouts.enums.HigherPriorityMedicalOrLegalEvents.ADJUDICATION_HEARING
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.whereabouts.enums.HigherPriorityMedicalOrLegalEvents.MEDICAL_OPTICIAN
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.TestObjectMapper
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@DisplayName("Get available visit sessions for PVB")
class AvailableVisitSessionsForPvbTest : IntegrationTestBase() {
  @Autowired
  lateinit var cacheManager: CacheManager

  @BeforeEach
  fun setupBankHolidays() {
    cacheManager.getCache(CacheNames.BANK_HOLIDAYS)?.clear()
    govUkMockServer.resetAll()
    govUkMockServer.stubGetBankHolidays(HolidaysDto(HolidayEventByDivisionDto("england-and-wales", emptyList())))
  }

  private val prisonCode = "MDI"
  private val prisonerId = "AA123456B"
  private val visitSession1 = AvailableVisitSessionDto(LocalDate.now().plusDays(3), "session1", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN, visitOrderRestriction = VO)
  private val visitSession2 = AvailableVisitSessionDto(LocalDate.now().plusDays(4), "session2", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN, visitOrderRestriction = PVO)
  private val visitSession3 = AvailableVisitSessionDto(LocalDate.now().plusDays(5), "session3", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN, visitOrderRestriction = VO_PVO)
  private val visitSession4 = AvailableVisitSessionDto(visitSession1.sessionDate, "session1", SessionTimeSlotDto(LocalTime.of(15, 0), LocalTime.of(16, 0)), OPEN, visitOrderRestriction = NONE)

  private val visitSchedulerPrisonDto = createVisitSchedulerPrisonDto(prisonCode, active = true, maxTotalVisitors = 6, maxAdultVisitors = 3, maxChildVisitors = 3, policyNoticeDaysMin = 2, policyNoticeDaysMax = 28, adultAgeYears = 18, weekStartDay = DayOfWeek.MONDAY, remandVisitLimitPerWeek = 3)

  private lateinit var dateRange: DateRange

  private val eventType = "APP"
  private val eventTypeDesc = "Appointment"

  @BeforeEach
  fun setupMocks() {
    dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), clientType = PUBLIC)
    visitSchedulerMockServer.stubGetPrison(prisonCode, visitSchedulerPrisonDto)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, OffenderRestrictionsDto(offenderRestrictions = emptyList()))
  }

  @Test
  fun `prisoner closed restrictions select closed sessions for PVB`() {
    val closedSession = visitSession1.copy(sessionRestriction = CLOSED)
    prisonApiMockServer.stubGetPrisonerRestrictions(
      prisonerId,
      OffenderRestrictionsDto(
        offenderRestrictions = listOf(
          OffenderRestrictionDto(restrictionId = 1, restrictionType = "CLOSED", restrictionTypeDescription = "Closed visits", startDate = LocalDate.now(), expiryDate = null, active = true),
        ),
      ),
    )
    visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, CLOSED, listOf(closedSession), dateRange = dateRange, clientType = PUBLIC)
    prisonApiMockServer.stubGetScheduledEvents(prisonerId, dateRange.fromDate, dateRange.toDate, emptyList())

    callGetAvailableVisitSessionsForPvb(webTestClient, prisonCode, prisonerId, roleVSIPOrchestrationServiceHttpHeaders)
      .expectStatus().isOk
      .expectBody().jsonPath("$[0].sessionRestriction").isEqualTo("CLOSED")

    verify(visitSchedulerClientSpy).getAvailableVisitSessions(prisonCode, prisonerId, CLOSED, dateRange, null, null, PUBLIC, null)
  }

  @Test
  fun `empty PVB sessions skip appointment and visitor checks`() {
    visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, emptyList(), dateRange = dateRange, clientType = PUBLIC)

    callGetAvailableVisitSessionsForPvb(webTestClient, prisonCode, prisonerId, roleVSIPOrchestrationServiceHttpHeaders)
      .expectStatus().isOk
      .expectBody().jsonPath("$.length()").isEqualTo(0)

    verifyNoInteractions(appointmentsServiceSpy, prisonerContactRegistryClientSpy, alertsApiClientSpy)
  }

  @Test
  fun `when there are no clashing appointments sessions are returned`() {
    // Given
    // appointment is not on the same date as the visits

    prisonApiMockServer.stubGetScheduledEvents(prisonerId, dateRange.fromDate, dateRange.toDate, emptyList())

    // When
    val responseSpec = callGetAvailableVisitSessionsForPvb(webTestClient, prisonCode, prisonerId, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()

    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(3)
    assertThat(availableSessions[0]).isEqualTo(visitSession1)
    assertThat(availableSessions[1]).isEqualTo(visitSession2)
    assertThat(availableSessions[2]).isEqualTo(visitSession3)

    verify(appointmentsServiceSpy, times(1)).getHigherPriorityAppointments(prisonerId, dateRange.fromDate, dateRange.toDate)
  }

  @Test
  fun `when there is a clashing appointment the session is not returned`() {
    // Given
    // appointment is on same date as visit session with start and end time as null
    val eventDate = visitSession1.sessionDate
    val eventStartTime = null
    val eventEndTime = null

    // evening session - same date
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3, visitSession4), clientType = PUBLIC)

    val appointment = createScheduledEvent(1L, eventDate, eventType, eventTypeDesc, ADJUDICATION_HEARING.code, ADJUDICATION_HEARING.desc, eventStartTime, eventEndTime)
    prisonApiMockServer.stubGetScheduledEvents(prisonerId, dateRange.fromDate, dateRange.toDate, listOf(appointment))

    // When
    val responseSpec = callGetAvailableVisitSessionsForPvb(webTestClient, prisonCode, prisonerId, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()

    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(2)
    assertThat(availableSessions[0]).isEqualTo(visitSession2)
    assertThat(availableSessions[1]).isEqualTo(visitSession3)

    verify(appointmentsServiceSpy, times(1)).getHigherPriorityAppointments(prisonerId, dateRange.fromDate, dateRange.toDate)
  }

  @Test
  fun `when there are multiple clashing appointments the session is not returned`() {
    // Given
    // appointment is on same date as visit session with start and end time as null
    val eventDate = visitSession1.sessionDate
    // evening session - same date
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3, visitSession4), clientType = PUBLIC)

    val appointment1 = createScheduledEvent(1L, eventDate, eventType, eventTypeDesc, ADJUDICATION_HEARING.code, ADJUDICATION_HEARING.desc, null, null)
    val appointment2 = createScheduledEvent(2L, eventDate, eventType, eventTypeDesc, MEDICAL_OPTICIAN.code, MEDICAL_OPTICIAN.desc, eventDate.atTime(visitSession1.sessionTimeSlot.startTime), eventDate.atTime(visitSession1.sessionTimeSlot.endTime))
    prisonApiMockServer.stubGetScheduledEvents(prisonerId, dateRange.fromDate, dateRange.toDate, listOf(appointment1, appointment2))

    // When
    val responseSpec = callGetAvailableVisitSessionsForPvb(webTestClient, prisonCode, prisonerId, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()

    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(2)
    assertThat(availableSessions[0]).isEqualTo(visitSession2)
    assertThat(availableSessions[1]).isEqualTo(visitSession3)

    verify(appointmentsServiceSpy, times(1)).getHigherPriorityAppointments(prisonerId, dateRange.fromDate, dateRange.toDate)
  }

  @Test
  fun `when appointment starts and ends between visit - the session is not returned`() {
    // Given
    // appointment is on same date as visit session with start and end time between the visit session time
    // visit time - 09:00 - 10:00
    // appointment time - 09:05 - 09:55

    val eventDate = visitSession1.sessionDate
    val eventStartTime = eventDate.atTime(visitSession1.sessionTimeSlot.startTime.plusMinutes(5))
    val eventEndTime = eventDate.atTime(visitSession1.sessionTimeSlot.endTime.minusMinutes(5))
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3, visitSession4).sortedBy { it.sessionDate }, clientType = PUBLIC)

    val appointment = createScheduledEvent(1L, eventDate, eventType, eventTypeDesc, ADJUDICATION_HEARING.code, ADJUDICATION_HEARING.desc, eventStartTime, eventEndTime)
    prisonApiMockServer.stubGetScheduledEvents(prisonerId, dateRange.fromDate, dateRange.toDate, listOf(appointment))

    // When
    val responseSpec = callGetAvailableVisitSessionsForPvb(webTestClient, prisonCode, prisonerId, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()

    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(3)
    assertThat(availableSessions[0]).isEqualTo(visitSession4)
    assertThat(availableSessions[1]).isEqualTo(visitSession2)
    assertThat(availableSessions[2]).isEqualTo(visitSession3)

    verify(appointmentsServiceSpy, times(1)).getHigherPriorityAppointments(prisonerId, dateRange.fromDate, dateRange.toDate)
  }

  @Test
  fun `when appointment starts after visit start and ends after visit end - the session is not returned`() {
    // Given
    // appointment is on same date as visit session with start and end time between the visit session time
    // visit time - 09:00 - 10:00
    // appointment time - 09:05 - 10:05

    val eventDate = visitSession1.sessionDate
    val eventStartTime = eventDate.atTime(visitSession1.sessionTimeSlot.startTime.plusMinutes(5))
    val eventEndTime = eventDate.atTime(visitSession1.sessionTimeSlot.endTime.plusMinutes(5))
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3, visitSession4).sortedBy { it.sessionDate }, clientType = PUBLIC)

    val appointment = createScheduledEvent(1L, eventDate, eventType, eventTypeDesc, ADJUDICATION_HEARING.code, ADJUDICATION_HEARING.desc, eventStartTime, eventEndTime)
    prisonApiMockServer.stubGetScheduledEvents(prisonerId, dateRange.fromDate, dateRange.toDate, listOf(appointment))

    // When
    val responseSpec = callGetAvailableVisitSessionsForPvb(webTestClient, prisonCode, prisonerId, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()

    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(3)
    assertThat(availableSessions[0]).isEqualTo(visitSession4)
    assertThat(availableSessions[1]).isEqualTo(visitSession2)
    assertThat(availableSessions[2]).isEqualTo(visitSession3)

    verify(appointmentsServiceSpy, times(1)).getHigherPriorityAppointments(prisonerId, dateRange.fromDate, dateRange.toDate)
  }

  @Test
  fun `when appointment starts before visit start and ends before visit end time - the session is not returned`() {
    // Given
    // appointment is on same date as visit session with start and end time between the visit session time
    // visit time - 09:00 - 10:00
    // appointment time - 08:55 - 09:55

    val eventDate = visitSession1.sessionDate
    val eventStartTime = eventDate.atTime(visitSession1.sessionTimeSlot.startTime.minusMinutes(5))
    val eventEndTime = eventDate.atTime(visitSession1.sessionTimeSlot.endTime.minusMinutes(5))
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3, visitSession4).sortedBy { it.sessionDate }, clientType = PUBLIC)

    val appointment = createScheduledEvent(1L, eventDate, eventType, eventTypeDesc, ADJUDICATION_HEARING.code, ADJUDICATION_HEARING.desc, eventStartTime, eventEndTime)
    prisonApiMockServer.stubGetScheduledEvents(prisonerId, dateRange.fromDate, dateRange.toDate, listOf(appointment))

    // When
    val responseSpec = callGetAvailableVisitSessionsForPvb(webTestClient, prisonCode, prisonerId, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()

    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(3)
    assertThat(availableSessions[0]).isEqualTo(visitSession4)
    assertThat(availableSessions[1]).isEqualTo(visitSession2)
    assertThat(availableSessions[2]).isEqualTo(visitSession3)

    verify(appointmentsServiceSpy, times(1)).getHigherPriorityAppointments(prisonerId, dateRange.fromDate, dateRange.toDate)
  }

  @Test
  fun `when appointment starts and ends at same time as visit session - the session is not returned`() {
    // Given
    // appointment is on same date as visit session with start and end time between the visit session time
    // visit time - 09:00 - 10:00
    // appointment time - 09:00 - 10:00

    val eventDate = visitSession1.sessionDate
    val eventStartTime = eventDate.atTime(visitSession1.sessionTimeSlot.startTime)
    val eventEndTime = eventDate.atTime(visitSession1.sessionTimeSlot.endTime)
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3, visitSession4).sortedBy { it.sessionDate }, clientType = PUBLIC)

    val appointment = createScheduledEvent(1L, eventDate, eventType, eventTypeDesc, ADJUDICATION_HEARING.code, ADJUDICATION_HEARING.desc, eventStartTime, eventEndTime)
    prisonApiMockServer.stubGetScheduledEvents(prisonerId, dateRange.fromDate, dateRange.toDate, listOf(appointment))

    // When
    val responseSpec = callGetAvailableVisitSessionsForPvb(webTestClient, prisonCode, prisonerId, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()

    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(3)
    assertThat(availableSessions[0]).isEqualTo(visitSession4)
    assertThat(availableSessions[1]).isEqualTo(visitSession2)
    assertThat(availableSessions[2]).isEqualTo(visitSession3)

    verify(appointmentsServiceSpy, times(1)).getHigherPriorityAppointments(prisonerId, dateRange.fromDate, dateRange.toDate)
  }

  @Test
  fun `when visit starts at same time as appointment end time - the session is returned`() {
    // Given
    // appointment is on same date as visit session with start and end time between the visit session time
    // visit time - 09:00 - 10:00
    // appointment time - 08:00 - 09:00

    val eventDate = visitSession1.sessionDate
    val eventStartTime = eventDate.atTime(visitSession1.sessionTimeSlot.startTime.minusMinutes(10))
    val eventEndTime = eventDate.atTime(visitSession1.sessionTimeSlot.startTime)
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3, visitSession4).sortedBy { it.sessionDate }, clientType = PUBLIC)

    val appointment = createScheduledEvent(1L, eventDate, eventType, eventTypeDesc, ADJUDICATION_HEARING.code, ADJUDICATION_HEARING.desc, eventStartTime, eventEndTime)
    prisonApiMockServer.stubGetScheduledEvents(prisonerId, dateRange.fromDate, dateRange.toDate, listOf(appointment))

    // When
    val responseSpec = callGetAvailableVisitSessionsForPvb(webTestClient, prisonCode, prisonerId, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()

    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(4)
    assertThat(availableSessions[0]).isEqualTo(visitSession1)
    assertThat(availableSessions[1]).isEqualTo(visitSession4)
    assertThat(availableSessions[2]).isEqualTo(visitSession2)
    assertThat(availableSessions[3]).isEqualTo(visitSession3)

    verify(appointmentsServiceSpy, times(1)).getHigherPriorityAppointments(prisonerId, dateRange.fromDate, dateRange.toDate)
  }

  @Test
  fun `when appointment starts at same time as visit end time - the session is returned`() {
    // Given
    // appointment is on same date as visit session with start and end time between the visit session time
    // visit time - 09:00 - 10:00
    // appointment time - 10:00 - 11:00

    val eventDate = visitSession1.sessionDate
    val eventStartTime = eventDate.atTime(visitSession1.sessionTimeSlot.endTime)
    val eventEndTime = eventStartTime.plusMinutes(10)
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3, visitSession4).sortedBy { it.sessionDate }, clientType = PUBLIC)

    val appointment = createScheduledEvent(1L, eventDate, eventType, eventTypeDesc, ADJUDICATION_HEARING.code, ADJUDICATION_HEARING.desc, eventStartTime, eventEndTime)
    prisonApiMockServer.stubGetScheduledEvents(prisonerId, dateRange.fromDate, dateRange.toDate, listOf(appointment))

    // When
    val responseSpec = callGetAvailableVisitSessionsForPvb(webTestClient, prisonCode, prisonerId, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()

    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(4)
    assertThat(availableSessions[0]).isEqualTo(visitSession1)
    assertThat(availableSessions[1]).isEqualTo(visitSession4)
    assertThat(availableSessions[2]).isEqualTo(visitSession2)
    assertThat(availableSessions[3]).isEqualTo(visitSession3)

    verify(appointmentsServiceSpy, times(1)).getHigherPriorityAppointments(prisonerId, dateRange.fromDate, dateRange.toDate)
  }

  @Test
  fun `when appointment is not a higher priority appointment all sessions are returned`() {
    // Given
    // not from the higher priority appointments list
    // visit time - 09:00 - 10:00
    // appointment time - 09:00 - 10:00

    // event not in the list of higher priority medical / legal appointments
    val eventSubType = "TEST-SUBTYPE"
    val eventDate = visitSession1.sessionDate
    val eventStartTime = eventDate.atTime(visitSession1.sessionTimeSlot.startTime)
    val eventEndTime = eventDate.atTime(visitSession1.sessionTimeSlot.endTime)
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3, visitSession4).sortedBy { it.sessionDate }, clientType = PUBLIC)

    val appointment = createScheduledEvent(1L, eventDate, eventType, eventTypeDesc, eventSubType, "TEST", eventStartTime, eventEndTime)
    prisonApiMockServer.stubGetScheduledEvents(prisonerId, dateRange.fromDate, dateRange.toDate, listOf(appointment))

    // When
    val responseSpec = callGetAvailableVisitSessionsForPvb(webTestClient, prisonCode, prisonerId, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()

    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(4)
    assertThat(availableSessions[0]).isEqualTo(visitSession1)
    assertThat(availableSessions[1]).isEqualTo(visitSession4)
    assertThat(availableSessions[2]).isEqualTo(visitSession2)
    assertThat(availableSessions[3]).isEqualTo(visitSession3)

    verify(appointmentsServiceSpy, times(1)).getHigherPriorityAppointments(prisonerId, dateRange.fromDate, dateRange.toDate)
  }

  @Test
  fun `when appointment is not of type appointment all sessions are returned`() {
    // Given
    // not of type APP
    // visit time - 09:00 - 10:00
    // appointment time - 09:00 - 10:00

    // event not of type APPOINTMENT
    val eventType = "TEST"
    val eventDate = visitSession1.sessionDate
    val eventStartTime = eventDate.atTime(visitSession1.sessionTimeSlot.startTime)
    val eventEndTime = eventDate.atTime(visitSession1.sessionTimeSlot.endTime)
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3, visitSession4).sortedBy { it.sessionDate }, clientType = PUBLIC)

    val appointment = createScheduledEvent(1L, eventDate, eventType, eventTypeDesc, ADJUDICATION_HEARING.code, ADJUDICATION_HEARING.desc, eventStartTime, eventEndTime)
    prisonApiMockServer.stubGetScheduledEvents(prisonerId, dateRange.fromDate, dateRange.toDate, listOf(appointment))

    // When
    val responseSpec = callGetAvailableVisitSessionsForPvb(webTestClient, prisonCode, prisonerId, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()

    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(4)
    assertThat(availableSessions[0]).isEqualTo(visitSession1)
    assertThat(availableSessions[1]).isEqualTo(visitSession4)
    assertThat(availableSessions[2]).isEqualTo(visitSession2)
    assertThat(availableSessions[3]).isEqualTo(visitSession3)

    verify(appointmentsServiceSpy, times(1)).getHigherPriorityAppointments(prisonerId, dateRange.fromDate, dateRange.toDate)
  }

  @Test
  fun `when call to whereabouts throws 404 then same 404 error status is sent back`() {
    // Given
    prisonApiMockServer.stubGetScheduledEvents(prisonerId, dateRange.fromDate, dateRange.toDate, null)

    // When
    val responseSpec = callGetAvailableVisitSessionsForPvb(webTestClient, prisonCode, prisonerId, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
    verify(appointmentsServiceSpy, times(1)).getHigherPriorityAppointments(prisonerId, dateRange.fromDate, dateRange.toDate)
  }

  @Test
  fun `when call to whereabouts throws INTERNAL_SERVER_ERROR then same INTERNAL_SERVER_ERROR error status is sent back`() {
    // Given
    prisonApiMockServer.stubGetScheduledEvents(prisonerId, dateRange.fromDate, dateRange.toDate, null, HttpStatus.INTERNAL_SERVER_ERROR)

    // When
    val responseSpec = callGetAvailableVisitSessionsForPvb(webTestClient, prisonCode, prisonerId, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().is5xxServerError
    verify(appointmentsServiceSpy, times(1)).getHigherPriorityAppointments(prisonerId, dateRange.fromDate, dateRange.toDate)
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): Array<AvailableVisitSessionDto> = TestObjectMapper.mapper.readValue(returnResult.returnResult().responseBody, Array<AvailableVisitSessionDto>::class.java)
}
