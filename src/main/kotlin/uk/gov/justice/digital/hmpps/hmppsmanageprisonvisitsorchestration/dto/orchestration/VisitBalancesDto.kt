package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "Balances of visit orders and privilege visit orders")
data class VisitBalancesDto(
  @Schema(required = true, description = "Balance of visit orders remaining")
  val remainingVo: Int,

  @Schema(required = true, description = "Balance of privilege visit orders remaining")
  val remainingPvo: Int,

  @Schema(description = "Date of next VO allocation")
  val nextVoAllocationDate: LocalDate,

  @Schema(description = "Date of next PVO allocation, null if not allocated")
  val nextPvoAllocationDate: LocalDate? = null,
)
