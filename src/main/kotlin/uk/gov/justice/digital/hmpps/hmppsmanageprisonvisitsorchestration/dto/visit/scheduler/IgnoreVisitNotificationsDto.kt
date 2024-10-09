package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.IgnoreVisitNotificationsOrchestrationDto

data class IgnoreVisitNotificationsDto(
  @Schema(description = "Reason why the visit's notifications can be ignored", required = true)
  @field:NotBlank
  val reason: String,

  @Schema(description = "Username for user who actioned this request", required = true)
  @field:NotBlank
  val actionedBy: String,
) {
  constructor(ignoreVisitNotificationsOrchestrationDto: IgnoreVisitNotificationsOrchestrationDto) : this (
    reason = ignoreVisitNotificationsOrchestrationDto.reason,
    actionedBy = ignoreVisitNotificationsOrchestrationDto.actionedBy,
  )
}
