package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums

enum class BookerPrisonerValidationErrorCodes : ValidationErrorCodes {
  PRISONER_RELEASED,
  PRISONER_TRANSFERRED_SUPPORTED_PRISON,
  PRISONER_TRANSFERRED_UNSUPPORTED_PRISON,
}
