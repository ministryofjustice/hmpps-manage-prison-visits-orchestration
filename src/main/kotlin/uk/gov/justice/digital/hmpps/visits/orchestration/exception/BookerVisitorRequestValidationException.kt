package uk.gov.justice.digital.hmpps.visits.orchestration.exception

import jakarta.validation.ValidationException
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.booker.registry.enums.VisitorRequestValidationErrorCodes

class BookerVisitorRequestValidationException(
  val errorCode: VisitorRequestValidationErrorCodes,
) : ValidationException()
