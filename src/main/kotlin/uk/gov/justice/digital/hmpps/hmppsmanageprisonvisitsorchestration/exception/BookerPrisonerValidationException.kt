package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception

import jakarta.validation.ValidationException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.enums.BookerPrisonerValidationErrorCodes

class BookerPrisonerValidationException(
  val errorCode: BookerPrisonerValidationErrorCodes,
) : ValidationException()
