package uk.gov.justice.digital.hmpps.visits.orchestration.utils

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.DateRange
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.IndefiniteDateRange
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.VisitSchedulerPrisonDto
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@ExtendWith(MockitoExtension::class)
class DateUtilsTest {
  private val currentDateUtils = CurrentDateUtils()
  private val dateUtils = DateUtils(currentDateUtils)

  private val today = LocalDate.now()

  private val prison = VisitSchedulerPrisonDto(
    code = "HEI",
    active = true,
    policyNoticeDaysMin = 2,
    policyNoticeDaysMax = 28,
    maxTotalVisitors = 6,
    maxAdultVisitors = 3,
    maxChildVisitors = 3,
    adultAgeYears = 18,
  )

  @Test
  fun `works out date range correctly with given prison object`() {
    // When
    val dateRange = dateUtils.getToDaysDateRange(prison)

    // Then
    Assertions.assertThat(dateRange.fromDate).isEqualTo(today.plusDays(prison.policyNoticeDaysMin.toLong().plus(1)))
    Assertions.assertThat(dateRange.toDate).isEqualTo(today.plusDays(prison.policyNoticeDaysMax.toLong()))
  }

  @Test
  fun `adds 1 day to from date if advance date by days is 1`() {
    // Given
    val pvbAdvanceFromDateByDays = 1

    // When
    var dateRange = dateUtils.getToDaysDateRange(prison)
    dateRange = dateUtils.advanceFromDate(dateRange, pvbAdvanceFromDateByDays)

    // Then
    Assertions.assertThat(dateRange.fromDate).isEqualTo(today.plusDays(prison.policyNoticeDaysMin.toLong().plus(1) + pvbAdvanceFromDateByDays))
    Assertions.assertThat(dateRange.toDate).isEqualTo(today.plusDays(prison.policyNoticeDaysMax.toLong()))
  }

  @Test
  fun `adds no days if advance date by days is zero`() {
    // Given
    val pvbAdvanceFromDateByDays = 0

    // When
    var dateRange = dateUtils.getToDaysDateRange(prison)
    dateRange = dateUtils.advanceFromDate(dateRange, pvbAdvanceFromDateByDays)

    // Then
    Assertions.assertThat(dateRange.fromDate).isEqualTo(today.plusDays(prison.policyNoticeDaysMin.toLong().plus(1)))
    Assertions.assertThat(dateRange.toDate).isEqualTo(today.plusDays(prison.policyNoticeDaysMax.toLong()))
  }

  @Test
  fun `adds no days if advance date by days is negative value`() {
    // Given
    val pvbAdvanceFromDateByDays = -1

    // When
    var dateRange = dateUtils.getToDaysDateRange(prison)
    dateRange = dateUtils.advanceFromDate(dateRange, pvbAdvanceFromDateByDays)

    // Then
    Assertions.assertThat(dateRange.fromDate).isEqualTo(today.plusDays(prison.policyNoticeDaysMin.toLong().plus(1)))
    Assertions.assertThat(dateRange.toDate).isEqualTo(today.plusDays(prison.policyNoticeDaysMax.toLong()))
  }

  @Test
  fun `adds no days if number of days is more than max policy days`() {
    // Given
    val pvbAdvanceFromDateByDays = (prison.policyNoticeDaysMax - prison.policyNoticeDaysMin) + 1

    // When
    var dateRange = dateUtils.getToDaysDateRange(prison)
    dateRange = dateUtils.advanceFromDate(dateRange, pvbAdvanceFromDateByDays)

    // Then
    Assertions.assertThat(dateRange.fromDate).isEqualTo(today.plusDays(prison.policyNoticeDaysMin.toLong().plus(1)))
    Assertions.assertThat(dateRange.toDate).isEqualTo(today.plusDays(prison.policyNoticeDaysMax.toLong()))
  }

