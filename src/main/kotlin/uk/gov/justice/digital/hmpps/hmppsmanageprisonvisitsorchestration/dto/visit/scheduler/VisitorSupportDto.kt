package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "Visitor support")
data class VisitorSupportDto(
  @Schema(description = "Support text description", example = "visually impaired assistance", required = true)
  @Size(min = 3, max = 512)
  @NotBlank
  val description: String,
)
