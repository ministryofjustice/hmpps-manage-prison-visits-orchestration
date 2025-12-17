package uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.prison.api

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Case Load")
class CaseLoadDto(
  @param:Schema(required = true, description = "Case Load ID", example = "MDI")
  val caseLoadId: String,

  @param:Schema(required = true, description = "Full description of the case load", example = "Moorland Closed (HMP & YOI)")
  val description: String,

  @param:Schema(
    required = true,
    description = "Type of case load. Note: Reference Code CSLD_TYPE",
    example = "INST",
    allowableValues = ["COMM", "INST", "APP"],
  )
  val type: String,

  @param:Schema(
    description = "Functional Use of the case load",
    example = "GENERAL",
    allowableValues = ["GENERAL", "ADMIN"],
  )
  val caseloadFunction: String? = null,

  @param:Schema(
    required = true,
    description = "Indicates that this caseload in the context of a staff member is the current active",
    example = "false",
  )
  val currentlyActive: Boolean,
)
