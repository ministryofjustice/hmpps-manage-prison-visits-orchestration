package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception

import jakarta.validation.ValidationException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.BookerPrisonerValidationErrorCodes

class BookerPrisonerValidationException(
  val errorCodes: List<BookerPrisonerValidationErrorCodes>,
) : ValidationException()
