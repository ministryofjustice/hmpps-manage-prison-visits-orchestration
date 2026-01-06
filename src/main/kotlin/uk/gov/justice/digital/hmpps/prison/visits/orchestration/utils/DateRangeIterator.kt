package uk.gov.justice.digital.hmpps.prison.visits.orchestration.utils

import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.visit.scheduler.DateRange
import java.time.LocalDate

class DateRangeIterator(
  val dateRange: DateRange,
  val stepDays: Long = 1,
) : Iterator<LocalDate> {
  private var date = dateRange.fromDate

  override fun hasNext() = date <= dateRange.toDate

  override fun next(): LocalDate {
    val next = date
    date = date.plusDays(stepDays)
    return next
  }
}
