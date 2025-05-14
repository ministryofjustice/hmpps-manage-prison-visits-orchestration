package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.sessions

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpStatus
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.OffenderRestrictionsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.AvailableVisitSessionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.DateRange
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionTimeSlotDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSchedulerPrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionRestriction.OPEN
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType.PUBLIC
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.whereabouts.enums.HigherPriorityMedicalOrLegalEvents.ADJUDICATION_HEARING
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.whereabouts.enums.HigherPriorityMedicalOrLegalEvents.MEDICAL_OPTICIAN
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.AppointmentsService
import java.time.LocalDate
import java.time.LocalTime

@DisplayName("Get available visit sessions with appointments check")
class AvailableVisitSessionsWithAppointmentsCheckTest : IntegrationTestBase() {

  @MockitoSpyBean
  private lateinit var appointmentsService: AppointmentsService
  private val prisonCode = "MDI"
  private val prisonerId = "AA123456B"
  private val visitSession1 = AvailableVisitSessionDto(LocalDate.now().plusDays(3), "session1", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
  private val visitSession2 = AvailableVisitSessionDto(LocalDate.now().plusDays(4), "session2", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
  private val visitSession3 = AvailableVisitSessionDto(LocalDate.now().plusDays(5), "session3", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
  private val visitSession4 = AvailableVisitSessionDto(visitSession1.sessionDate, "session1", SessionTimeSlotDto(LocalTime.of(15, 0), LocalTime.of(16, 0)), OPEN)

  private val visitSchedulerPrisonDto = VisitSchedulerPrisonDto(prisonCode, true, 2, 28, 6, 3, 3, 18)
  private val visitorIds = listOf(1L, 2L, 3L)

  private val eventType = "APP"
  private val eventTypeDesc = "Appointment"

  @BeforeEach
  fun setupMocks() {
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), userType = PUBLIC)
    visitSchedulerMockServer.stubGetPrison(prisonCode, visitSchedulerPrisonDto)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, OffenderRestrictionsDto(offenderRestrictions = emptyList()))
    prisonerContactRegistryMockServer.stubDoVisitorsHaveClosedRestrictions(prisonerId, visitorIds = visitorIds, result = false)
    prisonerContactRegistryMockServer.stubGetBannedRestrictionDateRage(prisonerId, visitorIds = visitorIds, dateRange = dateRange, result = dateRange)
  }

  @Test
  fun `when there are no clashing appointments sessions are returned`() {
    // Given
    // appointment is not on the same date as the visits
    val dateRange = DateRange(fromDate = LocalDate.now().plusDays(2), LocalDate.now().plusDays(28))

    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, emptyList())

    // When
    val responseSpec = callGetAvailableVisitSessions(webTestClient, prisonCode, prisonerId, OPEN, visitorIds = visitorIds, true, userType = PUBLIC, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()

    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(3)
    assertThat(availableSessions[0]).isEqualTo(visitSession1)
    assertThat(availableSessions[1]).isEqualTo(visitSession2)
    assertThat(availableSessions[2]).isEqualTo(visitSession3)

    verify(appointmentsService, times(1)).getHigherPriorityAppointments(prisonerId, dateRange.fromDate, dateRange.toDate)
  }

