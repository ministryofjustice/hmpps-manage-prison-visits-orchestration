package uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.booker.registry

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.booker.registry.enums.VisitorRequestRejectionReason

data class RejectVisitorRequestDto(
  @param:Schema(description = "Rejection Reason type", example = "ALREADY_LINKED", required = true)
  @field:NotNull
  val rejectionReason: VisitorRequestRejectionReason,
)
