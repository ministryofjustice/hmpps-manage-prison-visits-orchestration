package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.govuk.holidays

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class HolidayEventDto(
  @param:Schema(required = true, description = "Holiday title", example = "New Year’s Day")
  val title: String?,

  @param:Schema(required = true, description = "Holiday date", example = "2026-01-01")
  val date: LocalDate,
)
