package uk.gov.justice.digital.hmpps.visits.orchestration.dto.govuk.holidays

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class HolidayEventDto(
  @param:Schema(description = "Holiday title", example = "New Year’s Day", required = false)
  val title: String?,

  @param:Schema(description = "Holiday date", example = "2026-01-01", required = true)
  val date: LocalDate,
)
