package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums

@Suppress("unused")
enum class SessionConflict(val includeSession: Boolean) {
  NON_ASSOCIATION(false),
  DOUBLE_BOOKING_OR_RESERVATION(true),
  SESSION_DATE_BLOCKED(false),
  PRISON_DATE_BLOCKED(false),
  REMAND_VISITS_LIMIT_REACHED(true),
}
