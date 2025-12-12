package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

data class ApproveVisitorRequestDto(
  @param:Schema(description = "Identifier for this contact you wish to approve and link (Person in NOMIS)", example = "5871791", required = true)
  @field:NotNull
  val visitorId: Long,
)
