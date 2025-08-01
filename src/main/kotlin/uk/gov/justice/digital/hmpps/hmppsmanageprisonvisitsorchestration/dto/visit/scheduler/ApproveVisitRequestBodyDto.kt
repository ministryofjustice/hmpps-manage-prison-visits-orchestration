package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

data class ApproveVisitRequestBodyDto(
  @field:Schema(description = "Reference of the visit for approval", required = true)
  @field:NotNull
  val visitReference: String,
  @field:Schema(description = "Username for user who actioned this request", required = true)
  @field:NotNull
  val actionedBy: String,
)
