package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.sessions

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionConflictAttribute

data class AdditionalConflictInfoDto(
  @param:Schema(description = "Attribute Name", required = true)
  @field:NotNull
  val attributeName: SessionConflictAttribute,

  @param:Schema(description = "Attribute value", required = true)
  @field:NotBlank
  val attributeValue: String,
)
