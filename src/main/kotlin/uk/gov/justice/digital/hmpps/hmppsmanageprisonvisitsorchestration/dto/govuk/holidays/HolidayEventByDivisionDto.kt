package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.govuk.holidays

import io.swagger.v3.oas.annotations.media.Schema

data class HolidayEventByDivisionDto(
  @param:Schema(description = "division", example = "england-and-wales", required = false)
  val division: String?,

  @param:Schema(description = "holiday events", required = true)
  val events: List<HolidayEventDto>?,
)
