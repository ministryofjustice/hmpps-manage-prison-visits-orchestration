package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.sessions

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitSchedulerClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.OffenderRestrictionsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.AvailableVisitSessionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.DateRange
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionTimeSlotDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSchedulerPrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionRestriction.OPEN
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType.PUBLIC
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import java.time.LocalDate
import java.time.LocalTime

@DisplayName("Get available visit sessions with appointments check")
class AvailableVisitSessionsDateRangeTest : IntegrationTestBase() {

  @MockitoSpyBean
  private lateinit var visitSchedulerClient: VisitSchedulerClient
  private val prisonCode = "MDI"
  private val prisonerId = "AA123456B"
  private val visitSession1 = AvailableVisitSessionDto(LocalDate.now().plusDays(3), "session1", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
  private val visitSession2 = AvailableVisitSessionDto(LocalDate.now().plusDays(4), "session2", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
  private val visitSession3 = AvailableVisitSessionDto(LocalDate.now().plusDays(5), "session3", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)

  private val visitSchedulerPrisonDto = VisitSchedulerPrisonDto(prisonCode, true, 2, 28, 6, 3, 3, 18)

  @BeforeEach
  fun setupMocks() {
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), userType = PUBLIC)
    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, emptyList())
    visitSchedulerMockServer.stubGetPrison(prisonCode, visitSchedulerPrisonDto)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, OffenderRestrictionsDto(offenderRestrictions = emptyList()))
  }

  @Test
  fun `when pvbAdvanceFromDateByDays is not passed the original date range is passed`() {
    // Given
    visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), userType = PUBLIC)

    // appointment is not on the same date as the visits
    val dateRange = DateRange(
      fromDate = LocalDate.now().plusDays(visitSchedulerPrisonDto.policyNoticeDaysMin.toLong()),
      toDate = LocalDate.now().plusDays(visitSchedulerPrisonDto.policyNoticeDaysMax.toLong()),
    )

    // When
    callGetAvailableVisitSessions(
      webTestClient,
      prisonCode = prisonCode,
      prisonerId = prisonerId,
      sessionRestriction = OPEN,
      withAppointmentsCheck = true,
      excludedApplicationReference = null,
      pvbAdvanceFromDateByDays = null,
      userType = PUBLIC,
      authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders,
    )
    // Then
    verify(visitSchedulerClient, times(1)).getAvailableVisitSessions(prisonId = prisonCode, prisonerId = prisonerId, sessionRestriction = OPEN, dateRange = dateRange, userType = PUBLIC, excludedApplicationReference = null)
  }

  @Test
  fun `when pvbAdvanceFromDateByDays is passed as 1 the original date range from date is moved by 1`() {
    // Given
    val pvbAdvanceFromDateByDays = 1
    visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), userType = PUBLIC)

    // appointment is not on the same date as the visits
    val dateRange = DateRange(
      fromDate = LocalDate.now().plusDays(visitSchedulerPrisonDto.policyNoticeDaysMin.toLong() + pvbAdvanceFromDateByDays),
      toDate = LocalDate.now().plusDays(visitSchedulerPrisonDto.policyNoticeDaysMax.toLong()),
    )

    // When
    callGetAvailableVisitSessions(
      webTestClient,
      prisonCode = prisonCode,
      prisonerId = prisonerId,
      sessionRestriction = OPEN,
      withAppointmentsCheck = true,
      excludedApplicationReference = null,
      pvbAdvanceFromDateByDays = pvbAdvanceFromDateByDays,
      userType = PUBLIC,
      authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders,
    )

    // Then
    verify(visitSchedulerClient, times(1)).getAvailableVisitSessions(prisonId = prisonCode, prisonerId = prisonerId, sessionRestriction = OPEN, dateRange = dateRange, userType = PUBLIC, excludedApplicationReference = null)
  }

  @Test
  fun `when pvbAdvanceFromDateByDays is passed as 3 the original date range from date is moved by 3`() {
    // Given
    val pvbAdvanceFromDateByDays = 3
    visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), userType = PUBLIC)

    // appointment is not on the same date as the visits
    val dateRange = DateRange(
      fromDate = LocalDate.now().plusDays(visitSchedulerPrisonDto.policyNoticeDaysMin.toLong() + pvbAdvanceFromDateByDays.toLong()),
      toDate = LocalDate.now().plusDays(visitSchedulerPrisonDto.policyNoticeDaysMax.toLong()),
    )

    // When
    callGetAvailableVisitSessions(
      webTestClient,
      prisonCode = prisonCode,
      prisonerId = prisonerId,
      sessionRestriction = OPEN,
      withAppointmentsCheck = true,
      excludedApplicationReference = null,
      pvbAdvanceFromDateByDays = pvbAdvanceFromDateByDays,
      userType = PUBLIC,
      authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders,
    )

    // Then
    verify(visitSchedulerClient, times(1)).getAvailableVisitSessions(prisonId = prisonCode, prisonerId = prisonerId, sessionRestriction = OPEN, dateRange = dateRange, userType = PUBLIC, excludedApplicationReference = null)
  }

  @Test
  fun `when pvbAdvanceFromDateByDays is passed as 0 the original date range from date is not moved`() {
    // Given
    val pvbAdvanceFromDateByDays = 0
    visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), userType = PUBLIC)

    // appointment is not on the same date as the visits
    val dateRange = DateRange(
      fromDate = LocalDate.now().plusDays(visitSchedulerPrisonDto.policyNoticeDaysMin.toLong() + pvbAdvanceFromDateByDays),
      toDate = LocalDate.now().plusDays(visitSchedulerPrisonDto.policyNoticeDaysMax.toLong()),
    )

    // When
    callGetAvailableVisitSessions(
      webTestClient,
      prisonCode = prisonCode,
      prisonerId = prisonerId,
      sessionRestriction = OPEN,
      withAppointmentsCheck = true,
      excludedApplicationReference = null,
      pvbAdvanceFromDateByDays = pvbAdvanceFromDateByDays,
      userType = PUBLIC,
      authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders,
    )

    // Then
    verify(visitSchedulerClient, times(1)).getAvailableVisitSessions(prisonId = prisonCode, prisonerId = prisonerId, sessionRestriction = OPEN, dateRange = dateRange, userType = PUBLIC, excludedApplicationReference = null)
  }

  @Test
  fun `when pvbAdvanceFromDateByDays is passed as more than policy max days the original date range from date is not moved`() {
    // Given
    val pvbAdvanceFromDateByDays = 28
    visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), userType = PUBLIC)

    // appointment is not on the same date as the visits
    val dateRange = DateRange(
      fromDate = LocalDate.now().plusDays(visitSchedulerPrisonDto.policyNoticeDaysMin.toLong()),
      toDate = LocalDate.now().plusDays(visitSchedulerPrisonDto.policyNoticeDaysMax.toLong()),
    )

    // When
    callGetAvailableVisitSessions(
      webTestClient,
      prisonCode = prisonCode,
      prisonerId = prisonerId,
      sessionRestriction = OPEN,
      withAppointmentsCheck = true,
      excludedApplicationReference = null,
      pvbAdvanceFromDateByDays = pvbAdvanceFromDateByDays,
      userType = PUBLIC,
      authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders,
    )

    // Then
    verify(visitSchedulerClient, times(1)).getAvailableVisitSessions(prisonId = prisonCode, prisonerId = prisonerId, sessionRestriction = OPEN, dateRange = dateRange, userType = PUBLIC, excludedApplicationReference = null)
  }

  @Test
  fun `when pvbAdvanceFromDateByDays passed makes from date same as to date then from date is moved`() {
    // Given
    val pvbAdvanceFromDateByDays = visitSchedulerPrisonDto.policyNoticeDaysMax - visitSchedulerPrisonDto.policyNoticeDaysMin
    visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), userType = PUBLIC)

    // appointment is not on the same date as the visits
    val dateRange = DateRange(
      fromDate = LocalDate.now().plusDays(visitSchedulerPrisonDto.policyNoticeDaysMin.toLong() + pvbAdvanceFromDateByDays),
      toDate = LocalDate.now().plusDays(visitSchedulerPrisonDto.policyNoticeDaysMax.toLong()),
    )
    Assertions.assertThat(dateRange.fromDate).isEqualTo(dateRange.toDate)

    // When
    callGetAvailableVisitSessions(
      webTestClient,
      prisonCode = prisonCode,
      prisonerId = prisonerId,
      sessionRestriction = OPEN,
      withAppointmentsCheck = true,
      excludedApplicationReference = null,
      pvbAdvanceFromDateByDays = pvbAdvanceFromDateByDays,
      userType = PUBLIC,
      authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders,
    )

    // Then
    verify(visitSchedulerClient, times(1)).getAvailableVisitSessions(prisonId = prisonCode, prisonerId = prisonerId, sessionRestriction = OPEN, dateRange = dateRange, userType = PUBLIC, excludedApplicationReference = null)
  }

  @Test
  fun `when pvbAdvanceFromDateByDays is passed as a -ve value than policy max days the original date range from date is not moved`() {
    // Given
    val pvbAdvanceFromDateByDays = -2
    visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), userType = PUBLIC)

    // appointment is not on the same date as the visits
    val dateRange = DateRange(
      fromDate = LocalDate.now().plusDays(visitSchedulerPrisonDto.policyNoticeDaysMin.toLong()),
      toDate = LocalDate.now().plusDays(visitSchedulerPrisonDto.policyNoticeDaysMax.toLong()),
    )

    // When
    callGetAvailableVisitSessions(
      webTestClient,
      prisonCode = prisonCode,
      prisonerId = prisonerId,
      sessionRestriction = OPEN,
      withAppointmentsCheck = true,
      excludedApplicationReference = null,
      pvbAdvanceFromDateByDays = pvbAdvanceFromDateByDays,
      userType = PUBLIC,
      authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders,
    )

    // Then
    verify(visitSchedulerClient, times(1)).getAvailableVisitSessions(prisonId = prisonCode, prisonerId = prisonerId, sessionRestriction = OPEN, dateRange = dateRange, userType = PUBLIC, excludedApplicationReference = null)
  }

  @Test
  fun `when fromDateOverride is passed as 2 the original opening booking window is today + 2 days`() {
    // Given
    val fromDateOverride = 5
    visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), userType = PUBLIC)

    // When
    callGetAvailableVisitSessions(
      webTestClient,
      prisonCode = prisonCode,
      prisonerId = prisonerId,
      sessionRestriction = OPEN,
      withAppointmentsCheck = true,
      excludedApplicationReference = null,
      fromDateOverride = fromDateOverride,
      userType = PUBLIC,
      authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders,
    )

    // Then
    val dateRange = DateRange(
      fromDate = LocalDate.now().plusDays(fromDateOverride.toLong()),
      toDate = LocalDate.now().plusDays(visitSchedulerPrisonDto.policyNoticeDaysMax.toLong()),
    )

    verify(visitSchedulerClient, times(1)).getAvailableVisitSessions(prisonId = prisonCode, prisonerId = prisonerId, sessionRestriction = OPEN, dateRange = dateRange, userType = PUBLIC, excludedApplicationReference = null)
  }

  @Test
  fun `when toDateOverride is passed as 20 the original closing booking window is today + 20 days`() {
    // Given
    val toDateOverride = 20
    visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), userType = PUBLIC)

    // When
    callGetAvailableVisitSessions(
      webTestClient,
      prisonCode = prisonCode,
      prisonerId = prisonerId,
      sessionRestriction = OPEN,
      withAppointmentsCheck = true,
      excludedApplicationReference = null,
      toDateOverride = toDateOverride,
      userType = PUBLIC,
      authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders,
    )

    // Then
    val dateRange = DateRange(
      fromDate = LocalDate.now().plusDays(visitSchedulerPrisonDto.policyNoticeDaysMin.toLong()),
      toDate = LocalDate.now().plusDays(toDateOverride.toLong()),
    )

    verify(visitSchedulerClient, times(1)).getAvailableVisitSessions(prisonId = prisonCode, prisonerId = prisonerId, sessionRestriction = OPEN, dateRange = dateRange, userType = PUBLIC, excludedApplicationReference = null)
  }

  @Test
  fun `when usertype not passed to get visit sessions usertype defaults to PUBLIC when visit scheduler client is called`() {
    // Given
    visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), userType = PUBLIC)

    val dateRange = DateRange(
      fromDate = LocalDate.now().plusDays(visitSchedulerPrisonDto.policyNoticeDaysMin.toLong()),
      toDate = LocalDate.now().plusDays(visitSchedulerPrisonDto.policyNoticeDaysMax.toLong()),
    )

    // When
    val responseSpec = callGetAvailableVisitSessions(
      webTestClient,
      prisonCode = prisonCode,
      prisonerId = prisonerId,
      sessionRestriction = OPEN,
      withAppointmentsCheck = true,
      excludedApplicationReference = null,
      pvbAdvanceFromDateByDays = null,
      // userType passed as NULL
      userType = null,
      authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders,
    )

    // Then
    responseSpec.expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(3)

    // Then
    // verify getVisitSessions on visit-scheduler is called with userType = PUBLIC
    verify(visitSchedulerClient, times(1)).getAvailableVisitSessions(prisonCode, prisonerId, sessionRestriction = OPEN, dateRange = dateRange, userType = PUBLIC, excludedApplicationReference = null)
  }
}
