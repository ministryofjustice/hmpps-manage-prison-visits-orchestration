package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation.enums

@Suppress("unused")
enum class PrisonerBalanceAdjustmentValidationErrorCodes {
  VO_OR_PVO_NOT_SUPPLIED,
  VO_TOTAL_POST_ADJUSTMENT_ABOVE_MAX,
  VO_TOTAL_POST_ADJUSTMENT_BELOW_ZERO,
  PVO_TOTAL_POST_ADJUSTMENT_ABOVE_MAX,
  PVO_TOTAL_POST_ADJUSTMENT_BELOW_ZERO,
}
