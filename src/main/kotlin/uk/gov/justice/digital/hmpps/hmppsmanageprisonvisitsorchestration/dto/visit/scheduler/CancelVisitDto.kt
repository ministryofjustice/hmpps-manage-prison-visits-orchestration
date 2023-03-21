package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank

data class CancelVisitDto(
  @field:Valid
  val cancelOutcome: OutcomeDto,

  @field:NotBlank
  val actionedBy: String,
)
