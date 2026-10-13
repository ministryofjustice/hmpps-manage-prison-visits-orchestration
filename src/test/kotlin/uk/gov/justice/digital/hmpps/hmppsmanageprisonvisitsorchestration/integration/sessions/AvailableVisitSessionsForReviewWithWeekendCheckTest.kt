package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.sessions

import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager
import org.springframework.http.HttpStatus
import org.springframework.test.context.bean.override.mockito.MockitoBean
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config.CacheNames
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.alerts.api.enums.PrisonerSupportedAlertCodeType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.govuk.holidays.HolidayEventByDivisionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.govuk.holidays.HolidayEventDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.govuk.holidays.HolidaysDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.OffenderRestrictionsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.request.review.VisitorRestrictionsForReview
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.AvailableVisitSessionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.DateRange
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionTimeSlotDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.PrisonClientType.PUBLIC
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionRestriction.OPEN
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionTemplateVisitOrderRestrictionType.NONE
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.TestObjectMapper
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.utils.CurrentDateUtils
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import java.util.stream.Stream

@DisplayName("PUBLIC available visit sessions use full working days for the minimum booking window")
class AvailableVisitSessionsForReviewWithWeekendCheckTest : IntegrationTestBase() {

  companion object {
    @JvmStatic
    fun bookingWindowCases(): Stream<Arguments> {
      // Arguments: today offset, minimum working days, expected first-day offset, requires review.
      // All offsets are measured from the reference Monday.
      return Stream.of(
        // Monday, 1 working day: visits start on Wednesday.
        Arguments.of(0L, 1, 2L, false),
        Arguments.of(0L, 1, 2L, true),

        // Monday, 2 working days: visits start on Thursday.
        Arguments.of(0L, 2, 3L, false),
        Arguments.of(0L, 2, 3L, true),

        // Monday, 3 working days: visits start on Friday.
        Arguments.of(0L, 3, 4L, false),
        Arguments.of(0L, 3, 4L, true),

        // Tuesday, 1 working day: visits start on Thursday.
        Arguments.of(1L, 1, 3L, false),
        Arguments.of(1L, 1, 3L, true),

        // Tuesday, 2 working days: visits start on Friday.
        Arguments.of(1L, 2, 4L, false),
        Arguments.of(1L, 2, 4L, true),

        // Tuesday, 3 working days: visits start on Saturday.
        Arguments.of(1L, 3, 5L, false),
        Arguments.of(1L, 3, 5L, true),

        // Wednesday, 1 working day: visits start on Friday.
        Arguments.of(2L, 1, 4L, false),
        Arguments.of(2L, 1, 4L, true),

        // Wednesday, 2 working days: visits start on Saturday.
        Arguments.of(2L, 2, 5L, false),
        Arguments.of(2L, 2, 5L, true),

        // Wednesday, 3 working days: visits start next Tuesday.
        Arguments.of(2L, 3, 8L, false),
        Arguments.of(2L, 3, 8L, true),

        // Thursday, 1 working day: visits start on Saturday.
        Arguments.of(3L, 1, 5L, false),
        Arguments.of(3L, 1, 5L, true),

        // Thursday, 2 working days: visits start next Tuesday.
        Arguments.of(3L, 2, 8L, false),
        Arguments.of(3L, 2, 8L, true),

        // Thursday, 3 working days: visits start next Wednesday.
        Arguments.of(3L, 3, 9L, false),
        Arguments.of(3L, 3, 9L, true),

        // Friday, 1 working day: visits start next Tuesday.
        Arguments.of(4L, 1, 8L, false),
        Arguments.of(4L, 1, 8L, true),

        // Friday, 2 working days: visits start next Wednesday.
        Arguments.of(4L, 2, 9L, false),
        Arguments.of(4L, 2, 9L, true),

        // Friday, 3 working days: visits start next Thursday.
        Arguments.of(4L, 3, 10L, false),
        Arguments.of(4L, 3, 10L, true),

        // Saturday, 1 working day: visits start next Tuesday.
        Arguments.of(5L, 1, 8L, false),
        Arguments.of(5L, 1, 8L, true),

        // Saturday, 2 working days: visits start next Wednesday.
        Arguments.of(5L, 2, 9L, false),
        Arguments.of(5L, 2, 9L, true),

        // Saturday, 3 working days: visits start next Thursday.
        Arguments.of(5L, 3, 10L, false),
        Arguments.of(5L, 3, 10L, true),

        // Sunday, 1 working day: visits start next Tuesday.
        Arguments.of(6L, 1, 8L, false),
        Arguments.of(6L, 1, 8L, true),

        // Sunday, 2 working days: visits start next Wednesday.
        Arguments.of(6L, 2, 9L, false),
        Arguments.of(6L, 2, 9L, true),

        // Sunday, 3 working days: visits start next Thursday.
        Arguments.of(6L, 3, 10L, false),
        Arguments.of(6L, 3, 10L, true),
      )
    }

    @JvmStatic
    fun bankHolidayCases(): Stream<Arguments> {
      // Arguments: today offset, minimum working days, holiday offsets, expected first-day offset, requires review.
      return Stream.of(
        // A holiday during the notice period, including one before the old calendar-day start date.
        Arguments.of(0L, 1, listOf(1L), 3L, false),
        Arguments.of(0L, 1, listOf(1L), 3L, true),
        Arguments.of(0L, 3, listOf(1L), 5L, false),
        Arguments.of(0L, 3, listOf(1L), 5L, true),
        // A Friday holiday followed by a weekend, then a Monday holiday.
        Arguments.of(3L, 1, listOf(4L, 7L), 9L, false),
        Arguments.of(3L, 1, listOf(4L, 7L), 9L, true),
        // Consecutive holidays after a weekend.
        Arguments.of(4L, 2, listOf(7L, 8L), 11L, false),
        Arguments.of(4L, 2, listOf(7L, 8L), 11L, true),
        // Visits can start on a holiday after the working-day notice period has completed.
        Arguments.of(0L, 1, listOf(2L), 2L, false),
        Arguments.of(0L, 1, listOf(2L), 2L, true),
        // A holiday today is not part of the full notice period.
        Arguments.of(0L, 2, listOf(0L), 3L, false),
        Arguments.of(0L, 2, listOf(0L), 3L, true),
      )
    }

    @JvmStatic
    fun reviewStates(): Stream<Boolean> = Stream.of(false, true)
  }

