package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

@Schema(description = "Prisoners associated with the booker.")
data class BookerPrisonerVisitorsDto(
  @JsonProperty("prisonerNumber")
  @Schema(description = "Prisoner Number", example = "A1234AA", required = true)
  @NotBlank
  val prisonerNumber: String,

  @JsonProperty("personId")
  @Schema(description = "Identifier for this contact (Person in NOMIS)", example = "5871791", required = true)
  @NotNull
  val personId: Long,
)
