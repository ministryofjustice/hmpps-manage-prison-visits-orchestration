package uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.booker.registry.enums

import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.visit.scheduler.enums.ValidationErrorCodes

enum class BookerPrisonerValidationErrorCodes : ValidationErrorCodes {
  PRISONER_RELEASED,
  PRISONER_TRANSFERRED_SUPPORTED_PRISON,
  PRISONER_TRANSFERRED_UNSUPPORTED_PRISON,
  REGISTERED_PRISON_NOT_SUPPORTED,
}
