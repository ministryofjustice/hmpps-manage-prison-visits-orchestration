package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception

import jakarta.validation.ValidationException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.enums.PrisonerBalanceAdjustmentValidationErrorCodes

class PrisonerBalanceAdjustmentValidationException(
  val errorCodes: List<PrisonerBalanceAdjustmentValidationErrorCodes>,
) : ValidationException()
