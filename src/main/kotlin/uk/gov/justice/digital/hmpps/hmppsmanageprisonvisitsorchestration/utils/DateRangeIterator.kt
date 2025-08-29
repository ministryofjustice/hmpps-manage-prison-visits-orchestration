package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.utils

import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.DateRange
import java.time.LocalDate

class DateRangeIterator(
  val dateRange: DateRange,
  val stepDays: Long = 1,
) : Iterator<LocalDate> {
  private var currentDate = dateRange.fromDate

  override fun hasNext() = currentDate <= dateRange.toDate

  override fun next(): LocalDate {
    val next = currentDate
    currentDate = currentDate.plusDays(stepDays)
    return next
  }
}
