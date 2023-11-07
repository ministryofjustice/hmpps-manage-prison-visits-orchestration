package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.register

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Prison Information")
data class PrisonNameDto(
  @Schema(description = "Prison ID", example = "MDI", required = true)
  val prisonId: String,
  @Schema(description = "Name of the prison", example = "Moorland HMP", required = true)
  val prisonName: String,
)
