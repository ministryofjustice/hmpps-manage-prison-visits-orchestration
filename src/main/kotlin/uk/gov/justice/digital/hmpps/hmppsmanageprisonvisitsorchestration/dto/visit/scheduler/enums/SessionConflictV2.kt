package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums

@Suppress("unused")
enum class SessionConflictV2 {
  DOUBLE_BOOKING_OR_RESERVATION,
  SESSION_DATE_BLOCKED,
  REMAND_VISITS_LIMIT_REACHED,
  NO_VO_BALANCE,
  NO_PVO_BALANCE,
  NO_VO_OR_PVO_BALANCE,
  ;

  companion object {
    fun get(sessionConflict: SessionConflict): SessionConflictV2? = when (sessionConflict) {
      SessionConflict.NON_ASSOCIATION -> null
      SessionConflict.DOUBLE_BOOKING_OR_RESERVATION -> DOUBLE_BOOKING_OR_RESERVATION
      SessionConflict.SESSION_DATE_BLOCKED -> SESSION_DATE_BLOCKED
      SessionConflict.PRISON_DATE_BLOCKED -> null
      SessionConflict.REMAND_VISITS_LIMIT_REACHED -> REMAND_VISITS_LIMIT_REACHED
      SessionConflict.NO_VO_BALANCE -> NO_VO_BALANCE
      SessionConflict.NO_PVO_BALANCE -> NO_PVO_BALANCE
      SessionConflict.NO_VO_OR_PVO_BALANCE -> NO_VO_OR_PVO_BALANCE
    }
  }
}
