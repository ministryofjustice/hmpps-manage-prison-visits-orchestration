package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation.enums.AdjustmentReasonType

data class PrisonerBalanceAdjustmentDto(
  @param:Schema(description = "VOs that need to be added or removed (can be negative, negative denotes REMOVE)", example = "5", required = false)
  val voAmount: Int?,

  @param:Schema(description = "PVOs that need to be added or removed (can be negative, negative denotes REMOVE)", example = "5", required = false)
  val pvoAmount: Int?,

  @param:Schema(description = "Adjustment Reason Type", required = true)
  val adjustmentReasonType: AdjustmentReasonType,

  @param:Schema(description = "Adjustment Reason Text", required = false)
  val adjustmentReasonText: String? = null,

  @param:Schema(description = "Staff user ID", example = "ABC1234", required = true)
  val userName: String,
)
