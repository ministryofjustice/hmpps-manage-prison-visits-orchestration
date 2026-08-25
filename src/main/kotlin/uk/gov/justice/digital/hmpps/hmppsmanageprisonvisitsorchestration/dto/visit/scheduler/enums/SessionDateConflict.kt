package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums

@Suppress("unused")
enum class SessionDateConflict {
  NON_ASSOCIATION,
  PRISON_DATE_BLOCKED,
  OUTSIDE_BOOKING_WINDOW,
  ;

  companion object {
    fun get(sessionConflict: SessionConflict): SessionDateConflict? = when (sessionConflict) {
      SessionConflict.NON_ASSOCIATION -> NON_ASSOCIATION
      SessionConflict.PRISON_DATE_BLOCKED -> PRISON_DATE_BLOCKED
      // as a REMAND_VISITS_LIMIT_REACHED conflict is not handled currently, and the session is presented to STAFF, setting it to null
      SessionConflict.REMAND_VISITS_LIMIT_REACHED -> null
      // as a SESSION_DATE_BLOCKED conflict is specific to the session and does not affect the whole date, we set it to null
      SessionConflict.SESSION_DATE_BLOCKED -> null
      // as a DOUBLE_BOOKING_OR_RESERVATION conflict is specific to the session and does not affect the whole date, we set it to null
      SessionConflict.DOUBLE_BOOKING_OR_RESERVATION -> null
      // as a NO_VO_BALANCE conflict is specific to the session and does not affect the whole date, we set it to null
      SessionConflict.NO_VO_BALANCE -> null
      // as a NO_PVO_BALANCE conflict is specific to the session and does not affect the whole date, we set it to null
      SessionConflict.NO_PVO_BALANCE -> null
      // as a NO_VO_OR_PVO_BALANCE conflict is specific to the session and does not affect the whole date, we set it to null
      SessionConflict.NO_VO_OR_PVO_BALANCE -> null
    }
  }
}
