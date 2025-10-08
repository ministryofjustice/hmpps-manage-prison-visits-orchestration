package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.admin

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.RegisteredPrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.PrisonerContactDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerDto

data class BookerPrisonerDetailedInfoDto(
  @Schema(description = "Prisoner Details", required = true)
  val prisoner: PrisonerDto,

  @Schema(description = "Current prison code for the prison that the booker registered the prisoner with", required = true)
  val registeredPrison: RegisteredPrisonDto,

  @Schema(description = "Permitted visitors list", required = true)
  @field:Valid
  val permittedVisitors: List<BookerPrisonerVisitorDetailedInfoDto>,
) {
  constructor(prisoner: PrisonerDto, registeredPrisonDto: RegisteredPrisonDto, visitors: List<PrisonerContactDto>) : this(
    prisoner = prisoner,
    registeredPrison = registeredPrisonDto,
    permittedVisitors = visitors.map { BookerPrisonerVisitorDetailedInfoDto(it) }.toList(),
  )
}
