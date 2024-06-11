package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class PrisonerInfoDto(
  @Schema(description = "Prisoner Number", example = "A1234AA", required = true)
  val prisonerNumber: String,

  @Schema(description = "First Name", example = "Robert", required = true)
  val firstName: String,

  @Schema(description = "Last name", example = "Larsen", required = true)
  val lastName: String,

  @Schema(description = "Prison code", example = "MDI", required = true)
  val prisonCode: String,

  @Schema(description = "Available VOs - a total of VOs and PVOs", example = "0", required = true)
  val availableVos: Int = 0,

  @Schema(description = "Next available VO date", example = "2024-08-01", required = true)
  val nextAvailableVoDate: LocalDate,
) {
  constructor(prisonerNumber: String, prisonerDto: PrisonerDto, availableVos: Int = 0, nextAvailableVoDate: LocalDate) : this(
    prisonerNumber = prisonerNumber,
    firstName = prisonerDto.firstName,
    lastName = prisonerDto.lastName,
    prisonCode = prisonerDto.prisonId!!,
    availableVos = availableVos,
    nextAvailableVoDate = nextAvailableVoDate,
  )
}
