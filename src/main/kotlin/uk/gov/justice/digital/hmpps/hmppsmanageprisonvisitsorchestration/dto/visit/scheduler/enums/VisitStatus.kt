package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums

@Suppress("unused")
enum class VisitStatus(
  val description: String,
) {
  BOOKED("Booked"),
  CANCELLED("Cancelled"),
  REQUESTED("Requested"),
  REJECTED("Rejected"),
  AUTO_REJECTED("Auto Rejected"),
  WITHDRAWN("Withdrawn"),
}
