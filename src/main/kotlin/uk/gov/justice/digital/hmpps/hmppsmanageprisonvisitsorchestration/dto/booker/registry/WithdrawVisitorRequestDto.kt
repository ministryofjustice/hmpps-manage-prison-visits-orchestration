package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class WithdrawVisitorRequestDto(
  @param:Schema(description = "Reference of booker who rejected the visitor", example = "ab-cd-ef-gh", required = true)
  @field:NotNull
  @field:NotBlank
  val bookerReference: String,
)
