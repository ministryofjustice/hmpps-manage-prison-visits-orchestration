package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class PrisonerBasicInfoDto(
  @Schema(description = "Prisoner Number", example = "A1234AA", required = true)
  val prisonerNumber: String,

  @Schema(description = "First Name", example = "Robert", required = true)
  val firstName: String,

  @Schema(description = "Last name", example = "Larsen", required = true)
  val lastName: String,

  @Schema(description = "Date of Birth", example = "1975-04-02", required = false)
  val dateOfBirth: LocalDate?,
) {
  constructor(prisonerNumber: String, prisonerDto: PrisonerDto) : this(
    prisonerNumber = prisonerNumber,
    firstName = prisonerDto.firstName,
    lastName = prisonerDto.lastName,
    dateOfBirth = prisonerDto.dateOfBirth,
  )
}
