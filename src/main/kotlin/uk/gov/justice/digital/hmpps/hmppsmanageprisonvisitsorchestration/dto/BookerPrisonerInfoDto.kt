package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerDto
import java.time.LocalDate

data class BookerPrisonerInfoDto(
  @Schema(description = "Prisoner Details", required = true)
  val prisoner: PrisonerDto,

  @Schema(description = "Available VOs - a total of VOs and PVOs", example = "0", required = true)
  val availableVos: Int = 0,

  @Schema(description = "Next available VO date", example = "2024-08-01", required = true)
  val nextAvailableVoDate: LocalDate,
)
