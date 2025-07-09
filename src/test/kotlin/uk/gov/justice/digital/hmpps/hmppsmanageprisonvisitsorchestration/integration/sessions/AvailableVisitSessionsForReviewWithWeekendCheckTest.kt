package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.sessions

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.AlertsApiClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonerContactRegistryClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitSchedulerClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.WhereAboutsApiClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.alerts.api.enums.PrisonerSupportedAlertCodeType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.govuk.holidays.HolidayEventDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.OffenderRestrictionsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.request.review.VisitorRestrictionsForReview
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.AvailableVisitSessionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.DateRange
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionTimeSlotDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSchedulerPrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionRestriction.OPEN
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType.PUBLIC
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.GovUkHolidayService
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.utils.DateUtils
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

@DisplayName("Get available visit sessions marked for review test")
@ExtendWith(MockitoExtension::class)
class AvailableVisitSessionsForReviewWithWeekendCheckTest : IntegrationTestBase() {
  @MockitoSpyBean
  private lateinit var visitSchedulerClientSpy: VisitSchedulerClient

  @MockitoSpyBean
  private lateinit var prisonApiClientSpy: PrisonApiClient

  @MockitoSpyBean
  private lateinit var prisonerContactRegistryClientSpy: PrisonerContactRegistryClient

  @MockitoSpyBean
  private lateinit var whereAboutsApiClientSpy: WhereAboutsApiClient

  @MockitoSpyBean
  private lateinit var alertsApiClientSpy: AlertsApiClient

  @MockitoBean
  private lateinit var dateUtils: DateUtils

  @MockitoBean
  private lateinit var govUkHolidayService: GovUkHolidayService

  private val prisonCode = "MDI"
  private val prisonerId = "AA123456B"

  private val visitSchedulerPrisonDto = VisitSchedulerPrisonDto(prisonCode, true, 2, 28, 6, 3, 3, 18)
  private val visitorIds = listOf(1L, 2L, 3L)

  val visitorRestrictionsForReview = VisitorRestrictionsForReview.entries.map { it.name }

  @BeforeEach
  fun setupMocks() {
    visitSchedulerMockServer.stubGetPrison(prisonCode, visitSchedulerPrisonDto)
    prisonerContactRegistryMockServer.stubDoVisitorsHaveClosedRestrictions(prisonerId, visitorIds = visitorIds, result = false)
  }

