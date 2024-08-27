package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionRestriction

@Schema(description = "Visit Session restriction type")
data class AvailableVisitSessionRestrictionDto(
  @Schema(description = "Session Restriction", example = "OPEN", required = true)
  @field:NotNull
  val sessionRestriction: SessionRestriction,
)
