package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.sessions

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config.CacheNames
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.govuk.holidays.HolidayEventByDivisionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.govuk.holidays.HolidaysDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.OffenderRestrictionsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.AvailableVisitSessionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.DateRange
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionTimeSlotDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.PrisonClientType.PUBLIC
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.PrisonClientType.STAFF
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionRestriction.OPEN
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionTemplateVisitOrderRestrictionType.NONE
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@DisplayName("STAFF calendar-day windows and overrides, and the PVB default client type")
class AvailableVisitSessionsDateRangeTest : IntegrationTestBase() {
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
  private val visitSession1 = AvailableVisitSessionDto(LocalDate.now().plusDays(3), "session1", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN, visitOrderRestriction = NONE)
  private val visitSession2 = AvailableVisitSessionDto(LocalDate.now().plusDays(4), "session2", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN, visitOrderRestriction = NONE)
  private val visitSession3 = AvailableVisitSessionDto(LocalDate.now().plusDays(5), "session3", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN, visitOrderRestriction = NONE)

  private val visitSchedulerPrisonDto = createVisitSchedulerPrisonDto(prisonCode, active = true, maxTotalVisitors = 6, maxAdultVisitors = 3, maxChildVisitors = 3, policyNoticeDaysMin = 2, policyNoticeDaysMax = 28, adultAgeYears = 18, weekStartDay = DayOfWeek.MONDAY, remandVisitLimitPerWeek = 3)
  val staffClient = visitSchedulerPrisonDto.clients.first { it.clientType == STAFF }

