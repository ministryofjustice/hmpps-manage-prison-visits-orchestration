package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class PrisonerVOBalanceDto(
  @Schema(description = "nomsNumber of the prisoner", example = "AA123456", required = true)
  val prisonerId: String,

  @Schema(description = "The current available VO balance", example = "5", required = true)
  val availableVos: Int,

  @Schema(description = "The current accumulated VO balance", example = "5", required = true)
  val accumulatedVos: Int,

  @Schema(description = "The current negative VO balance, denoted as a positive value", example = "5", required = true)
  val negativeVos: Int,

  @Schema(description = "The current available PVO balance", example = "5", required = true)
  val availablePvos: Int,

  @Schema(description = "The current negative VO balance, denoted as a positive value", example = "5", required = true)
  val negativePvos: Int,

  @Schema(description = "The date VOs were last allocated to the prisoner", example = "2025-01-01", required = true)
  val lastVoAllocatedDate: LocalDate,

  @Schema(description = "The date PVOs were last allocated to the prisoner", example = "2025-01-01", required = false)
  val lastPvoAllocatedDate: LocalDate?,
)
