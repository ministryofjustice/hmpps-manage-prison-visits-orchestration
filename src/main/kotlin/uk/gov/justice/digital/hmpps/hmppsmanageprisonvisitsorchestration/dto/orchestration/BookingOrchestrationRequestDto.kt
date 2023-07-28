package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.ApplicationMethodType

data class BookingOrchestrationRequestDto(
  @Schema(description = "application method", required = true)
  @field:NotNull
  val applicationMethodType: ApplicationMethodType,
)
