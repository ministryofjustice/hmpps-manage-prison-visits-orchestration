package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.enums

import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.ValidationErrorCodes

enum class BookerPrisonerValidationErrorCodes : ValidationErrorCodes {
  PRISONER_RELEASED,
  PRISONER_TRANSFERRED_SUPPORTED_PRISON,
  PRISONER_TRANSFERRED_UNSUPPORTED_PRISON,
  REGISTERED_PRISON_NOT_SUPPORTED,
}