  @Test
  fun `adds days if number of days is same as max policy days`() {
    // Given
    val pvbAdvanceFromDateByDays = (prison.policyNoticeDaysMax - (prison.policyNoticeDaysMin + 1))

    // When
    var dateRange = dateUtils.getToDaysDateRange(prison)
    dateRange = dateUtils.advanceFromDate(dateRange, pvbAdvanceFromDateByDays)

    // Then
    Assertions.assertThat(dateRange.fromDate).isEqualTo(today.plusDays(prison.policyNoticeDaysMin.toLong().plus(1) + pvbAdvanceFromDateByDays))
    Assertions.assertThat(dateRange.toDate).isEqualTo(today.plusDays(prison.policyNoticeDaysMax.toLong()))
    Assertions.assertThat(dateRange.fromDate).isEqualTo(dateRange.toDate)
  }

  @Test
  fun `adds days if number of days is less than max policy days`() {
    // Given
    val pvbAdvanceFromDateByDays = 25

    // When
    var dateRange = dateUtils.getToDaysDateRange(prison)
    dateRange = dateUtils.advanceFromDate(dateRange, pvbAdvanceFromDateByDays)

    // Then
    Assertions.assertThat(dateRange.fromDate).isEqualTo(today.plusDays(prison.policyNoticeDaysMin.toLong().plus(1) + pvbAdvanceFromDateByDays))
    Assertions.assertThat(dateRange.toDate).isEqualTo(today.plusDays(prison.policyNoticeDaysMax.toLong()))
  }

  @Test
  fun `when min override passed is less than the prison allows the prison days configured needs to be considered`() {
    // Given
    val minOverrideDays = 1

    // When
    // minOverride is less than allowed prison configuration - ignore this parameter
    val dateRange = dateUtils.getToDaysDateRange(prison, minOverride = minOverrideDays)

    // Then
    Assertions.assertThat(dateRange.fromDate).isEqualTo(today.plusDays(prison.policyNoticeDaysMin.toLong().plus(1)))
    Assertions.assertThat(dateRange.toDate).isEqualTo(today.plusDays(prison.policyNoticeDaysMax.toLong()))
  }

  @Test
  fun `when max override passed is more than the prison allows the prison days configured needs to be considered`() {
    // Given
    // maxOverride is more than allowed prison configuration - ignore this parameter
    val maxOverrideDays = 56

    // When
    val dateRange = dateUtils.getToDaysDateRange(prison, maxOverride = maxOverrideDays)

    // Then
    Assertions.assertThat(dateRange.fromDate).isEqualTo(today.plusDays(prison.policyNoticeDaysMin.toLong().plus(1)))
    Assertions.assertThat(dateRange.toDate).isEqualTo(today.plusDays(prison.policyNoticeDaysMax.toLong()))
  }

