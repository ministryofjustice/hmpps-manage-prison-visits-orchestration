package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.enums.LanguagePreference
import java.time.LocalDate

data class AddVisitorToBookerPrisonerRequestDto(
  @param:Schema(name = "firstName", description = "First name of the visitor in request", required = true)
  @field:NotBlank
  val firstName: String,

  @param:Schema(name = "lastName", description = "Last name of the visitor in request", required = true)
  @field:NotBlank
  val lastName: String,

  @param:Schema(name = "dateOfBirth", description = "Date of birth of the visitor in request", required = true)
  val dateOfBirth: LocalDate,

  @param:Schema(name = "languagePreference", description = "The language in which your correspondence will be sent", defaultValue = "EN", required = false)
  val languagePreference: LanguagePreference = LanguagePreference.EN,
)
