package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler

import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType

data class CancelVisitFromExternalSystemDto(
  val visitReference: String,
  val cancelOutcome: OutcomeDto,
  val userType: UserType,
  val actionedBy: String,
)
