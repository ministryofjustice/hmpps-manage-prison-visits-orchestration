package uk.gov.justice.digital.hmpps.visits.orchestration.dto.booker.registry.enums

enum class VisitorRequestValidationErrorCodes {
  PRISONER_NOT_FOUND_FOR_BOOKER,
  MAX_IN_PROGRESS_REQUESTS_REACHED,
  REQUEST_ALREADY_EXISTS,
  VISITOR_ALREADY_EXISTS,
}
