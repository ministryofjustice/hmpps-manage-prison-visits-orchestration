package uk.gov.justice.digital.hmpps.orchestration.exception

import jakarta.validation.ValidationException
import uk.gov.justice.digital.hmpps.orchestration.dto.booker.registry.enums.VisitorRequestValidationErrorCodes

class BookerVisitorRequestValidationException(
  val errorCode: VisitorRequestValidationErrorCodes,
) : ValidationException()
