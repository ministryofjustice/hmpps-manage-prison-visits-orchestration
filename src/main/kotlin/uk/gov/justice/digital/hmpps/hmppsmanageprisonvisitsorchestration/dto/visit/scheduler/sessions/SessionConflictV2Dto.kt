package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.sessions

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionConflictV2

data class SessionConflictV2Dto(
  @param:Schema(description = "Session Conflict", example = "DOUBLE_BOOKING_OR_RESERVATION", required = true)
  @field:NotNull
  val sessionConflict: SessionConflictV2,

  @param:Schema(description = "Session Conflict attributes", required = false)
  val additionalAttributes: List<List<AdditionalConflictInfoDto>> = emptyList(),
)
