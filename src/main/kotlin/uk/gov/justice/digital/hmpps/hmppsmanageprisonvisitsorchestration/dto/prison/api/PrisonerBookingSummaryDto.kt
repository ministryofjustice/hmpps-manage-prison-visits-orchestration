package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Prisoner Booking Summary")
data class PrisonerBookingSummaryDto(
  @Schema(required = true, description = "Prisoner number (e.g. NOMS Number).", example = "A1234AA")
  val offenderNo: String,

  @Schema(
    description = "Convicted Status",
    name = "convictedStatus",
    example = "Convicted",
    allowableValues = ["Convicted", "Remand"]
  )
  val convictedStatus: String? = null,
)
