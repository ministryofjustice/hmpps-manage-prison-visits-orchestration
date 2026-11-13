package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.ApplicationMethodType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType

data class CancelVisitFromExternalSystemDto(
  val visitReference: String,
  @param:Schema(description = "External system that owns the visit. Missing, null or blank skips ownership validation; nonblank values must match exactly.", required = false)
  val clientName: String? = null,
  val cancelOutcome: OutcomeDto,
  val userType: UserType,
  val actionedBy: String,
) {
  val applicationMethodType: ApplicationMethodType
    get() = if (userType == UserType.PRISONER) ApplicationMethodType.BY_PRISONER else ApplicationMethodType.NOT_KNOWN
}
