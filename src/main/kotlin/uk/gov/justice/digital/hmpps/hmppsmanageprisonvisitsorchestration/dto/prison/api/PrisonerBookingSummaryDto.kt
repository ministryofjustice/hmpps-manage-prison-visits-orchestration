package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.constraints.NotBlank

@Schema(description = "Prisoner Booking Summary")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PrisonerBookingSummaryDto(
  @Schema(required = true, description = "Prisoner number (e.g. NOMS Number).", example = "A1234AA")
  val offenderNo: @NotBlank String,

  @Schema(
    description = "Convicted Status",
    name = "convictedStatus",
    example = "Convicted",
    allowableValues = ["Convicted", "Remand"]
  )
  val convictedStatus: String? = null,
)
