package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

@Schema(description = "Permitted prisoner associated with the booker.")
data class PermittedPrisonerForBookerDto(
  @param:JsonProperty("prisonerId")
  @param:Schema(description = "Prisoner Id", example = "A1234AA", required = true)
  @field:NotBlank
  val prisonerId: String,

  @param:JsonProperty("active")
  @param:Schema(description = "Active / Inactive permitted prisoner", example = "true", required = true)
  @field:NotNull
  val active: Boolean,

  @param:Schema(description = "prison code", example = "MDI", required = true)
  val prisonCode: String,

  @param:JsonProperty("permittedVisitors")
  @param:Schema(description = "Permitted visitors", required = true)
  @field:Valid
  val permittedVisitors: List<PermittedVisitorsForPermittedPrisonerBookerDto>,
)
