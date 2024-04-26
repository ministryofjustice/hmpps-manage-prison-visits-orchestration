package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums

enum class SessionType(
  val description: String,
) {
  OPEN("Open Visit Session"),
  CLOSED("Closed Visit Session"),
}
