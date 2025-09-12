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
      else -> null
    }
  }
}
