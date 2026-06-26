package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitRequestRejectionReason

data class RejectVisitRequestBodyDto(
  @field:Schema(description = "Reference of the visit for rejection", required = true)
  @field:NotNull
  val visitReference: String,
  @field:Schema(description = "Username for user who actioned this request", required = true)
  @field:NotNull
  val actionedBy: String,
  @field:Schema(description = "Reason for rejecting a visit request", required = false)
  val visitRequestRejectionReason: VisitRequestRejectionReason? = null,
)
