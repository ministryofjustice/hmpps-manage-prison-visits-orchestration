package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

@Schema(description = "Permitted prisoner associated with the booker.")
data class PermittedPrisonerForBookerDto(
  @JsonProperty("prisonerId")
  @Schema(description = "Prisoner Id", example = "A1234AA", required = true)
  @field:NotBlank
  val prisonerId: String,

  @JsonProperty("active")
  @Schema(description = "Active / Inactive permitted prisoner", example = "true", required = true)
  @field:NotNull
  val active: Boolean,

  @Schema(description = "prison code", example = "MDI", required = true)
  val prisonCode: String,

  @JsonProperty("permittedVisitors")
  @Schema(description = "Permitted visitors", required = true)
  @field:Valid
  val permittedVisitors: List<PermittedVisitorsForPermittedPrisonerBookerDto>,
)
