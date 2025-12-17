package uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.booker.registry.admin

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.booker.registry.BookerPrisonerInfoDto
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.booker.registry.RegisteredPrisonDto
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.contact.registry.PrisonerContactDto
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.prisoner.search.PrisonerDto

data class BookerPrisonerDetailedInfoDto(
  @param:Schema(description = "Prisoner Details", required = true)
  val prisoner: PrisonerDto,

  @param:Schema(description = "Current prison code for the prison that the booker registered the prisoner with", required = true)
  val registeredPrison: RegisteredPrisonDto,

  @param:Schema(description = "Permitted visitors list", required = true)
  @field:Valid
  val permittedVisitors: List<BookerPrisonerVisitorDetailedInfoDto>,
) {
  constructor(bookerPrisonerInfoDto: BookerPrisonerInfoDto, visitors: List<PrisonerContactDto>) : this(
    prisoner = bookerPrisonerInfoDto.prisoner,
    registeredPrison = bookerPrisonerInfoDto.registeredPrison,
    permittedVisitors = visitors.map { BookerPrisonerVisitorDetailedInfoDto(it) }.toList(),
  )
}