  @Test
  fun `test getUniqueDateRanges with different IndefiniteDateRanges`() {
    val dateRangeStartDate = LocalDate.now()
    val dateRangeEndDate = dateRangeStartDate.plusDays(28)

    // falls between the date range with toDate as null
    val restriction1DateRange = IndefiniteDateRange(fromDate = dateRangeStartDate.plusDays(5), toDate = null)
    // falls between the date range with toDate as null
    val restriction2DateRange = IndefiniteDateRange(fromDate = dateRangeStartDate.plusDays(15), toDate = null)
    // falls before the date range - should be ignored
    val restriction3DateRange =
      IndefiniteDateRange(fromDate = dateRangeStartDate.minusDays(3), toDate = dateRangeStartDate.minusDays(1))
    // falls between the date range
    val restriction4DateRange =
      IndefiniteDateRange(fromDate = dateRangeStartDate.minusDays(4), toDate = dateRangeStartDate.plusDays(8))
    // falls between the date range
    val restriction5DateRange = IndefiniteDateRange(fromDate = dateRangeEndDate, toDate = null)
    // falls after the date range - should be ignored
    val restriction6DateRange = IndefiniteDateRange(fromDate = dateRangeEndDate.plusDays(2), toDate = null)
    // starts after from date and ends after to date
    val restriction7DateRange =
      IndefiniteDateRange(fromDate = dateRangeStartDate.plusDays(7), toDate = dateRangeEndDate.plusDays(5))
    // starts before from date and ends before to date
    val restriction8DateRange =
      IndefiniteDateRange(fromDate = dateRangeStartDate.minusDays(5), toDate = dateRangeEndDate.minusDays(5))
    // should be ignored as same as restriction7DateRange but with toDate as null
    val restriction9DateRange = IndefiniteDateRange(fromDate = dateRangeStartDate.plusDays(7), toDate = null)

    val dateRanges: List<IndefiniteDateRange> = listOf(
      restriction1DateRange,
      restriction2DateRange,
      restriction3DateRange,
      restriction4DateRange,
      restriction5DateRange,
      restriction6DateRange,
      restriction7DateRange,
      restriction8DateRange,
      restriction9DateRange,
    )

    val dateRangeToTest = DateRange(dateRangeStartDate, dateRangeEndDate)
    val actualDateRanges = dateUtils.getUniqueDateRanges(dateRanges, dateRangeToTest)
    Assertions.assertThat(actualDateRanges.size).isEqualTo(6)
    Assertions.assertThat(actualDateRanges[0].fromDate).isEqualTo(restriction1DateRange.fromDate)
    Assertions.assertThat(actualDateRanges[0].toDate).isEqualTo(dateRangeEndDate)
    Assertions.assertThat(actualDateRanges[1].fromDate).isEqualTo(restriction2DateRange.fromDate)
    Assertions.assertThat(actualDateRanges[1].toDate).isEqualTo(dateRangeEndDate)
    Assertions.assertThat(actualDateRanges[2].fromDate).isEqualTo(dateRangeToTest.fromDate)
    Assertions.assertThat(actualDateRanges[2].toDate).isEqualTo(restriction4DateRange.toDate)
    Assertions.assertThat(actualDateRanges[3].fromDate).isEqualTo(restriction5DateRange.fromDate)
    Assertions.assertThat(actualDateRanges[3].toDate).isEqualTo(dateRangeToTest.toDate)
    Assertions.assertThat(actualDateRanges[4].fromDate).isEqualTo(restriction7DateRange.fromDate)
    Assertions.assertThat(actualDateRanges[4].toDate).isEqualTo(dateRangeToTest.toDate)
    Assertions.assertThat(actualDateRanges[5].fromDate).isEqualTo(dateRangeToTest.fromDate)
    Assertions.assertThat(actualDateRanges[5].toDate).isEqualTo(restriction8DateRange.toDate)
  }

  @Test
  fun `test isDateBetweenDateRanges with different date ranges - check if between`() {
    val dateToBeChecked = LocalDate.now()
    val dateRanges: List<DateRange> = listOf(
      // dateToBeChecked falls between this date range
      DateRange(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)),
      DateRange(LocalDate.now().minusDays(12), LocalDate.now().plusDays(6)),
    )

