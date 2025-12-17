package uk.gov.justice.digital.hmpps.orchestration.dto.visit.scheduler

import java.time.LocalDate

data class DateRange(
  var fromDate: LocalDate,
  val toDate: LocalDate,
)