  @BeforeEach
  fun setupMocks() {
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), clientType = STAFF)
    prisonApiMockServer.stubGetScheduledEvents(prisonerId, dateRange.fromDate, dateRange.toDate, emptyList())
    visitSchedulerMockServer.stubGetPrison(prisonCode, visitSchedulerPrisonDto)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, OffenderRestrictionsDto(offenderRestrictions = emptyList()))
  }

  @Test
  fun `when pvbAdvanceFromDateByDays is not passed the original date range is passed`() {
    // Given
    visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), clientType = STAFF)

    // appointment is not on the same date as the visits
    val dateRange = DateRange(
      fromDate = LocalDate.now().plusDays(staffClient.policyNoticeDaysMin.toLong()),
      toDate = LocalDate.now().plusDays(staffClient.policyNoticeDaysMax.toLong()),
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
      clientType = STAFF,
      authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders,
    )
    // Then
    verify(visitSchedulerClientSpy, times(1)).getAvailableVisitSessions(prisonId = prisonCode, prisonerId = prisonerId, sessionRestriction = OPEN, dateRange = dateRange, clientType = STAFF, excludedApplicationReference = null)
  }

  @Test
  fun `when pvbAdvanceFromDateByDays is passed as 1 the original date range from date is moved by 1`() {
    // Given
    val pvbAdvanceFromDateByDays = 1
    visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), clientType = STAFF)

    // appointment is not on the same date as the visits
    val dateRange = DateRange(
      fromDate = LocalDate.now().plusDays(staffClient.policyNoticeDaysMin.toLong() + pvbAdvanceFromDateByDays),
      toDate = LocalDate.now().plusDays(staffClient.policyNoticeDaysMax.toLong()),
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
      clientType = STAFF,
      authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders,
    )

    // Then
    verify(visitSchedulerClientSpy, times(1)).getAvailableVisitSessions(prisonId = prisonCode, prisonerId = prisonerId, sessionRestriction = OPEN, dateRange = dateRange, clientType = STAFF, excludedApplicationReference = null)
  }

  @Test
  fun `when pvbAdvanceFromDateByDays is passed as 3 the original date range from date is moved by 3`() {
    // Given
    val pvbAdvanceFromDateByDays = 3
    visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), clientType = STAFF)

    // appointment is not on the same date as the visits
    val dateRange = DateRange(
      fromDate = LocalDate.now().plusDays(staffClient.policyNoticeDaysMin.toLong() + pvbAdvanceFromDateByDays.toLong()),
      toDate = LocalDate.now().plusDays(staffClient.policyNoticeDaysMax.toLong()),
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
      clientType = STAFF,
      authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders,
    )

    // Then
    verify(visitSchedulerClientSpy, times(1)).getAvailableVisitSessions(prisonId = prisonCode, prisonerId = prisonerId, sessionRestriction = OPEN, dateRange = dateRange, clientType = STAFF, excludedApplicationReference = null)
  }

  @Test
  fun `when pvbAdvanceFromDateByDays is passed as 0 the original date range from date is not moved`() {
    // Given
    val pvbAdvanceFromDateByDays = 0
    visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), clientType = STAFF)

    // appointment is not on the same date as the visits
    val dateRange = DateRange(
      fromDate = LocalDate.now().plusDays(staffClient.policyNoticeDaysMin.toLong() + pvbAdvanceFromDateByDays),
      toDate = LocalDate.now().plusDays(staffClient.policyNoticeDaysMax.toLong()),
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
      clientType = STAFF,
      authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders,
    )

    // Then
    verify(visitSchedulerClientSpy, times(1)).getAvailableVisitSessions(prisonId = prisonCode, prisonerId = prisonerId, sessionRestriction = OPEN, dateRange = dateRange, clientType = STAFF, excludedApplicationReference = null)
  }

  @Test
  fun `when pvbAdvanceFromDateByDays is passed as more than policy max days the original date range from date is not moved`() {
    // Given
    val pvbAdvanceFromDateByDays = 28
    visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), clientType = STAFF)

    // appointment is not on the same date as the visits
    val dateRange = DateRange(
      fromDate = LocalDate.now().plusDays(staffClient.policyNoticeDaysMin.toLong()),
      toDate = LocalDate.now().plusDays(staffClient.policyNoticeDaysMax.toLong()),
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
      clientType = STAFF,
      authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders,
    )

    // Then
    verify(visitSchedulerClientSpy, times(1)).getAvailableVisitSessions(prisonId = prisonCode, prisonerId = prisonerId, sessionRestriction = OPEN, dateRange = dateRange, clientType = STAFF, excludedApplicationReference = null)
  }

  @Test
  fun `when pvbAdvanceFromDateByDays passed makes from date same as to date then from date is moved`() {
    // Given
    val pvbAdvanceFromDateByDays = staffClient.policyNoticeDaysMax - (staffClient.policyNoticeDaysMin)
    visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), clientType = STAFF)

    // appointment is not on the same date as the visits
    val dateRange = DateRange(
      fromDate = LocalDate.now().plusDays(staffClient.policyNoticeDaysMin.toLong() + pvbAdvanceFromDateByDays),
      toDate = LocalDate.now().plusDays(staffClient.policyNoticeDaysMax.toLong()),
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
      clientType = STAFF,
      authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders,
    )

    // Then
    verify(visitSchedulerClientSpy, times(1)).getAvailableVisitSessions(prisonId = prisonCode, prisonerId = prisonerId, sessionRestriction = OPEN, dateRange = dateRange, clientType = STAFF, excludedApplicationReference = null)
  }

  @Test
  fun `when pvbAdvanceFromDateByDays is passed as a -ve value than policy max days the original date range from date is not moved`() {
    // Given
    val pvbAdvanceFromDateByDays = -2
    visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), clientType = STAFF)

    // appointment is not on the same date as the visits
    val dateRange = DateRange(
      fromDate = LocalDate.now().plusDays(staffClient.policyNoticeDaysMin.toLong()),
      toDate = LocalDate.now().plusDays(staffClient.policyNoticeDaysMax.toLong()),
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
      clientType = STAFF,
      authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders,
    )

    // Then
    verify(visitSchedulerClientSpy, times(1)).getAvailableVisitSessions(prisonId = prisonCode, prisonerId = prisonerId, sessionRestriction = OPEN, dateRange = dateRange, clientType = STAFF, excludedApplicationReference = null)
  }

  @Test
  fun `when fromDateOverride is passed as 2 the original opening booking window is today + 2 days`() {
    // Given
    val fromDateOverride = 5
    visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), clientType = STAFF)

    // When
    callGetAvailableVisitSessions(
      webTestClient,
      prisonCode = prisonCode,
      prisonerId = prisonerId,
      sessionRestriction = OPEN,
      withAppointmentsCheck = true,
      excludedApplicationReference = null,
      fromDateOverride = fromDateOverride,
      clientType = STAFF,
      authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders,
    )

    // Then
    val dateRange = DateRange(
      fromDate = LocalDate.now().plusDays(fromDateOverride.toLong()),
      toDate = LocalDate.now().plusDays(staffClient.policyNoticeDaysMax.toLong()),
    )

    verify(visitSchedulerClientSpy, times(1)).getAvailableVisitSessions(prisonId = prisonCode, prisonerId = prisonerId, sessionRestriction = OPEN, dateRange = dateRange, clientType = STAFF, excludedApplicationReference = null)
  }

  @Test
  fun `when toDateOverride is passed as 20 the original closing booking window is today + 20 days`() {
    // Given
    val toDateOverride = 20
    visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), clientType = STAFF)

    // When
    callGetAvailableVisitSessions(
      webTestClient,
      prisonCode = prisonCode,
      prisonerId = prisonerId,
      sessionRestriction = OPEN,
      withAppointmentsCheck = true,
      excludedApplicationReference = null,
      toDateOverride = toDateOverride,
      clientType = STAFF,
      authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders,
    )

    // Then
    val dateRange = DateRange(
      fromDate = LocalDate.now().plusDays(staffClient.policyNoticeDaysMin.toLong()),
      toDate = LocalDate.now().plusDays(toDateOverride.toLong()),
    )

    verify(visitSchedulerClientSpy, times(1)).getAvailableVisitSessions(prisonId = prisonCode, prisonerId = prisonerId, sessionRestriction = OPEN, dateRange = dateRange, clientType = STAFF, excludedApplicationReference = null)
  }

  @Test
  fun `when fromDateOverride is less than the prison configured min the first date offered is today + prison configured min days`() {
    // Given
    // the fromDateOverride passed is less than the allowed prison config days
    val fromDateOverride = 1
    visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), clientType = STAFF)

    // When
    callGetAvailableVisitSessions(
      webTestClient,
      prisonCode = prisonCode,
      prisonerId = prisonerId,
      sessionRestriction = OPEN,
      withAppointmentsCheck = true,
      excludedApplicationReference = null,
      fromDateOverride = fromDateOverride,
      clientType = STAFF,
      authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders,
    )

    // Then
    // date range should ignore the fromDateOverride as it is less than the prison configured min value
    val dateRange = DateRange(
      fromDate = LocalDate.now().plusDays(staffClient.policyNoticeDaysMin.toLong()),
      toDate = LocalDate.now().plusDays(staffClient.policyNoticeDaysMax.toLong()),
    )

    verify(visitSchedulerClientSpy, times(1)).getAvailableVisitSessions(prisonId = prisonCode, prisonerId = prisonerId, sessionRestriction = OPEN, dateRange = dateRange, clientType = STAFF, excludedApplicationReference = null)
  }

  @Test
  fun `when fromDateOverride is more than the prison configured min the first date offered is today + fromDateOverride`() {
    // Given
    // the fromDateOverride passed is more than the allowed prison config days
    val fromDateOverride = 15
    visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), clientType = STAFF)

    // When
    callGetAvailableVisitSessions(
      webTestClient,
      prisonCode = prisonCode,
      prisonerId = prisonerId,
      sessionRestriction = OPEN,
      withAppointmentsCheck = true,
      excludedApplicationReference = null,
      fromDateOverride = fromDateOverride,
      clientType = STAFF,
      authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders,
    )

    // Then
    // date range should use the fromDateOverride as it is more than the prison configured min value
    val dateRange = DateRange(
      fromDate = LocalDate.now().plusDays(fromDateOverride.toLong()),
      toDate = LocalDate.now().plusDays(staffClient.policyNoticeDaysMax.toLong()),
    )

    verify(visitSchedulerClientSpy, times(1)).getAvailableVisitSessions(prisonId = prisonCode, prisonerId = prisonerId, sessionRestriction = OPEN, dateRange = dateRange, clientType = STAFF, excludedApplicationReference = null)
  }

  @Test
  fun `when toDateOverride is less than the prison configured max the first date offered is today + toDateOverride`() {
    // Given
    // the toDateOverride passed is less than the allowed prison config days
    val toDateOverride = 15
    visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), clientType = STAFF)

    // When
    callGetAvailableVisitSessions(
      webTestClient,
      prisonCode = prisonCode,
      prisonerId = prisonerId,
      sessionRestriction = OPEN,
      withAppointmentsCheck = true,
      excludedApplicationReference = null,
      toDateOverride = toDateOverride,
      clientType = STAFF,
      authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders,
    )

    // Then
    // date range should use the toDateOverride as it is more than the prison configured max value
    val dateRange = DateRange(
      fromDate = LocalDate.now().plusDays(staffClient.policyNoticeDaysMin.toLong()),
      toDate = LocalDate.now().plusDays(toDateOverride.toLong()),
    )

    verify(visitSchedulerClientSpy, times(1)).getAvailableVisitSessions(prisonId = prisonCode, prisonerId = prisonerId, sessionRestriction = OPEN, dateRange = dateRange, clientType = STAFF, excludedApplicationReference = null)
  }

  @Test
  fun `when toDateOverride is more than the prison configured max the first date offered is today + prison configured max days`() {
    // Given
    // the toDateOverride passed is more than the allowed prison config days

    val toDateOverride = 56
    visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), clientType = STAFF)

    // When
    callGetAvailableVisitSessions(
      webTestClient,
      prisonCode = prisonCode,
      prisonerId = prisonerId,
      sessionRestriction = OPEN,
      withAppointmentsCheck = true,
      excludedApplicationReference = null,
      toDateOverride = toDateOverride,
      clientType = STAFF,
      authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders,
    )

    // Then
    // date range should ignore the toDateOverride as it is more than the prison configured max value
    val dateRange = DateRange(
      fromDate = LocalDate.now().plusDays(staffClient.policyNoticeDaysMin.toLong()),
      toDate = LocalDate.now().plusDays(staffClient.policyNoticeDaysMax.toLong()),
    )

    verify(visitSchedulerClientSpy, times(1)).getAvailableVisitSessions(prisonId = prisonCode, prisonerId = prisonerId, sessionRestriction = OPEN, dateRange = dateRange, clientType = STAFF, excludedApplicationReference = null)
  }

  @Test
  fun `when usertype not passed to get visit sessions usertype defaults to PUBLIC when visit scheduler client is called`() {
    // Given
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), clientType = PUBLIC)
    prisonApiMockServer.stubGetScheduledEvents(prisonerId, dateRange.fromDate, dateRange.toDate, emptyList())

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
      clientType = null,
      authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders,
    )

    // Then
    responseSpec.expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(3)

    // Then
    // verify getVisitSessions on visit-scheduler is called with clientType = PUBLIC
    verify(visitSchedulerClientSpy, times(1)).getAvailableVisitSessions(prisonCode, prisonerId, sessionRestriction = OPEN, dateRange = dateRange, clientType = PUBLIC, excludedApplicationReference = null)
  }
}
