package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums

@Suppress("unused")
enum class SessionDateConflict {
  NON_ASSOCIATION,
  PRISON_DATE_BLOCKED,
  OUTSIDE_BOOKING_WINDOW,
  REMAND_VISITS_LIMIT_REACHED,
  ;

  companion object {
    fun get(sessionConflict: SessionConflict): SessionDateConflict? = when (sessionConflict) {
      SessionConflict.NON_ASSOCIATION -> NON_ASSOCIATION
      SessionConflict.PRISON_DATE_BLOCKED -> PRISON_DATE_BLOCKED
      SessionConflict.REMAND_VISITS_LIMIT_REACHED -> REMAND_VISITS_LIMIT_REACHED
      // as a SESSION_DATE_BLOCKED conflict is specific to the session and does not affect the whole date, we set it to null
      SessionConflict.SESSION_DATE_BLOCKED -> null
      // as a DOUBLE_BOOKING_OR_RESERVATION conflict is specific to the session and does not affect the whole date, we set it to null
      SessionConflict.DOUBLE_BOOKING_OR_RESERVATION -> null
      // as a NO_VOS conflict is specific to the session and does not affect the whole date, we set it to null
      SessionConflict.NO_VOS -> null
      // as a NO_PVOS conflict is specific to the session and does not affect the whole date, we set it to null
      SessionConflict.NO_PVOS -> null
      // as a NO_VO_OR_PVOS conflict is specific to the session and does not affect the whole date, we set it to null
      SessionConflict.NO_VO_OR_PVOS -> null
    }
  }
}
