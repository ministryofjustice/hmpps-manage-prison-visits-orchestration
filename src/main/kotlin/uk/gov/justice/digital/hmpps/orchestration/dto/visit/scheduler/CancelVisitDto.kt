package uk.gov.justice.digital.hmpps.orchestration.dto.visit.scheduler

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.orchestration.dto.orchestration.CancelVisitOrchestrationDto
import uk.gov.justice.digital.hmpps.orchestration.dto.visit.scheduler.enums.ApplicationMethodType
import uk.gov.justice.digital.hmpps.orchestration.dto.visit.scheduler.enums.UserType

data class CancelVisitDto(
  @field:Valid
  val cancelOutcome: OutcomeDto,

  @field:NotBlank
  val actionedBy: String,

  @param:Schema(description = "application method", required = true)
  @field:NotNull
  val applicationMethodType: ApplicationMethodType,

  @param:Schema(description = "User type", example = "STAFF", required = true)
  @field:NotNull
  val userType: UserType,
) {
  constructor(cancelVisitOrchestrationDto: CancelVisitOrchestrationDto) : this (
    cancelOutcome = cancelVisitOrchestrationDto.cancelOutcome,
    actionedBy = cancelVisitOrchestrationDto.actionedBy,
    applicationMethodType = cancelVisitOrchestrationDto.applicationMethodType,
    userType = cancelVisitOrchestrationDto.userType,
  )
}
