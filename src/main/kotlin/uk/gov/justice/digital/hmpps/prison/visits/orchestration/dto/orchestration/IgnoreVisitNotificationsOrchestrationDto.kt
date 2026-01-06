package uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.orchestration

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class IgnoreVisitNotificationsOrchestrationDto(
  @param:Schema(description = "Reason why the visit's notifications can be ignored", required = true)
  @field:NotBlank
  val reason: String,

  @param:Schema(description = "Username for user who actioned this request", required = true)
  @field:NotBlank
  val actionedBy: String,
)
