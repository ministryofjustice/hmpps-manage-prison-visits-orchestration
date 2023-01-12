package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto

import io.swagger.v3.oas.annotations.media.Schema

data class VisitCancelDto(
  @Schema(description = "Visit Reference", example = "v9-d7-ed-7u", required = true)
  val reference: String,

  @Schema(description = "Outcome details - includes status and description.", required = true)
  val outcome: OutcomeDto
)
