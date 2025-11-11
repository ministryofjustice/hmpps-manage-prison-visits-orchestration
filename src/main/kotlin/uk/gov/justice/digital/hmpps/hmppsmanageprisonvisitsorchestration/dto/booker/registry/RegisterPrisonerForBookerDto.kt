package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import java.time.LocalDate

@Schema(description = "Details to register a prisoner to a booker.")
data class RegisterPrisonerForBookerDto(
  @JsonProperty("prisonerId")
  @param:Schema(description = "Prisoner Id", example = "A1234AA", required = true)
  @field:NotBlank
  val prisonerId: String,

  @param:Schema(description = "Prisoner first name", example = "James", required = true)
  @field:NotBlank
  val prisonerFirstName: String,

  @param:Schema(description = "Prisoner last name", example = "Smith", required = true)
  @field:NotBlank
  val prisonerLastName: String,

  @param:Schema(description = "Prisoner date of birth", example = "1960-01-30", required = true)
  val prisonerDateOfBirth: LocalDate,

  @JsonProperty("prisonId")
  @field:NotBlank
  @param:Schema(description = "Prison Id", example = "MDI", required = true)
  val prisonCode: String,
)
