package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "Prisoners associated with the booker.")
data class BookerPrisonersDto(
  @JsonProperty("prisonerNumber")
  @Schema(description = "Prisoner Number", example = "A1234AA", required = true)
  @NotBlank
  val prisonerNumber: String,

  @JsonProperty("prisonCode")
  @Schema(description = "Prison Code", example = "MDI", required = true)
  @NotBlank
  val prisonCode: String,
)
