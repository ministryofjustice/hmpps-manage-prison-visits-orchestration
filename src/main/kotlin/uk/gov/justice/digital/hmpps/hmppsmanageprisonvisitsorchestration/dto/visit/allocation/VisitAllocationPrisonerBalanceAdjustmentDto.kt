package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation

import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation.enums.AdjustmentReasonType

data class VisitAllocationPrisonerBalanceAdjustmentDto(
  val voAmount: Int?,
  val pvoAmount: Int?,
  val adjustmentReasonType: AdjustmentReasonType,
  val adjustmentReasonText: String?,
  val userName: String,
  val caseloadId: String,
) {
  constructor(prisonerBalanceAdjustmentDto: PrisonerBalanceAdjustmentDto, caseloadId: String) : this(
    voAmount = prisonerBalanceAdjustmentDto.voAmount,
    pvoAmount = prisonerBalanceAdjustmentDto.pvoAmount,
    adjustmentReasonType = prisonerBalanceAdjustmentDto.adjustmentReasonType,
    adjustmentReasonText = prisonerBalanceAdjustmentDto.adjustmentReasonText,
    userName = prisonerBalanceAdjustmentDto.userName,
    caseloadId = caseloadId,
  )
}
