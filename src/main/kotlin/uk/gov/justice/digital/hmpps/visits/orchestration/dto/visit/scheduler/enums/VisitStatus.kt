package uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.enums

@Suppress("unused")
enum class VisitStatus(
  val description: String,
) {
  BOOKED("Booked"),
  CANCELLED("Cancelled"),
}
