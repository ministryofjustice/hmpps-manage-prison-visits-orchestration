package uk.gov.justice.digital.hmpps.prison.visits.orchestration.exception

import jakarta.validation.ValidationException
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.booker.registry.enums.BookerPrisonerValidationErrorCodes

class BookerPrisonerValidationException(
  val errorCode: BookerPrisonerValidationErrorCodes,
) : ValidationException()
