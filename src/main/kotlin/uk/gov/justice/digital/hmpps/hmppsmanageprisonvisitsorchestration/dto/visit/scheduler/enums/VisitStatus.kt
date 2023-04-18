package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums

@Suppress("unused")
enum class VisitStatus(
  val description: String,
) {
  RESERVED("Reserved"),
  CHANGING("Changing"),
  BOOKED("Booked"),
  CANCELLED("Cancelled"),
}
