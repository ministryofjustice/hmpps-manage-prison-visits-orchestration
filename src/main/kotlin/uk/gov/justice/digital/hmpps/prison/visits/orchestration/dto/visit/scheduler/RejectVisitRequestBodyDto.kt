package uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.visit.scheduler

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

data class RejectVisitRequestBodyDto(
  @field:Schema(description = "Reference of the visit for rejection", required = true)
  @field:NotNull
  val visitReference: String,
  @field:Schema(description = "Username for user who actioned this request", required = true)
  @field:NotNull
  val actionedBy: String,
)
