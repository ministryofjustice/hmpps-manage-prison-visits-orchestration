package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.constraints.NotBlank

@Schema(description = "Case Load")
@JsonInclude(JsonInclude.Include.NON_NULL)
class CaseLoadDto(
  @Schema(required = true, description = "Case Load ID", example = "MDI")
  @JsonProperty("caseLoadId")
  val caseLoadId: @NotBlank String? = null,

  @Schema(required = true, description = "Full description of the case load", example = "Moorland Closed (HMP & YOI)")
  @JsonProperty("description")
  val description: @NotBlank String? = null,

  @Schema(
    required = true,
    description = "Type of case load. Note: Reference Code CSLD_TYPE",
    example = "INST",
    allowableValues = ["COMM", "INST", "APP"]
  )
  @JsonProperty("type")
  val type: @NotBlank String? = null,

  @Schema(
    description = "Functional Use of the case load",
    example = "GENERAL",
    allowableValues = ["GENERAL", "ADMIN"]
  )
  @JsonProperty("caseloadFunction")
  val caseloadFunction: String? = null,

  @Schema(
    required = true,
    description = "Indicates that this caseload in the context of a staff member is the current active",
    example = "false"
  )
  @JsonProperty("currentlyActive")
  val currentlyActive: @NotBlank Boolean = false
)
