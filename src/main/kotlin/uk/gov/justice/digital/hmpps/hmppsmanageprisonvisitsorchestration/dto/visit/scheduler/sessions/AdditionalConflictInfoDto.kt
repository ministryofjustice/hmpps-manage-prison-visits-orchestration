package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.sessions

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class AdditionalConflictInfoDto(
  @Schema(description = "Attribute Name", required = true)
  @field:NotBlank
  val attributeName: String,

  @Schema(description = "Attribute value", required = true)
  @field:NotBlank
  val attributeValue: String,
)
