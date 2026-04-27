package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.sessions

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionDateConflict

data class SessionDateConflictDto(
  @param:Schema(description = "Session Date Conflict", example = "NON_ASSOCIATION", required = true)
  @field:NotNull
  val sessionDateConflict: SessionDateConflict,

  @param:Schema(description = "Session Conflict attributes", required = false)
  val additionalAttributes: List<List<AdditionalConflictInfoDto>> = emptyList(),
)
