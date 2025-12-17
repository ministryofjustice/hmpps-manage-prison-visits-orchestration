package uk.gov.justice.digital.hmpps.prison.visits.orchestration.integration.sessions

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.alerts.api.enums.PrisonerSupportedAlertCodeType
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.govuk.holidays.HolidayEventByDivisionDto
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.govuk.holidays.HolidayEventDto
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.govuk.holidays.HolidaysDto
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.prison.api.OffenderRestrictionsDto
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.request.review.VisitorRestrictionsForReview
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.visit.scheduler.AvailableVisitSessionDto
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.visit.scheduler.DateRange
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.visit.scheduler.SessionTimeSlotDto
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.visit.scheduler.VisitSchedulerPrisonDto
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.visit.scheduler.enums.SessionRestriction.OPEN
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.visit.scheduler.enums.UserType.PUBLIC
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.utils.CurrentDateUtils
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

@DisplayName("Get available visit sessions marked for review test")
@ExtendWith(MockitoExtension::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AvailableVisitSessionsForReviewWithWeekendCheckTest : IntegrationTestBase() {
  @MockitoBean
  private lateinit var currentDateUtils: CurrentDateUtils

  private val prisonCode = "MDI"
  private val prisonerId = "AA123456B"

  private val visitSchedulerPrisonDto = VisitSchedulerPrisonDto(prisonCode, true, 2, 28, 6, 3, 3, 18)
  private val visitorIds = listOf(1L, 2L, 3L)

  private val visitorRestrictionsForReview = VisitorRestrictionsForReview.entries.map { it.name }

  private val emptyHolidaysDto = HolidaysDto(HolidayEventByDivisionDto(division = "england-and-wales", emptyList()))

  @BeforeEach
  fun setupMocks() {
    visitSchedulerMockServer.stubGetPrison(prisonCode, visitSchedulerPrisonDto)
    prisonerContactRegistryMockServer.stubDoVisitorsHaveClosedRestrictions(prisonerId, visitorIds = visitorIds, result = false)
  }

  @Test
  fun `when first session date falls on a weekend and session review is false weekend dates are not skipped`() {
    // Given
    val today = if (LocalDate.now().dayOfWeek == DayOfWeek.THURSDAY) LocalDate.now() else LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.THURSDAY))

    val dateRange = DateRange(today.plusDays(2).plusDays(1), today.plusDays(28))
    Mockito.`when`(currentDateUtils.getCurrentDate()).thenReturn(today)

    val saturdaySession = AvailableVisitSessionDto(today.plusDays(2), "session3", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    // as this is a weekend but there are no reviews this session will be available
    val sundaySession = AvailableVisitSessionDto(today.plusDays(3), "session4", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    // this should be available
    val mondaySession = AvailableVisitSessionDto(today.plusDays(4), "session5", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    // this should be available
    val nextTuesdaySession = AvailableVisitSessionDto(today.plusDays(5), "session6", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)

    visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(saturdaySession, sundaySession, mondaySession, nextTuesdaySession), userType = PUBLIC, dateRange = dateRange)
    prisonerContactRegistryMockServer.stubGetBannedRestrictionDateRage(prisonerId, visitorIds = visitorIds, dateRange = dateRange, result = dateRange)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, OffenderRestrictionsDto(offenderRestrictions = emptyList()))
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, mutableListOf())
    prisonerContactRegistryMockServer.stubGetVisitorRestrictionsDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange, emptyList())
    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, emptyList())
    govUkMockServer.stubGetBankHolidays(emptyHolidaysDto)

    // When
    val responseSpec = callGetAvailableVisitSessionsPublic(webTestClient, prisonCode, prisonerId, visitorIds = visitorIds, excludedApplicationReference = null, userType = PUBLIC, userName = null, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()

    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(4)
    assertThat(availableSessions.map { it.sessionForReview }).doesNotContain(true)
    assertThat(availableSessions[0].sessionTemplateReference).isEqualTo(saturdaySession.sessionTemplateReference)
    assertThat(availableSessions[1].sessionTemplateReference).isEqualTo(sundaySession.sessionTemplateReference)
    assertThat(availableSessions[2].sessionTemplateReference).isEqualTo(mondaySession.sessionTemplateReference)
    assertThat(availableSessions[3].sessionTemplateReference).isEqualTo(nextTuesdaySession.sessionTemplateReference)
  }

  @Test
  fun `when first session date falls on a weekend and session review is true any sessions falling on the weekend are removed - today being TUESDAY`() {
    // Given
    val today = if (LocalDate.now().dayOfWeek == DayOfWeek.TUESDAY) LocalDate.now() else LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.TUESDAY))

    val dateRange = DateRange(today.plusDays(2).plusDays(1), today.plusDays(28))
    Mockito.`when`(currentDateUtils.getCurrentDate()).thenReturn(today)

    // same day session not available
    val tuesdaySession = AvailableVisitSessionDto(today, "session1", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    // next day session not available
    val wednesdaySession = AvailableVisitSessionDto(today.plusDays(1), "session1", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    // as there are reviews this session will not be available
    val thursdaySession = AvailableVisitSessionDto(today.plusDays(2), "session2", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    // as there are reviews this session will not be available
    val fridaySession = AvailableVisitSessionDto(today.plusDays(3), "session3", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    // as this is a weekend this session will not be available
    val saturdaySession = AvailableVisitSessionDto(today.plusDays(4), "session4", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    // as this is a weekend this session will not be available
    val sundaySession = AvailableVisitSessionDto(today.plusDays(5), "session4", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    // this should be the first session available
    val mondaySession = AvailableVisitSessionDto(today.plusDays(6), "session4", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    // this should be the next session available
    val nextTuesdaySession = AvailableVisitSessionDto(today.plusDays(7), "session4", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)

    val alert1 = createAlertResponseDto(code = PrisonerSupportedAlertCodeType.CC1.name, activeFrom = LocalDate.now(), activeTo = null)

    visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(tuesdaySession, wednesdaySession, thursdaySession, fridaySession, saturdaySession, sundaySession, mondaySession, nextTuesdaySession), userType = PUBLIC, dateRange = dateRange)
    prisonerContactRegistryMockServer.stubGetBannedRestrictionDateRage(prisonerId, visitorIds = visitorIds, dateRange = dateRange, result = dateRange)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, OffenderRestrictionsDto(offenderRestrictions = emptyList()))
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, mutableListOf(alert1))
    prisonerContactRegistryMockServer.stubGetVisitorRestrictionsDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange, emptyList())
    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, emptyList())
    govUkMockServer.stubGetBankHolidays(emptyHolidaysDto)

    // When
    val responseSpec = callGetAvailableVisitSessionsPublic(webTestClient, prisonCode, prisonerId, visitorIds = visitorIds, excludedApplicationReference = null, userType = PUBLIC, userName = null, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()

    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(2)
    assertThat(availableSessions[0].sessionTemplateReference).isEqualTo(mondaySession.sessionTemplateReference)
    assertThat(availableSessions[0].sessionForReview).isTrue
    assertThat(availableSessions[1].sessionTemplateReference).isEqualTo(nextTuesdaySession.sessionTemplateReference)
    assertThat(availableSessions[1].sessionForReview).isTrue
  }

  @Test
  fun `when first session date falls on a weekend and session review is true any sessions falling on the weekend are removed - today being WEDNESDAY`() {
    // Given
    val today = if (LocalDate.now().dayOfWeek == DayOfWeek.WEDNESDAY) LocalDate.now() else LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY))

    val dateRange = DateRange(today.plusDays(2).plusDays(1), today.plusDays(28))
    Mockito.`when`(currentDateUtils.getCurrentDate()).thenReturn(today)

    // same day session not available
    val wednesdaySession = AvailableVisitSessionDto(today, "session1", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    // next day session not available
    val thursdaySession = AvailableVisitSessionDto(today.plusDays(1), "session1", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    // as there are reviews this session will not be available
    val fridaySession = AvailableVisitSessionDto(today.plusDays(2), "session2", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    // as there are reviews this session will not be available
    val saturdaySession = AvailableVisitSessionDto(today.plusDays(3), "session3", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    // as this is a weekend this session will not be available
    val sundaySession = AvailableVisitSessionDto(today.plusDays(4), "session4", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    // this should be the first session available
    val mondaySession = AvailableVisitSessionDto(today.plusDays(5), "session4", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    // this should be the next session available
    val tuesdaySession = AvailableVisitSessionDto(today.plusDays(6), "session4", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)

    val alert1 = createAlertResponseDto(code = PrisonerSupportedAlertCodeType.CC1.name, activeFrom = LocalDate.now(), activeTo = null)

    visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(tuesdaySession, wednesdaySession, thursdaySession, fridaySession, saturdaySession, sundaySession, mondaySession), userType = PUBLIC, dateRange = dateRange)
    prisonerContactRegistryMockServer.stubGetBannedRestrictionDateRage(prisonerId, visitorIds = visitorIds, dateRange = dateRange, result = dateRange)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, OffenderRestrictionsDto(offenderRestrictions = emptyList()))
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, mutableListOf(alert1))
    prisonerContactRegistryMockServer.stubGetVisitorRestrictionsDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange, emptyList())
    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, emptyList())
    govUkMockServer.stubGetBankHolidays(emptyHolidaysDto)

    // When
    val responseSpec = callGetAvailableVisitSessionsPublic(webTestClient, prisonCode, prisonerId, visitorIds = visitorIds, excludedApplicationReference = null, userType = PUBLIC, userName = null, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()

    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(2)
    assertThat(availableSessions.map { it.sessionForReview }).doesNotContain(false)
    assertThat(availableSessions[0].sessionTemplateReference).isEqualTo(mondaySession.sessionTemplateReference)
    assertThat(availableSessions[0].sessionForReview).isTrue
    assertThat(availableSessions[1].sessionTemplateReference).isEqualTo(tuesdaySession.sessionTemplateReference)
    assertThat(availableSessions[1].sessionForReview).isTrue
  }

  @Test
  fun `when first session date falls on a weekend and session review is true any sessions falling on the weekend are removed - today being THURSDAY`() {
    // Given
    val today = if (LocalDate.now().dayOfWeek == DayOfWeek.THURSDAY) LocalDate.now() else LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.THURSDAY))

    val dateRange = DateRange(today.plusDays(2).plusDays(1), today.plusDays(28))
    Mockito.`when`(currentDateUtils.getCurrentDate()).thenReturn(today)

    // same day session not available
    val thursdaySession = AvailableVisitSessionDto(today, "session1", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    // next day session not available
    val fridaySession = AvailableVisitSessionDto(today.plusDays(1), "session2", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    // as there are reviews this session will not be available
    val saturdaySession = AvailableVisitSessionDto(today.plusDays(2), "session3", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    // as there are reviews this session will not be available
    val sundaySession = AvailableVisitSessionDto(today.plusDays(3), "session4", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    // as this is a weekend this session will not be available
    val mondaySession = AvailableVisitSessionDto(today.plusDays(4), "session5", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    // this should be the first session available
    val tuesdaySession = AvailableVisitSessionDto(today.plusDays(5), "session6", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    // this should be the next session available
    val wednesdaySession = AvailableVisitSessionDto(today.plusDays(6), "session7", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)

    val alert1 = createAlertResponseDto(code = PrisonerSupportedAlertCodeType.CC1.name, activeFrom = LocalDate.now(), activeTo = null)

    visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(thursdaySession, fridaySession, saturdaySession, sundaySession, mondaySession, tuesdaySession, wednesdaySession), userType = PUBLIC, dateRange = dateRange)
    prisonerContactRegistryMockServer.stubGetBannedRestrictionDateRage(prisonerId, visitorIds = visitorIds, dateRange = dateRange, result = dateRange)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, OffenderRestrictionsDto(offenderRestrictions = emptyList()))
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, mutableListOf(alert1))
    prisonerContactRegistryMockServer.stubGetVisitorRestrictionsDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange, emptyList())
    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, emptyList())
    govUkMockServer.stubGetBankHolidays(emptyHolidaysDto)

    // When
    val responseSpec = callGetAvailableVisitSessionsPublic(webTestClient, prisonCode, prisonerId, visitorIds = visitorIds, excludedApplicationReference = null, userType = PUBLIC, userName = null, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()

    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(2)
    assertThat(availableSessions.map { it.sessionForReview }).doesNotContain(false)
    assertThat(availableSessions[0].sessionTemplateReference).isEqualTo(tuesdaySession.sessionTemplateReference)
    assertThat(availableSessions[1].sessionTemplateReference).isEqualTo(wednesdaySession.sessionTemplateReference)
  }

  @Test
  fun `when first session date falls on a weekend and session review is true any sessions falling on the weekend are removed - today being FRIDAY`() {
    // Given
    val today = if (LocalDate.now().dayOfWeek == DayOfWeek.FRIDAY) LocalDate.now() else LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.FRIDAY))

    val dateRange = DateRange(today.plusDays(2).plusDays(1), today.plusDays(28))
    Mockito.`when`(currentDateUtils.getCurrentDate()).thenReturn(today)

    // same day session not available
    val fridaySession = AvailableVisitSessionDto(today.plusDays(1), "session2", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    // next day session not available
    val saturdaySession = AvailableVisitSessionDto(today.plusDays(1), "session2", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    // as there are reviews this session will not be available
    val sundaySession = AvailableVisitSessionDto(today.plusDays(2), "session3", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    // as there are reviews this session will not be available
    val mondaySession = AvailableVisitSessionDto(today.plusDays(3), "session4", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    // this should be the first session available
    val tuesdaySession = AvailableVisitSessionDto(today.plusDays(5), "session6", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)

    val alert1 = createAlertResponseDto(code = PrisonerSupportedAlertCodeType.CC1.name, activeFrom = LocalDate.now(), activeTo = null)

    visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(fridaySession, saturdaySession, sundaySession, mondaySession, tuesdaySession), userType = PUBLIC, dateRange = dateRange)
    prisonerContactRegistryMockServer.stubGetBannedRestrictionDateRage(prisonerId, visitorIds = visitorIds, dateRange = dateRange, result = dateRange)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, OffenderRestrictionsDto(offenderRestrictions = emptyList()))
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, mutableListOf(alert1))
    prisonerContactRegistryMockServer.stubGetVisitorRestrictionsDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange, emptyList())
    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, emptyList())
    govUkMockServer.stubGetBankHolidays(emptyHolidaysDto)

    // When
    val responseSpec = callGetAvailableVisitSessionsPublic(webTestClient, prisonCode, prisonerId, visitorIds = visitorIds, excludedApplicationReference = null, userType = PUBLIC, userName = null, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()

    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(1)
    assertThat(availableSessions.map { it.sessionForReview }).doesNotContain(false)
    assertThat(availableSessions[0].sessionTemplateReference).isEqualTo(tuesdaySession.sessionTemplateReference)
  }

  @Test
  fun `when first session date falls on a weekend and session review is true any sessions falling on the weekend are removed - today being SATURDAY`() {
    // Given
    val today = if (LocalDate.now().dayOfWeek == DayOfWeek.SATURDAY) LocalDate.now() else LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.SATURDAY))

    val dateRange = DateRange(today.plusDays(2).plusDays(1), today.plusDays(28))
    Mockito.`when`(currentDateUtils.getCurrentDate()).thenReturn(today)

    // same day session not available
    val saturdaySession = AvailableVisitSessionDto(today.plusDays(1), "session2", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    // next day session not available
    val sundaySession = AvailableVisitSessionDto(today.plusDays(1), "session2", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    // as there are reviews this session will not be available
    val mondaySession = AvailableVisitSessionDto(today.plusDays(2), "session3", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    // as there are reviews this session will not be available
    val tuesdaySession = AvailableVisitSessionDto(today.plusDays(3), "session4", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    // this should be the first session available
    val wednesdaySession = AvailableVisitSessionDto(today.plusDays(5), "session6", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)

    val alert1 = createAlertResponseDto(code = PrisonerSupportedAlertCodeType.CC1.name, activeFrom = LocalDate.now(), activeTo = null)

    visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(saturdaySession, sundaySession, mondaySession, tuesdaySession, wednesdaySession), userType = PUBLIC, dateRange = dateRange)
    prisonerContactRegistryMockServer.stubGetBannedRestrictionDateRage(prisonerId, visitorIds = visitorIds, dateRange = dateRange, result = dateRange)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, OffenderRestrictionsDto(offenderRestrictions = emptyList()))
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, mutableListOf(alert1))
    prisonerContactRegistryMockServer.stubGetVisitorRestrictionsDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange, emptyList())
    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, emptyList())
    govUkMockServer.stubGetBankHolidays(emptyHolidaysDto)

    // When
    val responseSpec = callGetAvailableVisitSessionsPublic(webTestClient, prisonCode, prisonerId, visitorIds = visitorIds, excludedApplicationReference = null, userType = PUBLIC, userName = null, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()

    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(1)
    assertThat(availableSessions.map { it.sessionForReview }).doesNotContain(false)
    assertThat(availableSessions[0].sessionTemplateReference).isEqualTo(wednesdaySession.sessionTemplateReference)
  }

  @Test
  fun `when visits fall on a weekend and there are holidays after and session review is true any sessions falling on the weekend are removed`() {
    // Given
    val today = if (LocalDate.now().dayOfWeek == DayOfWeek.TUESDAY) LocalDate.now() else LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.TUESDAY))
    // same day session not available
    val tuesdaySession = AvailableVisitSessionDto(today, "session1", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    // next day session not available
    val wednesdaySession = AvailableVisitSessionDto(today.plusDays(1), "session1", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    // as there are reviews this session will not be available
    val thursdaySession = AvailableVisitSessionDto(today.plusDays(2), "session2", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    // as there are reviews this session will not be available
    val fridaySession = AvailableVisitSessionDto(today.plusDays(3), "session3", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    // as this is a weekend this session will not be available
    val saturdaySession = AvailableVisitSessionDto(today.plusDays(4), "session4", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    // as this is a weekend this session will not be available
    val sundaySession = AvailableVisitSessionDto(today.plusDays(5), "session4", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    // this should be the first session but this should not be available as MONDAY is a holiday
    val mondaySession = AvailableVisitSessionDto(today.plusDays(6), "session4", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    // this should be the first session available
    val nextTuesdaySession = AvailableVisitSessionDto(today.plusDays(7), "session4", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)

    val dateRange = DateRange(today.plusDays(2).plusDays(1), today.plusDays(28))
    Mockito.`when`(currentDateUtils.getCurrentDate()).thenReturn(today)
    val holidaysDto = HolidaysDto(
      englandAndWalesHolidays = HolidayEventByDivisionDto(
        division = "england-and-wales",
        events = listOf(HolidayEventDto(date = mondaySession.sessionDate, title = "Bank Holiday Monday")),
      ),
    )

    val alert1 = createAlertResponseDto(code = PrisonerSupportedAlertCodeType.CC1.name, activeFrom = LocalDate.now(), activeTo = null)

    visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(tuesdaySession, wednesdaySession, thursdaySession, fridaySession, saturdaySession, sundaySession, mondaySession, nextTuesdaySession), userType = PUBLIC, dateRange = dateRange)
    prisonerContactRegistryMockServer.stubGetBannedRestrictionDateRage(prisonerId, visitorIds = visitorIds, dateRange = dateRange, result = dateRange)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, OffenderRestrictionsDto(offenderRestrictions = emptyList()))
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, mutableListOf(alert1))
    prisonerContactRegistryMockServer.stubGetVisitorRestrictionsDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange, emptyList())
    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, emptyList())
    govUkMockServer.stubGetBankHolidays(holidaysDto)
    // When
    val responseSpec = callGetAvailableVisitSessionsPublic(webTestClient, prisonCode, prisonerId, visitorIds = visitorIds, excludedApplicationReference = null, userType = PUBLIC, userName = null, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()

    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(1)
    assertThat(availableSessions[0].sessionTemplateReference).isEqualTo(nextTuesdaySession.sessionTemplateReference)
    assertThat(availableSessions[0].sessionForReview).isTrue
  }

  @Test
  fun `when visits fall on a weekend and there are 2 holidays after and session review is true any sessions falling on the weekend are removed`() {
    // Given
    val today = if (LocalDate.now().dayOfWeek == DayOfWeek.TUESDAY) LocalDate.now() else LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.TUESDAY))
    // same day session not available
    val tuesdaySession = AvailableVisitSessionDto(today, "session1", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    // next day session not available
    val wednesdaySession = AvailableVisitSessionDto(today.plusDays(1), "session1", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    // 2 days after session not available
    val thursdaySession = AvailableVisitSessionDto(today.plusDays(2), "session2", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    // as there are reviews this session will not be available
    val fridaySession = AvailableVisitSessionDto(today.plusDays(3), "session3", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    // as there are reviews this session will not be available
    val saturdaySession = AvailableVisitSessionDto(today.plusDays(4), "session4", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    // as this is a weekend this session will not be available
    val sundaySession = AvailableVisitSessionDto(today.plusDays(5), "session5", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    // this should be the first session but this should not be available as MONDAY is a holiday
    val mondaySession = AvailableVisitSessionDto(today.plusDays(6), "session6", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    // this should be the next session but this should not be available as TUESDAY is a holiday
    val nextTuesdaySession = AvailableVisitSessionDto(today.plusDays(7), "session7", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    // this should be the first session available
    val nextWednesdaySession = AvailableVisitSessionDto(today.plusDays(8), "session8", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)

    val dateRange = DateRange(today.plusDays(2).plusDays(1), today.plusDays(28))
    Mockito.`when`(currentDateUtils.getCurrentDate()).thenReturn(today)
    val holidaysDto = HolidaysDto(
      englandAndWalesHolidays = HolidayEventByDivisionDto(
        division = "england-and-wales",
        events = listOf(
          HolidayEventDto(date = mondaySession.sessionDate, title = "Bank Holiday Monday"),
          HolidayEventDto(date = nextTuesdaySession.sessionDate, title = "Bank Holiday Tuesday"),
        ),
      ),
    )

    val alert1 = createAlertResponseDto(code = PrisonerSupportedAlertCodeType.CC1.name, activeFrom = LocalDate.now(), activeTo = null)

    visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, dateRange = dateRange, visitSessions = mutableListOf(tuesdaySession, wednesdaySession, thursdaySession, fridaySession, saturdaySession, sundaySession, mondaySession, nextTuesdaySession, nextWednesdaySession), userType = PUBLIC)
    prisonerContactRegistryMockServer.stubGetBannedRestrictionDateRage(prisonerId, visitorIds = visitorIds, dateRange = dateRange, result = dateRange)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, OffenderRestrictionsDto(offenderRestrictions = emptyList()))
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, mutableListOf(alert1))
    prisonerContactRegistryMockServer.stubGetVisitorRestrictionsDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange, emptyList())
    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, emptyList())
    govUkMockServer.stubGetBankHolidays(holidaysDto)
    // When
    val responseSpec = callGetAvailableVisitSessionsPublic(webTestClient, prisonCode, prisonerId, visitorIds = visitorIds, excludedApplicationReference = null, userType = PUBLIC, userName = null, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()

    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(1)
    assertThat(availableSessions[0].sessionTemplateReference).isEqualTo(nextWednesdaySession.sessionTemplateReference)
    assertThat(availableSessions[0].sessionForReview).isTrue
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): Array<AvailableVisitSessionDto> = objectMapper.readValue(returnResult.returnResult().responseBody, Array<AvailableVisitSessionDto>::class.java)
}
