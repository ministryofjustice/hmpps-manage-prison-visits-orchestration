package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception

import jakarta.validation.ValidationException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.enums.BookerPrisonerRegistrationErrorCodes

class BookerPrisonerRegistrationException(
  val errorCode: BookerPrisonerRegistrationErrorCodes,
) : ValidationException()
