package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.utils

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.DateRange
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.IndefiniteDateRange
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.PrisonUserClientDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.PrisonClientType
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class DateUtils(private val currentDateUtils: CurrentDateUtils) {
  fun getToDaysDateRange(
    clientType: PrisonClientType,
    client: PrisonUserClientDto,
    minOverride: Int? = null,
    maxOverride: Int? = null,
  ): DateRange {
    val today = currentDateUtils.getCurrentDate()

    val min = if (minOverride == null || minOverride < client.policyNoticeDaysMin) {
      client.policyNoticeDaysMin
    } else {
      minOverride
    }

    val max = if (maxOverride == null || maxOverride > client.policyNoticeDaysMax) {
      client.policyNoticeDaysMax
    } else {
      maxOverride
    }

    // add 1 to the policyNoticeDaysMin for PUBLIC client - not STAFF - to ensure we are adding whole days if client type requested is PUBLIC
    val bookableStartDate = if (clientType == PrisonClientType.STAFF) {
      today.plusDays(min.toLong())
    } else {
      today.plusDays(min.toLong().plus(1))
    }
    var bookableEndDate = today.plusDays(max.toLong())

    if (bookableEndDate.isBefore(bookableStartDate)) {
      bookableEndDate = bookableStartDate
    }
    return DateRange(bookableStartDate, bookableEndDate)
  }

  fun getPublicBookableDateRange(client: PrisonUserClientDto, bankHolidays: List<LocalDate>): DateRange {
    val today = currentDateUtils.getCurrentDate()
    val minimumWorkingDaysNotice = client.policyNoticeDaysMin
    var workingDaysAdded = 0
    var lastNoticeDay = today

    while (workingDaysAdded < minimumWorkingDaysNotice) {
      // Move to the next calendar day. Today cannot count as a full day's notice.
      lastNoticeDay = lastNoticeDay.plusDays(1)

      val fallsOnWeekend = isWeekend(lastNoticeDay)
      val fallsOnBankHoliday = isBankHoliday(lastNoticeDay, bankHolidays)

      if (!fallsOnWeekend && !fallsOnBankHoliday) {
        workingDaysAdded++
      }
    }

    // Visits can start on the calendar day after the full working-day notice period, including weekends and holidays.
    val bookableStartDate = lastNoticeDay.plusDays(1)
    val bookableEndDate = today.plusDays(client.policyNoticeDaysMax.toLong())

    return DateRange(
      fromDate = bookableStartDate,
      toDate = bookableEndDate,
    )
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

  fun now(): LocalDateTime = LocalDateTime.now()

  fun today(): LocalDate = currentDateUtils.getCurrentDate()

  private fun isWeekend(dateToBeChecked: LocalDate): Boolean = ((dateToBeChecked.dayOfWeek == SATURDAY || dateToBeChecked.dayOfWeek == SUNDAY))

  private fun isBankHoliday(dateToBeChecked: LocalDate, holidays: List<LocalDate>): Boolean = holidays.contains(dateToBeChecked)
}
