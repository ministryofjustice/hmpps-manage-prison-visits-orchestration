package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Session Capacity")
data class SessionCapacityDto(
  @Schema(description = "closed capacity", example = "10", required = true)
  val closed: Int,
  @Schema(description = "open capacity", example = "50", required = true)
  val open: Int
)
