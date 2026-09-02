package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.sessions

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionConflict

data class SessionConflictDto(
  @param:Schema(description = "Session Conflict", example = "NON_ASSOCIATION", required = true)
  @field:NotNull
  val sessionConflict: SessionConflict,

  @param:Schema(description = "Session Conflict attributes", required = false)
  val additionalAttributes: List<List<AdditionalConflictInfoDto>> = emptyList(),
)
