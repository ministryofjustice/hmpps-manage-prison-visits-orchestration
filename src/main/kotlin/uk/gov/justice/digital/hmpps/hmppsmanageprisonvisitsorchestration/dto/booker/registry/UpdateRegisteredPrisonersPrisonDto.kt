package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "Update a booker prisoner's prison code.")
data class UpdateRegisteredPrisonersPrisonDto(
  @param:JsonProperty("prisonId")
  @field:NotBlank
  @param:Schema(description = "Prison Id", example = "MDI", required = true)
  val prisonCode: String,
)
