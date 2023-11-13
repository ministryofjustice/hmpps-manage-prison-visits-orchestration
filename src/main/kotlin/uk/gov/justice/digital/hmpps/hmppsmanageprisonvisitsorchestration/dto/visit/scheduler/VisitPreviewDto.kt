package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

/**
 * A visit's preview with minimum visit details.
 */
data class VisitPreviewDto(
  @Schema(required = true, description = "Prisoner Number", example = "A1234AA")
  @NotNull
  val prisonerId: String,

  @Schema(description = "First name of the prisoner", example = "John", required = true)
  @NotNull
  val firstName: String,

  @Schema(description = "Last name of the prisoner", example = "Smith", required = true)
  @NotNull
  val lastName: String,

  @Schema(description = "Visit reference", example = "dp-we-rs-te", required = true)
  @NotNull
  val visitReference: String,
) {
  constructor(prisonerId: String, visitReference: String) : this(prisonerId, prisonerId, prisonerId, visitReference)
}
