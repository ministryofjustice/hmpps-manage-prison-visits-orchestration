package uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Session Capacity")
data class SessionCapacityDto(
  @param:Schema(description = "closed capacity", example = "10", required = true)
  val closed: Int,
  @param:Schema(description = "open capacity", example = "50", required = true)
  val open: Int,
)
