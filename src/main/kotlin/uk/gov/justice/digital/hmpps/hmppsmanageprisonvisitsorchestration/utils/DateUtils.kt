package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.utils

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.DateRange
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.IndefiniteDateRange
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSchedulerPrisonDto
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.LocalDate

@Component
class DateUtils(private val currentDateUtils: CurrentDateUtils) {
  fun getToDaysDateRange(
    prison: VisitSchedulerPrisonDto,
    minOverride: Int? = null,
    maxOverride: Int? = null,
  ): DateRange {
    val today = currentDateUtils.getCurrentDate()

    val min = if (minOverride == null || minOverride < prison.policyNoticeDaysMin) {
      prison.policyNoticeDaysMin
    } else {
      minOverride
    }

    val max = if (maxOverride == null || maxOverride > prison.policyNoticeDaysMax) {
      prison.policyNoticeDaysMax
    } else {
      maxOverride
    }

    val bookableStartDate = today.plusDays(min.toLong())
    val bookableEndDate = today.plusDays(max.toLong())
    return DateRange(bookableStartDate, bookableEndDate)
  }

  fun advanceFromDate(dateRange: DateRange, pvbAdvanceFromDateByDays: Int): DateRange {
    // if pvbAdvanceFromDateByDays is greater than zero and new from date is before toDate
    if (pvbAdvanceFromDateByDays > 0) {
      val fromDate = dateRange.fromDate.plusDays(pvbAdvanceFromDateByDays.toLong())

      // check if new fromDate is before or equal to toDate
      if (fromDate <= dateRange.toDate) {
        return DateRange(fromDate, dateRange.toDate)
      }
    }

    return dateRange
  }

  fun isDateBetweenDateRanges(dateRanges: List<DateRange>, date: LocalDate): Boolean = dateRanges.any { dateRange ->
    !(date.isBefore(dateRange.fromDate) || date.isAfter(dateRange.toDate))
  }

  fun getUniqueDateRanges(
    dateRanges: List<IndefiniteDateRange>,
    dateRangeToCheckAgainst: DateRange,
  ): List<DateRange> {
    val uniqueDateRanges = dateRanges.filter { dateRange ->
      // consider only null expiry dates or restriction to date not before from date
      (dateRange.toDate == null || (!dateRange.toDate.isBefore(dateRangeToCheckAgainst.fromDate)))
        // also ignore any date ranges that start after the checked date range to date
        .and(!dateRange.fromDate.isAfter(dateRangeToCheckAgainst.toDate))
    }.map { restrictionDateRange ->
      // if restriction start date is after dateRange fromDate use restriction start date else date range start date
      val fromDate = if (restrictionDateRange.fromDate.isAfter(dateRangeToCheckAgainst.fromDate)) restrictionDateRange.fromDate else dateRangeToCheckAgainst.fromDate
      // if restriction end date is null or after date range end date use date range end date else use restriction end date
      val toDate = if (restrictionDateRange.toDate == null || restrictionDateRange.toDate.isAfter(dateRangeToCheckAgainst.toDate)) dateRangeToCheckAgainst.toDate else restrictionDateRange.toDate

      DateRange(
        fromDate = fromDate,
        toDate = toDate,
      )
    }

    return uniqueDateRanges.distinct()
  }

  fun advanceDaysIfWeekendOrBankHoliday(fromDate: LocalDate, toDate: LocalDate, bankHolidays: List<LocalDate>): LocalDate {
    var newFromDate = fromDate
    while ((isWeekend(newFromDate) || isBankHoliday(newFromDate, bankHolidays)) && newFromDate < toDate) {
      newFromDate = newFromDate.plusDays(1)
    }

    return newFromDate
  }

  private fun isWeekend(dateToBeChecked: LocalDate): Boolean = ((dateToBeChecked.dayOfWeek == SATURDAY || dateToBeChecked.dayOfWeek == SUNDAY))

  private fun isBankHoliday(dateToBeChecked: LocalDate, holidays: List<LocalDate>): Boolean = holidays.contains(dateToBeChecked)
}