  @Test
  fun `when there is a clashing appointment the session is not returned`() {
    // Given
    // appointment is on same date as visit session with start and end time as null
    val eventDate = visitSession1.sessionDate
    val eventStartTime = null
    val eventEndTime = null

    // evening session - same date
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3, visitSession4), userType = PUBLIC)

    val appointment = createScheduledEvent(1L, eventDate, eventType, eventTypeDesc, ADJUDICATION_HEARING.code, ADJUDICATION_HEARING.desc, eventStartTime, eventEndTime)
    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, listOf(appointment))

    // When
    val responseSpec = callGetAvailableVisitSessions(webTestClient, prisonCode, prisonerId, OPEN, visitorIds = visitorIds, true, userType = PUBLIC, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()

    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(2)
    assertThat(availableSessions[0]).isEqualTo(visitSession2)
    assertThat(availableSessions[1]).isEqualTo(visitSession3)

    verify(appointmentsService, times(1)).getHigherPriorityAppointments(prisonerId, dateRange.fromDate, dateRange.toDate)
  }

  @Test
  fun `when there are multiple clashing appointments the session is not returned`() {
    // Given
    // appointment is on same date as visit session with start and end time as null
    val eventDate = visitSession1.sessionDate
    // evening session - same date
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3, visitSession4), userType = PUBLIC)

    val appointment1 = createScheduledEvent(1L, eventDate, eventType, eventTypeDesc, ADJUDICATION_HEARING.code, ADJUDICATION_HEARING.desc, null, null)
    val appointment2 = createScheduledEvent(2L, eventDate, eventType, eventTypeDesc, MEDICAL_OPTICIAN.code, MEDICAL_OPTICIAN.desc, eventDate.atTime(visitSession1.sessionTimeSlot.startTime), eventDate.atTime(visitSession1.sessionTimeSlot.endTime))
    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, listOf(appointment1, appointment2))

    // When
    val responseSpec = callGetAvailableVisitSessions(webTestClient, prisonCode, prisonerId, OPEN, visitorIds = visitorIds, true, userType = PUBLIC, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()

    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(2)
    assertThat(availableSessions[0]).isEqualTo(visitSession2)
    assertThat(availableSessions[1]).isEqualTo(visitSession3)

    verify(appointmentsService, times(1)).getHigherPriorityAppointments(prisonerId, dateRange.fromDate, dateRange.toDate)
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
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3, visitSession4).sortedBy { it.sessionDate }, userType = PUBLIC)

    val appointment = createScheduledEvent(1L, eventDate, eventType, eventTypeDesc, ADJUDICATION_HEARING.code, ADJUDICATION_HEARING.desc, eventStartTime, eventEndTime)
    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, listOf(appointment))

    // When
    val responseSpec = callGetAvailableVisitSessions(webTestClient, prisonCode, prisonerId, OPEN, visitorIds = visitorIds, true, userType = PUBLIC, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()

    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(3)
    assertThat(availableSessions[0]).isEqualTo(visitSession4)
    assertThat(availableSessions[1]).isEqualTo(visitSession2)
    assertThat(availableSessions[2]).isEqualTo(visitSession3)

    verify(appointmentsService, times(1)).getHigherPriorityAppointments(prisonerId, dateRange.fromDate, dateRange.toDate)
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
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3, visitSession4).sortedBy { it.sessionDate }, userType = PUBLIC)

    val appointment = createScheduledEvent(1L, eventDate, eventType, eventTypeDesc, ADJUDICATION_HEARING.code, ADJUDICATION_HEARING.desc, eventStartTime, eventEndTime)
    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, listOf(appointment))

    // When
    val responseSpec = callGetAvailableVisitSessions(webTestClient, prisonCode, prisonerId, OPEN, visitorIds = visitorIds, true, userType = PUBLIC, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()

    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(3)
    assertThat(availableSessions[0]).isEqualTo(visitSession4)
    assertThat(availableSessions[1]).isEqualTo(visitSession2)
    assertThat(availableSessions[2]).isEqualTo(visitSession3)

    verify(appointmentsService, times(1)).getHigherPriorityAppointments(prisonerId, dateRange.fromDate, dateRange.toDate)
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
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3, visitSession4).sortedBy { it.sessionDate }, userType = PUBLIC)

    val appointment = createScheduledEvent(1L, eventDate, eventType, eventTypeDesc, ADJUDICATION_HEARING.code, ADJUDICATION_HEARING.desc, eventStartTime, eventEndTime)
    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, listOf(appointment))

    // When
    val responseSpec = callGetAvailableVisitSessions(webTestClient, prisonCode, prisonerId, OPEN, visitorIds = visitorIds, true, userType = PUBLIC, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()

    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(3)
    assertThat(availableSessions[0]).isEqualTo(visitSession4)
    assertThat(availableSessions[1]).isEqualTo(visitSession2)
    assertThat(availableSessions[2]).isEqualTo(visitSession3)

    verify(appointmentsService, times(1)).getHigherPriorityAppointments(prisonerId, dateRange.fromDate, dateRange.toDate)
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
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3, visitSession4).sortedBy { it.sessionDate }, userType = PUBLIC)

    val appointment = createScheduledEvent(1L, eventDate, eventType, eventTypeDesc, ADJUDICATION_HEARING.code, ADJUDICATION_HEARING.desc, eventStartTime, eventEndTime)
    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, listOf(appointment))

    // When
    val responseSpec = callGetAvailableVisitSessions(webTestClient, prisonCode, prisonerId, OPEN, visitorIds = visitorIds, true, userType = PUBLIC, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()

    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(3)
    assertThat(availableSessions[0]).isEqualTo(visitSession4)
    assertThat(availableSessions[1]).isEqualTo(visitSession2)
    assertThat(availableSessions[2]).isEqualTo(visitSession3)

    verify(appointmentsService, times(1)).getHigherPriorityAppointments(prisonerId, dateRange.fromDate, dateRange.toDate)
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
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3, visitSession4).sortedBy { it.sessionDate }, userType = PUBLIC)

    val appointment = createScheduledEvent(1L, eventDate, eventType, eventTypeDesc, ADJUDICATION_HEARING.code, ADJUDICATION_HEARING.desc, eventStartTime, eventEndTime)
    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, listOf(appointment))

    // When
    val responseSpec = callGetAvailableVisitSessions(webTestClient, prisonCode, prisonerId, OPEN, visitorIds = visitorIds, true, userType = PUBLIC, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()

    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(4)
    assertThat(availableSessions[0]).isEqualTo(visitSession1)
    assertThat(availableSessions[1]).isEqualTo(visitSession4)
    assertThat(availableSessions[2]).isEqualTo(visitSession2)
    assertThat(availableSessions[3]).isEqualTo(visitSession3)

    verify(appointmentsService, times(1)).getHigherPriorityAppointments(prisonerId, dateRange.fromDate, dateRange.toDate)
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
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3, visitSession4).sortedBy { it.sessionDate }, userType = PUBLIC)

    val appointment = createScheduledEvent(1L, eventDate, eventType, eventTypeDesc, ADJUDICATION_HEARING.code, ADJUDICATION_HEARING.desc, eventStartTime, eventEndTime)
    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, listOf(appointment))

    // When
    val responseSpec = callGetAvailableVisitSessions(webTestClient, prisonCode, prisonerId, OPEN, visitorIds = visitorIds, true, userType = PUBLIC, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()

    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(4)
    assertThat(availableSessions[0]).isEqualTo(visitSession1)
    assertThat(availableSessions[1]).isEqualTo(visitSession4)
    assertThat(availableSessions[2]).isEqualTo(visitSession2)
    assertThat(availableSessions[3]).isEqualTo(visitSession3)

    verify(appointmentsService, times(1)).getHigherPriorityAppointments(prisonerId, dateRange.fromDate, dateRange.toDate)
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
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3, visitSession4).sortedBy { it.sessionDate }, userType = PUBLIC)

    val appointment = createScheduledEvent(1L, eventDate, eventType, eventTypeDesc, eventSubType, "TEST", eventStartTime, eventEndTime)
    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, listOf(appointment))

    // When
    val responseSpec = callGetAvailableVisitSessions(webTestClient, prisonCode, prisonerId, OPEN, visitorIds = visitorIds, true, userType = PUBLIC, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()

    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(4)
    assertThat(availableSessions[0]).isEqualTo(visitSession1)
    assertThat(availableSessions[1]).isEqualTo(visitSession4)
    assertThat(availableSessions[2]).isEqualTo(visitSession2)
    assertThat(availableSessions[3]).isEqualTo(visitSession3)

    verify(appointmentsService, times(1)).getHigherPriorityAppointments(prisonerId, dateRange.fromDate, dateRange.toDate)
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
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3, visitSession4).sortedBy { it.sessionDate }, userType = PUBLIC)

    val appointment = createScheduledEvent(1L, eventDate, eventType, eventTypeDesc, ADJUDICATION_HEARING.code, ADJUDICATION_HEARING.desc, eventStartTime, eventEndTime)
    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, listOf(appointment))

    // When
    val responseSpec = callGetAvailableVisitSessions(webTestClient, prisonCode, prisonerId, OPEN, visitorIds = visitorIds, true, userType = PUBLIC, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()

    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(4)
    assertThat(availableSessions[0]).isEqualTo(visitSession1)
    assertThat(availableSessions[1]).isEqualTo(visitSession4)
    assertThat(availableSessions[2]).isEqualTo(visitSession2)
    assertThat(availableSessions[3]).isEqualTo(visitSession3)

    verify(appointmentsService, times(1)).getHigherPriorityAppointments(prisonerId, dateRange.fromDate, dateRange.toDate)
  }

  @Test
  fun `when call to whereabouts throws 404 then same 404 error status is sent back`() {
    // Given
    val dateRange = DateRange(LocalDate.now().plusDays(2), LocalDate.now().plusDays(28))
    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, null)

    // When
    val responseSpec = callGetAvailableVisitSessions(webTestClient, prisonCode, prisonerId, OPEN, visitorIds = visitorIds, true, userType = PUBLIC, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
    verify(appointmentsService, times(1)).getHigherPriorityAppointments(prisonerId, dateRange.fromDate, dateRange.toDate)
  }

  @Test
  fun `when call to whereabouts throws INTERNAL_SERVER_ERROR then same INTERNAL_SERVER_ERROR error status is sent back`() {
    // Given
    val dateRange = DateRange(LocalDate.now().plusDays(2), LocalDate.now().plusDays(28))
    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, null, HttpStatus.INTERNAL_SERVER_ERROR)

    // When
    val responseSpec = callGetAvailableVisitSessions(webTestClient, prisonCode, prisonerId, OPEN, visitorIds = visitorIds, true, userType = PUBLIC, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().is5xxServerError
    verify(appointmentsService, times(1)).getHigherPriorityAppointments(prisonerId, dateRange.fromDate, dateRange.toDate)
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): Array<AvailableVisitSessionDto> = objectMapper.readValue(returnResult.returnResult().responseBody, Array<AvailableVisitSessionDto>::class.java)
}
