package uk.gov.justice.digital.hmpps.orchestration.dto.visit.allocation

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class PrisonerVOBalanceDto(
  @param:Schema(description = "nomsNumber of the prisoner", example = "AA123456", required = true)
  val prisonerId: String,

  @param:Schema(description = "The total of available and accumulated VO balance - any negative VO balance", example = "5", required = true)
  val voBalance: Int,

  @param:Schema(description = "The current available VO balance", example = "5", required = true)
  val availableVos: Int,

  @param:Schema(description = "The current accumulated VO balance", example = "5", required = true)
  val accumulatedVos: Int,

  @param:Schema(description = "The current negative VO balance, denoted as a positive value", example = "5", required = true)
  val negativeVos: Int,

  @param:Schema(description = "The total of available PVO balance - any negative VO balance", example = "5", required = true)
  val pvoBalance: Int,

  @param:Schema(description = "The current available PVO balance", example = "5", required = true)
  val availablePvos: Int,

  @param:Schema(description = "The current negative VO balance, denoted as a positive value", example = "5", required = true)
  val negativePvos: Int,

  @param:Schema(description = "The date VOs were last allocated to the prisoner", example = "2025-01-01", required = true)
  val lastVoAllocatedDate: LocalDate,

  @param:Schema(description = "The next likely VO allocation date", example = "2025-01-01", required = true)
  val nextVoAllocationDate: LocalDate,

  @param:Schema(description = "The date PVOs were last allocated to the prisoner", example = "2025-01-01", required = false)
  val lastPvoAllocatedDate: LocalDate?,

  @param:Schema(description = "The next likely PVO allocation date", example = "2025-01-01", required = false)
  val nextPvoAllocationDate: LocalDate?,
)
