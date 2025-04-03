package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler

data class CancelVisitFromExternalSystemDto(
  val visitReference: String,
  val cancelOutcome: OutcomeDto,
  val actionedBy: String,
)