    Assertions.assertThat(dateUtils.isDateBetweenDateRanges(dateRanges, dateToBeChecked)).isTrue
  }

  @Test
  fun `test isDateBetweenDateRanges with different date ranges - check if on start date`() {
    val dateToBeChecked = LocalDate.now()
    val dateRanges: List<DateRange> = listOf(
      // dateToBeChecked starts from this date range
      DateRange(LocalDate.now(), LocalDate.now().plusDays(12)),
    )

    Assertions.assertThat(dateUtils.isDateBetweenDateRanges(dateRanges, dateToBeChecked)).isTrue
  }

  @Test
  fun `test isDateBetweenDateRanges with different date ranges - check if on end date`() {
    val dateToBeChecked = LocalDate.now().plusDays(12)
    val dateRanges: List<DateRange> = listOf(
      // dateToBeChecked is same as date range end date
      DateRange(LocalDate.now(), LocalDate.now().plusDays(12)),
    )

    Assertions.assertThat(dateUtils.isDateBetweenDateRanges(dateRanges, dateToBeChecked)).isTrue
  }

  @Test
  fun `test isDateBetweenDateRanges with different date ranges - check if before start date`() {
    // date before date range start date
    val dateToBeChecked = LocalDate.now().minusDays(12)
    val dateRanges: List<DateRange> = listOf(
      DateRange(LocalDate.now(), LocalDate.now().plusDays(12)),
    )

    Assertions.assertThat(dateUtils.isDateBetweenDateRanges(dateRanges, dateToBeChecked)).isFalse
  }

  @Test
  fun `test isDateBetweenDateRanges with different date ranges - check if after end date`() {
    // date after date range end date
    val dateToBeChecked = LocalDate.now().plusDays(12)
    val dateRanges: List<DateRange> = listOf(
      DateRange(LocalDate.now(), LocalDate.now().plusDays(11)),
    )

    Assertions.assertThat(dateUtils.isDateBetweenDateRanges(dateRanges, dateToBeChecked)).isFalse
  }

  @Test
  fun `test isDateBetweenDateRanges with different date ranges - check if date ranges is empty`() {
    // date ranges are empty
    val dateToBeChecked = LocalDate.now().plusDays(12)
    val dateRanges: List<DateRange> = emptyList()

    Assertions.assertThat(dateUtils.isDateBetweenDateRanges(dateRanges, dateToBeChecked)).isFalse
  }

  @Test
  fun `when advanceDaysIfWeekendOrBankHoliday called with fromDate falling on Saturday then new fromDate is set to the next Monday`() {
    // date ranges are empty
    val fromDate = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.SATURDAY))
    val toDate = fromDate.plusWeeks(1)

    val newFromDate = dateUtils.advanceDaysIfWeekendOrBankHoliday(fromDate, toDate, emptyList())

    val expectedFromDate = fromDate.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
    Assertions.assertThat(newFromDate).isEqualTo(expectedFromDate)
  }

  @Test
  fun `when advanceDaysIfWeekendOrBankHoliday called with fromDate falling on Sunday then new fromDate is set to the next Monday`() {
    val fromDate = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.SUNDAY))
    val toDate = fromDate.plusWeeks(1)

    val newFromDate = dateUtils.advanceDaysIfWeekendOrBankHoliday(fromDate, toDate, emptyList())

    val expectedFromDate = fromDate.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
    Assertions.assertThat(newFromDate).isEqualTo(expectedFromDate)
  }

  @Test
  fun `when advanceDaysIfWeekendOrBankHoliday called with fromDate falling on weekday then new fromDate is same as fromDate`() {
    val fromDate = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY))
    val toDate = fromDate.plusWeeks(1)

    val newFromDate = dateUtils.advanceDaysIfWeekendOrBankHoliday(fromDate, toDate, emptyList())

    Assertions.assertThat(newFromDate).isEqualTo(fromDate)
  }

  @Test
  fun `when advanceDaysIfWeekendOrBankHoliday called with list of holidays then the holiday dates are skipped`() {
    val fromDate = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY))
    val toDate = fromDate.plusWeeks(1)

    // fromDate and fromDate + 1 are holidays
    val holidays = listOf(fromDate, fromDate.plusDays(1))
    val newFromDate = dateUtils.advanceDaysIfWeekendOrBankHoliday(fromDate, toDate, holidays)

    val expectedFromDate = fromDate.plusDays(2)
    Assertions.assertThat(newFromDate).isEqualTo(expectedFromDate)
  }

  @Test
  fun `when advanceDaysIfWeekendOrBankHoliday called then new fromDate does not exceed toDate`() {
    val fromDate = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.SATURDAY))

    // toDate falls on a Sunday
    val toDate = fromDate.plusDays(1)

    val holidays = emptyList<LocalDate>()
    val newFromDate = dateUtils.advanceDaysIfWeekendOrBankHoliday(fromDate, toDate, holidays)

    // ensure that the fromDate does not go over toDate
    val expectedFromDate = toDate
    Assertions.assertThat(newFromDate).isEqualTo(expectedFromDate)
  }
}
