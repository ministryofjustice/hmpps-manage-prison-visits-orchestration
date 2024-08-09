package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api

import io.swagger.v3.oas.annotations.media.Schema
/**
 * Inmate Detail - retrieved from Prison API.
 */
@Schema(description = "Inmate Detail")
data class InmateDetailDto(
  @Schema(required = true, description = "Offender Unique Reference", example = "A1234AA")
  val offenderNo: String,

  @Schema(description = "Category description (from list of assessments)")
  val category: String? = null,

  @Schema(description = "Category code (from list of assessments)")
  val categoryCode: String? = null,
)
