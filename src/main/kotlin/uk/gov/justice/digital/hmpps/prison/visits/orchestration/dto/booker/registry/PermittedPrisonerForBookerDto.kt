package uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.booker.registry

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank

@Schema(description = "Permitted prisoner associated with the booker.")
data class PermittedPrisonerForBookerDto(
  @param:JsonProperty("prisonerId")
  @param:Schema(description = "Prisoner Id", example = "A1234AA", required = true)
  @field:NotBlank
  val prisonerId: String,

  @param:Schema(description = "prison code", example = "MDI", required = true)
  val prisonCode: String,

  @param:JsonProperty("permittedVisitors")
  @param:Schema(description = "Permitted visitors", required = true)
  @field:Valid
  val permittedVisitors: List<PermittedVisitorsForPermittedPrisonerBookerDto>,
)
