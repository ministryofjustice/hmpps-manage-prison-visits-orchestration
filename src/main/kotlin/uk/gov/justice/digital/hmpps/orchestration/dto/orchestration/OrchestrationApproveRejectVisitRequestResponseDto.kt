package uk.gov.justice.digital.hmpps.orchestration.dto.orchestration

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class OrchestrationApproveRejectVisitRequestResponseDto(
  @field:Schema(description = "Reference of the approved visit", required = true)
  @field:NotBlank
  val visitReference: String,

  @field:Schema(description = "First name of the prisoner being visited", required = true)
  @field:NotBlank
  val prisonerFirstName: String,

  @field:Schema(description = "Last name of the prisoner being visited", required = true)
  @field:NotBlank
  val prisonerLastName: String,
)
