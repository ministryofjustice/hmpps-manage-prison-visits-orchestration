package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.utils

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.DateRange
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSchedulerPrisonDto
import java.time.LocalDate

@Component
class DateUtils {
  fun getToDaysDateRange(
    prison: VisitSchedulerPrisonDto,
    minOverride: Int? = null,
    maxOverride: Int? = null,
  ): DateRange {
    val today = LocalDate.now()

    val min = minOverride ?: prison.policyNoticeDaysMin
    val max = maxOverride ?: prison.policyNoticeDaysMax

    val bookableStartDate = today.plusDays(min.toLong())
    val bookableEndDate = today.plusDays(max.toLong())
    return DateRange(bookableStartDate, bookableEndDate)
  }

  fun advanceFromDate(dateRange: DateRange, advanceFromDateByDays: Int): DateRange {
    // if advanceFromDateByDays is greater than zero and new from date is beofre toDate
    if (advanceFromDateByDays > 0) {
      val fromDate = dateRange.fromDate.plusDays(advanceFromDateByDays.toLong())

      // check if new fromDate is before or equal to toDate
      if (fromDate <= dateRange.toDate) {
        return DateRange(fromDate, dateRange.toDate)
      }
    }

    return dateRange
  }

  fun getCurrentDate(): LocalDate {
    return LocalDate.now()
  }
}
