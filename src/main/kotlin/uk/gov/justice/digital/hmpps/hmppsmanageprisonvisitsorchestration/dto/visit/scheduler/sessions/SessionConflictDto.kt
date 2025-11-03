package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.sessions

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionConflict

data class SessionConflictDto(
  @Schema(description = "Session Conflict", example = "NON_ASSOCIATION", required = true)
  @field:NotNull
  val sessionConflict: SessionConflict,

  @Schema(description = "Session Conflict attributes", required = false)
  val additionalAttributes: List<List<AdditionalSessionConflictInfoDto>> = emptyList(),
)

data class AdditionalSessionConflictInfoDto(
  @Schema(description = "Attribute Name", required = true)
  @field:NotBlank
  val attributeName: String,

  @Schema(description = "Attribute value", required = true)
  @field:NotBlank
  val attributeValue: String,
)
