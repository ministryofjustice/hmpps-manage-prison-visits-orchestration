package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto

import com.fasterxml.jackson.annotation.JsonCreator
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerDto
import java.time.LocalDate

data class BookerPrisonerInfoDto(
  private val prisoner: PrisonerDto,

  @Schema(description = "Available VOs - a total of VOs and PVOs", example = "0", required = true)
  val availableVos: Int = 0,

  @Schema(description = "Next available VO date", example = "2024-08-01", required = true)
  val nextAvailableVoDate: LocalDate,
) : PrisonerDto(
  prisonerId = prisoner.prisonerId,
  prisonId = prisoner.prisonId,
  firstName = prisoner.firstName,
  lastName = prisoner.lastName,
  dateOfBirth = prisoner.dateOfBirth,
  prisonName = prisoner.prisonName,
) {
  @JsonCreator
  constructor(
    prisonerId: String,
    prisonId: String?,
    firstName: String,
    lastName: String,
    dateOfBirth: LocalDate,
    cellLocation: String?,
    prisonName: String?,
    availableVos: Int,
    nextAvailableVoDate: LocalDate,
  ) : this(
    prisoner = PrisonerDto(
      prisonerId = prisonerId,
      firstName = firstName,
      lastName = lastName,
      dateOfBirth = dateOfBirth,
      prisonId = prisonId,
      cellLocation = cellLocation,
      prisonName = prisonName,
    ),
    availableVos = availableVos,
    nextAvailableVoDate = nextAvailableVoDate,
  )
}
