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
    val advanceFromDateByDays = 1

    // When
    var dateRange = dateUtils.getToDaysDateRange(prison)
    dateRange = dateUtils.advanceFromDate(dateRange, advanceFromDateByDays)

    // Then
    Assertions.assertThat(dateRange.fromDate).isEqualTo(today.plusDays(prison.policyNoticeDaysMin.toLong() + advanceFromDateByDays))
    Assertions.assertThat(dateRange.toDate).isEqualTo(today.plusDays(prison.policyNoticeDaysMax.toLong()))
  }

  @Test
  fun `adds no days if advance date by days is zero`() {
    // Given
    val advanceFromDateByDays = 0

    // When
    var dateRange = dateUtils.getToDaysDateRange(prison)
    dateRange = dateUtils.advanceFromDate(dateRange, advanceFromDateByDays)

    // Then
    Assertions.assertThat(dateRange.fromDate).isEqualTo(today.plusDays(prison.policyNoticeDaysMin.toLong()))
    Assertions.assertThat(dateRange.toDate).isEqualTo(today.plusDays(prison.policyNoticeDaysMax.toLong()))
  }

  @Test
  fun `adds no days if advance date by days is negative value`() {
    // Given
    val advanceFromDateByDays = -1

    // When
    var dateRange = dateUtils.getToDaysDateRange(prison)
    dateRange = dateUtils.advanceFromDate(dateRange, advanceFromDateByDays)

    // Then
    Assertions.assertThat(dateRange.fromDate).isEqualTo(today.plusDays(prison.policyNoticeDaysMin.toLong()))
    Assertions.assertThat(dateRange.toDate).isEqualTo(today.plusDays(prison.policyNoticeDaysMax.toLong()))
  }

  @Test
  fun `adds no days if number of days is more than max policy days`() {
    // Given
    val advanceFromDateByDays = (prison.policyNoticeDaysMax - prison.policyNoticeDaysMin) + 1

    // When
    var dateRange = dateUtils.getToDaysDateRange(prison)
    dateRange = dateUtils.advanceFromDate(dateRange, advanceFromDateByDays)

    // Then
    Assertions.assertThat(dateRange.fromDate).isEqualTo(today.plusDays(prison.policyNoticeDaysMin.toLong()))
    Assertions.assertThat(dateRange.toDate).isEqualTo(today.plusDays(prison.policyNoticeDaysMax.toLong()))
  }

  @Test
  fun `adds days if number of days is same as max policy days`() {
    // Given
    val advanceFromDateByDays = (prison.policyNoticeDaysMax - prison.policyNoticeDaysMin)

    // When
    var dateRange = dateUtils.getToDaysDateRange(prison)
    dateRange = dateUtils.advanceFromDate(dateRange, advanceFromDateByDays)

    // Then
    Assertions.assertThat(dateRange.fromDate).isEqualTo(today.plusDays(prison.policyNoticeDaysMin.toLong() + advanceFromDateByDays))
    Assertions.assertThat(dateRange.toDate).isEqualTo(today.plusDays(prison.policyNoticeDaysMax.toLong()))
    Assertions.assertThat(dateRange.fromDate).isEqualTo(dateRange.toDate)
  }

  @Test
  fun `adds days if number of days is less than max policy days`() {
    // Given
    val advanceFromDateByDays = 25

    // When
    var dateRange = dateUtils.getToDaysDateRange(prison)
    dateRange = dateUtils.advanceFromDate(dateRange, advanceFromDateByDays)

    // Then
    Assertions.assertThat(dateRange.fromDate).isEqualTo(today.plusDays(prison.policyNoticeDaysMin.toLong() + advanceFromDateByDays))
    Assertions.assertThat(dateRange.toDate).isEqualTo(today.plusDays(prison.policyNoticeDaysMax.toLong()))
  }
}
