package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.ApplicationMethodType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType

data class CancelVisitFromExternalSystemDto(
  val visitReference: String,
  @param:Schema(description = "External system that owns the visit", required = true)
  @field:NotBlank
  val clientName: String,
  val cancelOutcome: OutcomeDto,
  val userType: UserType,
  val actionedBy: String,
  @param:Schema(description = "Application method supplied by the external system", required = true)
  val applicationMethodType: ApplicationMethodType,
)
