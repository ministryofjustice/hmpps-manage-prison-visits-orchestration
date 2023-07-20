package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.ApplicationMethodType

data class CancelVisitDto(
  @field:Valid
  val cancelOutcome: OutcomeDto,

  @field:NotBlank
  val actionedBy: String,

  @Schema(description = "application method", required = true)
  @field:NotNull
  val applicationMethodType: ApplicationMethodType,
)
