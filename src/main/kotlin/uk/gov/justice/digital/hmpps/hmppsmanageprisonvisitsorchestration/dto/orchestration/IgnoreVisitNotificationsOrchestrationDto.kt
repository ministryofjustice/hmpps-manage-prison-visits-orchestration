package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class IgnoreVisitNotificationsOrchestrationDto(
  @Schema(description = "Reason why the visit's notifications can be ignored", required = true)
  @field:NotBlank
  val reason: String,
)
