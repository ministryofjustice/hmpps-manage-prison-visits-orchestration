package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums

@Suppress("unused")
enum class VisitRestriction(
  val description: String,
) {
  OPEN("Open"),
  CLOSED("Closed"),
  UNKNOWN("Unknown"),
}