  @Autowired
  lateinit var cacheManager: CacheManager

  @MockitoBean
  private lateinit var currentDateUtils: CurrentDateUtils

  private val prisonCode = "MDI"
  private val prisonerId = "AA123456B"
  private val visitorIds = listOf(1L, 2L, 3L)
  private val visitorRestrictionsForReview = VisitorRestrictionsForReview.entries.map { it.name }
  private val monday = LocalDate.now().plusYears(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))

  @BeforeEach
  fun setupMocks() {
    cacheManager.getCache(CacheNames.BANK_HOLIDAYS)?.clear()
    visitSchedulerMockServer.resetAll()
    prisonApiMockServer.resetAll()
    alertApiMockServer.resetAll()
    prisonerContactRegistryMockServer.resetAll()
    govUkMockServer.resetAll()
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, OffenderRestrictionsDto(offenderRestrictions = emptyList()))
    prisonerContactRegistryMockServer.stubDoVisitorsHaveClosedRestrictions(prisonerId, visitorIds = visitorIds, result = false)
  }

  @ParameterizedTest(name = "day offset {0}, minimum {1}, first day offset {2}, review {3}")
  @MethodSource("bookingWindowCases")
  fun `booking window matches the table for both auto approved and review sessions`(dayOffset: Long, minimum: Int, firstDayOffset: Long, forReview: Boolean) {
    assertBookingWindow(monday.plusDays(dayOffset), minimum, monday.plusDays(firstDayOffset), forReview)
  }

  @ParameterizedTest(name = "day offset {0}, minimum {1}, holidays {2}, first day offset {3}, review {4}")
  @MethodSource("bankHolidayCases")
  fun `bank holidays do not count towards the full working day notice period`(dayOffset: Long, minimum: Int, holidayOffsets: List<Long>, firstDayOffset: Long, forReview: Boolean) {
    assertBookingWindow(monday.plusDays(dayOffset), minimum, monday.plusDays(firstDayOffset), forReview, holidayOffsets.map { monday.plusDays(it) })
  }

  @ParameterizedTest
  @MethodSource("reviewStates")
  fun `no sessions are queried when the working day notice period exceeds the maximum booking window`(forReview: Boolean) {
    assertBookingWindow(monday.plusDays(4), 2, monday.plusDays(9), forReview, maximum = 3)
  }

  @ParameterizedTest
  @MethodSource("reviewStates")
  fun `sessions on the maximum booking date remain available`(forReview: Boolean) {
    assertBookingWindow(monday, 1, monday.plusDays(2), forReview, maximum = 2)
  }

  @ParameterizedTest
  @MethodSource("reviewStates")
  fun `a zero day notice period allows visits from tomorrow`(forReview: Boolean) {
    assertBookingWindow(monday.plusDays(4), 0, monday.plusDays(5), forReview)
  }

  @Test
  fun `when GOV UK returns NOT_FOUND then the PUBLIC request fails without querying sessions`() {
    govUkMockServer.stubGetBankHolidays(null, HttpStatus.NOT_FOUND)

    assertPublicRequestFailsWithoutQueryingSessions(HttpStatus.NOT_FOUND)
  }

  @Test
  fun `when GOV UK returns INTERNAL_SERVER_ERROR then the PUBLIC request fails without querying sessions`() {
    govUkMockServer.stubGetBankHolidays(null, HttpStatus.INTERNAL_SERVER_ERROR)

    assertPublicRequestFailsWithoutQueryingSessions(HttpStatus.INTERNAL_SERVER_ERROR)
  }

  @Test
  fun `when GOV UK returns an empty response body then the PUBLIC request fails without querying sessions`() {
    govUkMockServer.stubGetBankHolidays(null, HttpStatus.OK)

    assertPublicRequestFailsWithoutQueryingSessions(HttpStatus.INTERNAL_SERVER_ERROR)
  }

  @Test
  fun `when GOV UK returns no holiday events then the PUBLIC request fails without querying sessions`() {
    govUkMockServer.stubGetBankHolidays(HolidaysDto(HolidayEventByDivisionDto("england-and-wales", null)))

    assertPublicRequestFailsWithoutQueryingSessions(HttpStatus.INTERNAL_SERVER_ERROR)
  }

  private fun assertPublicRequestFailsWithoutQueryingSessions(expectedStatus: HttpStatus) {
    whenever(currentDateUtils.getCurrentDate()).thenReturn(monday)
    val prison = createVisitSchedulerPrisonDto(prisonCode, active = true, maxTotalVisitors = 6, maxAdultVisitors = 3, maxChildVisitors = 3, adultAgeYears = 18, policyNoticeDaysMin = 1, policyNoticeDaysMax = 28, weekStartDay = DayOfWeek.MONDAY, remandVisitLimitPerWeek = 3)
    val dateRange = DateRange(monday.plusDays(2), monday.plusDays(28))
    visitSchedulerMockServer.stubGetPrison(prisonCode, prison)

    // Swallowing the holiday error would reach this valid scheduler response and incorrectly return HTTP 200.
    visitSchedulerMockServer.stubGetAvailableVisitSessions(prison, prisonerId, OPEN, emptyList(), clientType = PUBLIC, dateRange = dateRange)

    callGetAvailableVisitSessionsPublic(
      webTestClient = webTestClient,
      prisonCode = prisonCode,
      prisonerId = prisonerId,
      clientType = PUBLIC,
      authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders,
    ).expectStatus().isEqualTo(expectedStatus)

    visitSchedulerMockServer.verify(0, getRequestedFor(urlPathEqualTo("/visit-sessions/available")))
  }

  private fun assertBookingWindow(today: LocalDate, minimum: Int, firstDay: LocalDate, forReview: Boolean, holidays: List<LocalDate> = emptyList(), maximum: Int = 28) {
    whenever(currentDateUtils.getCurrentDate()).thenReturn(today)

    val prison = createVisitSchedulerPrisonDto(prisonCode, active = true, maxTotalVisitors = 6, maxAdultVisitors = 3, maxChildVisitors = 3, adultAgeYears = 18, policyNoticeDaysMin = minimum, policyNoticeDaysMax = maximum, weekStartDay = DayOfWeek.MONDAY, remandVisitLimitPerWeek = 3)

    visitSchedulerMockServer.stubGetPrison(prisonCode, prison)

    govUkMockServer.stubGetBankHolidays(
      HolidaysDto(HolidayEventByDivisionDto("england-and-wales", holidays.map { HolidayEventDto(title = "Bank holiday", date = it) })),
    )

    val dateRange = DateRange(firstDay, today.plusDays(maximum.toLong()))

    val sessions = listOf(firstDay, firstDay.plusDays(1), dateRange.toDate).distinct().filter { it <= dateRange.toDate }.mapIndexed { index, date ->
      AvailableVisitSessionDto(date, "session$index", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN, visitOrderRestriction = NONE)
    }

    // Only stub if the date is inside our booking window, otherwise the method exits early with no sessions.
    if (firstDay <= dateRange.toDate) {
      visitSchedulerMockServer.stubGetAvailableVisitSessions(prison, prisonerId, OPEN, sessions.reversed(), clientType = PUBLIC, dateRange = dateRange)
      prisonerContactRegistryMockServer.stubGetBannedRestrictionDateRage(prisonerId, visitorIds, dateRange, dateRange)
      prisonApiMockServer.stubGetScheduledEvents(prisonerId, dateRange.fromDate, dateRange.toDate, emptyList())
      val alerts = if (forReview) mutableListOf(createAlertResponseDto(code = PrisonerSupportedAlertCodeType.CC1.name, activeFrom = today)) else mutableListOf()
      alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, alerts)
      prisonerContactRegistryMockServer.stubGetVisitorRestrictionsDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange, emptyList())
    }

    val result = callGetAvailableVisitSessionsPublic(
      webTestClient = webTestClient,
      prisonCode = prisonCode,
      prisonerId = prisonerId,
      visitorIds = visitorIds,
      clientType = PUBLIC,
      authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders,
    ).expectStatus().isOk.expectBody().returnResult()
    val availableSessions = TestObjectMapper.mapper.readValue(result.responseBody, Array<AvailableVisitSessionDto>::class.java)

    if (firstDay <= dateRange.toDate) {
      assertThat(availableSessions.map { it.sessionDate }).containsExactlyElementsOf(sessions.map { it.sessionDate })
      assertThat(availableSessions.map { it.sessionForReview }).containsOnly(forReview)
      verify(visitSchedulerClientSpy).getAvailableVisitSessions(prisonCode, prisonerId, OPEN, dateRange, null, null, PUBLIC)
    } else {
      // Else the date is past the "toDate (end date)", so we expect no sessions to be returned.
      assertThat(availableSessions).isEmpty()
      visitSchedulerMockServer.verify(0, getRequestedFor(urlPathEqualTo("/visit-sessions/available")))
    }
  }
}
