package uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler

import java.time.LocalDate

data class IndefiniteDateRange(
  var fromDate: LocalDate,
  val toDate: LocalDate?,
) {
  constructor(dateRange: DateRange) : this(fromDate = dateRange.fromDate, toDate = dateRange.toDate)
}
