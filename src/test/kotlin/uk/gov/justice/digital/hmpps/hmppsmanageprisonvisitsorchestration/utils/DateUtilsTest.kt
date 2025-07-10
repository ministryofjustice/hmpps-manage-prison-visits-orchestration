package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.utils

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSchedulerPrisonDto
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class DateUtilsTest {

  private val dateUtils = DateUtils()

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
    Assertions.assertThat(dateRange.fromDate).isEqualTo(today.plusDays(prison.policyNoticeDaysMin.toLong()))
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
    Assertions.assertThat(dateRange.fromDate).isEqualTo(today.plusDays(prison.policyNoticeDaysMin.toLong() + pvbAdvanceFromDateByDays))
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
    Assertions.assertThat(dateRange.fromDate).isEqualTo(today.plusDays(prison.policyNoticeDaysMin.toLong()))
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
    Assertions.assertThat(dateRange.fromDate).isEqualTo(today.plusDays(prison.policyNoticeDaysMin.toLong()))
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
    Assertions.assertThat(dateRange.fromDate).isEqualTo(today.plusDays(prison.policyNoticeDaysMin.toLong()))
    Assertions.assertThat(dateRange.toDate).isEqualTo(today.plusDays(prison.policyNoticeDaysMax.toLong()))
  }

  @Test
  fun `adds days if number of days is same as max policy days`() {
    // Given
    val pvbAdvanceFromDateByDays = (prison.policyNoticeDaysMax - prison.policyNoticeDaysMin)

    // When
    var dateRange = dateUtils.getToDaysDateRange(prison)
    dateRange = dateUtils.advanceFromDate(dateRange, pvbAdvanceFromDateByDays)

    // Then
    Assertions.assertThat(dateRange.fromDate).isEqualTo(today.plusDays(prison.policyNoticeDaysMin.toLong() + pvbAdvanceFromDateByDays))
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
    Assertions.assertThat(dateRange.fromDate).isEqualTo(today.plusDays(prison.policyNoticeDaysMin.toLong() + pvbAdvanceFromDateByDays))
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
    Assertions.assertThat(dateRange.fromDate).isEqualTo(today.plusDays(prison.policyNoticeDaysMin.toLong()))
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
    Assertions.assertThat(dateRange.fromDate).isEqualTo(today.plusDays(prison.policyNoticeDaysMin.toLong()))
    Assertions.assertThat(dateRange.toDate).isEqualTo(today.plusDays(prison.policyNoticeDaysMax.toLong()))
  }
}
