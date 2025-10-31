package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums

@Suppress("unused")
enum class SessionConflict {
  NON_ASSOCIATION,
  DOUBLE_BOOKING_OR_RESERVATION,
  SESSION_DATE_BLOCKED,
  PRISON_DATE_BLOCKED,
  NO_VO_AVAILABLE_FOR_SESSION,
  NO_PVO_AVAILABLE_FOR_SESSION,
}
