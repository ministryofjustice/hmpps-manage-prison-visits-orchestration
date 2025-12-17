package uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.orchestration

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.visit.scheduler.OutcomeDto
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.visit.scheduler.enums.ApplicationMethodType
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.visit.scheduler.enums.UserType

data class CancelVisitOrchestrationDto(
  @param:Schema(description = "Outcome - status and text", required = true)
  @field:Valid
  val cancelOutcome: OutcomeDto,

  @param:Schema(description = "application method", required = true)
  @field:NotNull
  val applicationMethodType: ApplicationMethodType,

  @param:Schema(description = "Username for user who actioned this request", required = true)
  @field:NotBlank
  val actionedBy: String,

  @param:Schema(description = "User type", example = "STAFF", required = true)
  @field:NotNull
  val userType: UserType,
)
