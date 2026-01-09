package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation.PrisonerVOBalanceDetailedDto
import java.time.LocalDate

@Schema(description = "Balances of visit orders and privilege visit orders")
data class VisitBalancesDto(
  @param:Schema(required = true, description = "Balance of visit orders remaining")
  val remainingVo: Int,

  @param:Schema(required = true, description = "Balance of privilege visit orders remaining")
  val remainingPvo: Int,

  @param:Schema(description = "Date of last VO allocation")
  val lastVoAllocationDate: LocalDate,

  @param:Schema(description = "Date of next VO allocation")
  val nextVoAllocationDate: LocalDate,

  @param:Schema(description = "Date of last PVO allocation, null if not allocated")
  val lastPvoAllocationDate: LocalDate? = null,

  @param:Schema(description = "Date of next PVO allocation, null if not allocated")
  val nextPvoAllocationDate: LocalDate? = null,
) {
  constructor(prisonerVOBalanceDetailedDto: PrisonerVOBalanceDetailedDto) : this(
    remainingVo = prisonerVOBalanceDetailedDto.voBalance,
    remainingPvo = prisonerVOBalanceDetailedDto.pvoBalance,
    lastVoAllocationDate = prisonerVOBalanceDetailedDto.lastVoAllocatedDate,
    nextVoAllocationDate = prisonerVOBalanceDetailedDto.nextVoAllocationDate,
    lastPvoAllocationDate = prisonerVOBalanceDetailedDto.lastPvoAllocatedDate,
    nextPvoAllocationDate = prisonerVOBalanceDetailedDto.nextPvoAllocationDate,
  )
}
