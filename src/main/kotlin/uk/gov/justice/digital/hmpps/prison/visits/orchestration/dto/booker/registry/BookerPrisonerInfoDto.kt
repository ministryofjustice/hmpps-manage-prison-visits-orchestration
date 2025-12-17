package uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.booker.registry

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.prisoner.search.PrisonerDto
import java.time.LocalDate

data class BookerPrisonerInfoDto(
  @param:Schema(description = "Prisoner Details", required = true)
  val prisoner: PrisonerDto,

  @param:Schema(description = "Available VOs - a total of VOs and PVOs", example = "0", required = true)
  val availableVos: Int = 0,

  @param:Schema(description = "Next available VO date", example = "2024-08-01", required = true)
  val nextAvailableVoDate: LocalDate,

  @param:Schema(description = "Current prison code for the prison that the booker registered the prisoner with", required = true)
  val registeredPrison: RegisteredPrisonDto,
)