  @Test
  fun `when visits fall on a weekend and session review is true any sessions falling on the weekend are removed`() {
    // Given
    val today = if (LocalDate.now().dayOfWeek == DayOfWeek.TUESDAY) LocalDate.now() else LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.TUESDAY))
    val nextSaturday = today.with(TemporalAdjusters.next(DayOfWeek.SATURDAY))
    val nextSunday = today.with(TemporalAdjusters.next(DayOfWeek.SATURDAY))
    val nextMonday = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
    val nextTuesday = today.plusWeeks(1)
    val session1Date = nextSaturday
    val session2Date = nextSunday
    val session3Date = nextMonday
    val session4Date = nextTuesday

    val dateRange = DateRange(today.plusDays(2), today.plusDays(28))
    Mockito.`when`(dateUtils.getToDaysDateRange(any(), any(), any())).thenReturn(dateRange)
    Mockito.`when`(dateUtils.getUniqueDateRanges(any(), any())).thenReturn(listOf(dateRange))
    Mockito.`when`(dateUtils.isDateBetweenDateRanges(any(), any())).thenReturn(true)
    Mockito.`when`(dateUtils.advanceDaysIfWeekendOrBankHoliday(any(), any(), any())).thenReturn(session3Date)

    val visitSession1 = AvailableVisitSessionDto(session1Date, "session1", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    val visitSession2 = AvailableVisitSessionDto(session2Date, "session2", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    val visitSession3 = AvailableVisitSessionDto(session3Date, "session3", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    val visitSession4 = AvailableVisitSessionDto(session4Date, "session4", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)

    val alert1 = createAlertResponseDto(code = PrisonerSupportedAlertCodeType.CC1.name, activeFrom = LocalDate.now(), activeTo = null)

    visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3, visitSession4), userType = PUBLIC, dateRange = dateRange)
    prisonerContactRegistryMockServer.stubGetBannedRestrictionDateRage(prisonerId, visitorIds = visitorIds, dateRange = dateRange, result = dateRange)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, OffenderRestrictionsDto(offenderRestrictions = emptyList()))
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, mutableListOf(alert1))
    prisonerContactRegistryMockServer.stubGetVisitorRestrictionsDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange, emptyList())
    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, emptyList())

    // When
    val responseSpec = callGetAvailableVisitSessionsV2(webTestClient, prisonCode, prisonerId, visitorIds = visitorIds, excludedApplicationReference = null, userType = PUBLIC, userName = null, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()

    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(2)
    assertThat(availableSessions[0].sessionTemplateReference).isEqualTo(visitSession3.sessionTemplateReference)
    assertThat(availableSessions[0].sessionForReview).isTrue
    assertThat(availableSessions[1].sessionTemplateReference).isEqualTo(visitSession4.sessionTemplateReference)
    assertThat(availableSessions[1].sessionForReview).isTrue
    verify(prisonerContactRegistryClientSpy, times(1)).doVisitorsHaveClosedRestrictions(prisonerId, visitorIds)
    verify(prisonerContactRegistryClientSpy, times(1)).getBannedRestrictionDateRange(prisonerId, visitorIds, dateRange)
    verify(prisonerContactRegistryClientSpy, times(0)).getVisitorRestrictionDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange)
    verify(prisonApiClientSpy, times(1)).getPrisonerRestrictions(prisonerId)
    verify(alertsApiClientSpy, times(1)).getPrisonerAlerts(prisonerId)
    verify(visitSchedulerClientSpy, times(1)).getPrison(prisonCode)
    verify(visitSchedulerClientSpy, times(1)).getAvailableVisitSessions(prisonCode, prisonerId, OPEN, dateRange, null, null, PUBLIC)
    verify(whereAboutsApiClientSpy, times(1)).getEvents(prisonerId, dateRange.fromDate, dateRange.toDate)
  }

  @Test
  fun `when visits fall on a weekend and there are holidays after and session review is true any sessions falling on the weekend are removed`() {
    // Given
    val today = if (LocalDate.now().dayOfWeek == DayOfWeek.TUESDAY) LocalDate.now() else LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.TUESDAY))
    val nextSaturday = today.with(TemporalAdjusters.next(DayOfWeek.SATURDAY))
    val nextSunday = today.with(TemporalAdjusters.next(DayOfWeek.SATURDAY))
    // monday is a bank holiday
    val nextMonday = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
    val nextTuesday = today.plusWeeks(1)
    val session1Date = nextSaturday
    val session2Date = nextSunday
    val session3Date = nextMonday
    val session4Date = nextTuesday

    val dateRange = DateRange(today.plusDays(2), today.plusDays(28))
    Mockito.`when`(dateUtils.getToDaysDateRange(any(), any(), any())).thenReturn(dateRange)
    Mockito.`when`(dateUtils.getUniqueDateRanges(any(), any())).thenReturn(listOf(dateRange))
    Mockito.`when`(dateUtils.isDateBetweenDateRanges(any(), any())).thenReturn(true)
    Mockito.`when`(dateUtils.advanceDaysIfWeekendOrBankHoliday(any(), any(), any())).thenReturn(session4Date)
    Mockito.`when`(govUkHolidayService.getGovUKBankHolidays(any<DateRange>())).thenReturn(listOf(HolidayEventDto(date = nextMonday, title = "Bank Holiday Monday")))

    val visitSession1 = AvailableVisitSessionDto(session1Date, "session1", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    val visitSession2 = AvailableVisitSessionDto(session2Date, "session2", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    val visitSession3 = AvailableVisitSessionDto(session3Date, "session3", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    val visitSession4 = AvailableVisitSessionDto(session4Date, "session4", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)

    val alert1 = createAlertResponseDto(code = PrisonerSupportedAlertCodeType.CC1.name, activeFrom = LocalDate.now(), activeTo = null)

    visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3, visitSession4), userType = PUBLIC, dateRange = dateRange)
    prisonerContactRegistryMockServer.stubGetBannedRestrictionDateRage(prisonerId, visitorIds = visitorIds, dateRange = dateRange, result = dateRange)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, OffenderRestrictionsDto(offenderRestrictions = emptyList()))
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, mutableListOf(alert1))
    prisonerContactRegistryMockServer.stubGetVisitorRestrictionsDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange, emptyList())
    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, emptyList())

    // When
    val responseSpec = callGetAvailableVisitSessionsV2(webTestClient, prisonCode, prisonerId, visitorIds = visitorIds, excludedApplicationReference = null, userType = PUBLIC, userName = null, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()

    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(1)
    assertThat(availableSessions[0].sessionTemplateReference).isEqualTo(visitSession4.sessionTemplateReference)
    assertThat(availableSessions[0].sessionForReview).isTrue
    verify(prisonerContactRegistryClientSpy, times(1)).doVisitorsHaveClosedRestrictions(prisonerId, visitorIds)
    verify(prisonerContactRegistryClientSpy, times(1)).getBannedRestrictionDateRange(prisonerId, visitorIds, dateRange)
    verify(prisonerContactRegistryClientSpy, times(0)).getVisitorRestrictionDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange)
    verify(prisonApiClientSpy, times(1)).getPrisonerRestrictions(prisonerId)
    verify(alertsApiClientSpy, times(1)).getPrisonerAlerts(prisonerId)
    verify(visitSchedulerClientSpy, times(1)).getPrison(prisonCode)
    verify(visitSchedulerClientSpy, times(1)).getAvailableVisitSessions(prisonCode, prisonerId, OPEN, dateRange, null, null, PUBLIC)
    verify(whereAboutsApiClientSpy, times(1)).getEvents(prisonerId, dateRange.fromDate, dateRange.toDate)
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): Array<AvailableVisitSessionDto> = objectMapper.readValue(returnResult.returnResult().responseBody, Array<AvailableVisitSessionDto>::class.java)
}
