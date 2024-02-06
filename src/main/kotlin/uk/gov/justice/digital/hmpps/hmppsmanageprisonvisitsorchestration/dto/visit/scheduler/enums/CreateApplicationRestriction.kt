package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums

@Suppress("unused")
enum class CreateApplicationRestriction(
  val description: String,
) {
  OPEN("Open"),
  CLOSED("Closed"),
}
