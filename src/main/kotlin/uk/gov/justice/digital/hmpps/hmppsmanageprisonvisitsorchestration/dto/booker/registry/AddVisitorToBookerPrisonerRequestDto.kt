package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import java.time.LocalDate

data class AddVisitorToBookerPrisonerRequestDto(
  @param:Schema(name = "visitors first name", description = "First name of the visitor in request", required = true)
  @field:NotBlank
  val firstName: String,

  @param:Schema(name = "visitors last name", description = "Last name of the visitor in request", required = true)
  @field:NotBlank
  val lastName: String,

  @param:Schema(name = "visitors date of birth", description = "Date of birth of the visitor in request", required = true)
  val dateOfBirth: LocalDate,
)
