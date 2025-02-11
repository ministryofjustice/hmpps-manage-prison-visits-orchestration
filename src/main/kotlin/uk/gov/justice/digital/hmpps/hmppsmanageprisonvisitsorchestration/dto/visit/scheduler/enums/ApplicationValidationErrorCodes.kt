package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums

enum class ApplicationValidationErrorCodes : ValidationErrorCodes {
  APPLICATION_INVALID_PRISONER_NOT_FOUND,
  APPLICATION_INVALID_PRISON_PRISONER_MISMATCH,
  APPLICATION_INVALID_SESSION_NOT_AVAILABLE,
  APPLICATION_INVALID_SESSION_TEMPLATE_NOT_FOUND,
  APPLICATION_INVALID_NON_ASSOCIATION_VISITS,
  APPLICATION_INVALID_VISIT_ALREADY_BOOKED,
  APPLICATION_INVALID_NO_VO_BALANCE,
  APPLICATION_INVALID_NO_SLOT_CAPACITY,
}
