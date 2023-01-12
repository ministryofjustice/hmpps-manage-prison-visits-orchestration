package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Visitor support")
class VisitorSupportDto(
  @Schema(description = "Support type", example = "OTHER", required = true)
  val type: String,
  @Schema(description = "Support text description", example = "visually impaired assistance", required = false)
  val text: String? = null
)
