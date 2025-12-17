package uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.visit.scheduler

@Suppress("unused")
enum class VisitSubStatus(
  val description: String,
) {
  APPROVED("Approved"),
  AUTO_APPROVED("Auto_Approved"),
  REQUESTED("Requested"),

  REJECTED("Rejected"),
  AUTO_REJECTED("Auto_Rejected"),
  WITHDRAWN("Withdrawn"),
  CANCELLED("Cancelled"),
}
