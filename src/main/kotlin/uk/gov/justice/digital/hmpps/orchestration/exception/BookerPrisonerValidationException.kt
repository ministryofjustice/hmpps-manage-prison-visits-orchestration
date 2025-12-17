package uk.gov.justice.digital.hmpps.orchestration.exception

import jakarta.validation.ValidationException
import uk.gov.justice.digital.hmpps.orchestration.dto.booker.registry.enums.BookerPrisonerValidationErrorCodes

class BookerPrisonerValidationException(
  val errorCode: BookerPrisonerValidationErrorCodes,
) : ValidationException()
