package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums

@Suppress("unused")
enum class SessionDateConflict(val includeSessions: Boolean) {
  NON_ASSOCIATION(includeSessions = false),
  PRISON_DATE_BLOCKED(includeSessions = false),
  OUTSIDE_BOOKING_WINDOW(includeSessions = false),
  REMAND_VISITS_LIMIT_REACHED(includeSessions = true),
  ;

  companion object {
    fun get(sessionConflict: SessionConflict): SessionDateConflict? = when (sessionConflict) {
      SessionConflict.NON_ASSOCIATION -> NON_ASSOCIATION
      SessionConflict.PRISON_DATE_BLOCKED -> PRISON_DATE_BLOCKED
      SessionConflict.REMAND_VISITS_LIMIT_REACHED -> REMAND_VISITS_LIMIT_REACHED
      SessionConflict.SESSION_DATE_BLOCKED -> null
      SessionConflict.DOUBLE_BOOKING_OR_RESERVATION -> null
    }
  }
}
