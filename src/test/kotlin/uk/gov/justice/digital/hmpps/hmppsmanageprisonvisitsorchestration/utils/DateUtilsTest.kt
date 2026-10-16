package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.utils

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.DateRange
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.IndefiniteDateRange
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.PrisonUserClientDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.PrisonClientType
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class DateUtilsTest {
  private val currentDateUtils = CurrentDateUtils()
  private val dateUtils = DateUtils(currentDateUtils)

  private val today = LocalDate.now()

  private val prisonClient = PrisonUserClientDto(
    active = true,
    policyNoticeDaysMin = 2,
    policyNoticeDaysMax = 28,
    clientType = PrisonClientType.STAFF,
  )

  @Test
  fun `works out date range correctly with given prison client`() {
    // When
    val dateRange = dateUtils.getToDaysDateRange(PrisonClientType.STAFF, prisonClient)

    // Then
    Assertions.assertThat(dateRange.fromDate).isEqualTo(today.plusDays(prisonClient.policyNoticeDaysMin.toLong()))
    Assertions.assertThat(dateRange.toDate).isEqualTo(today.plusDays(prisonClient.policyNoticeDaysMax.toLong()))
  }

  @Test
  fun `when min override passed is less than the prison allows the prison days configured needs to be considered`() {
    // Given
    val minOverrideDays = 1

    // When
    // minOverride is less than allowed prison configuration - ignore this parameter
    val dateRange = dateUtils.getToDaysDateRange(PrisonClientType.STAFF, prisonClient, minOverride = minOverrideDays)

    // Then
    Assertions.assertThat(dateRange.fromDate).isEqualTo(today.plusDays(prisonClient.policyNoticeDaysMin.toLong()))
    Assertions.assertThat(dateRange.toDate).isEqualTo(today.plusDays(prisonClient.policyNoticeDaysMax.toLong()))
  }

  @Test
  fun `when max override passed is more than the prison allows the prison days configured needs to be considered`() {
    // Given
    // maxOverride is more than allowed prison configuration - ignore this parameter
    val maxOverrideDays = 56

    // When
    val dateRange = dateUtils.getToDaysDateRange(PrisonClientType.STAFF, prisonClient, maxOverride = maxOverrideDays)

    // Then
    Assertions.assertThat(dateRange.fromDate).isEqualTo(today.plusDays(prisonClient.policyNoticeDaysMin.toLong()))
    Assertions.assertThat(dateRange.toDate).isEqualTo(today.plusDays(prisonClient.policyNoticeDaysMax.toLong()))
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
}
