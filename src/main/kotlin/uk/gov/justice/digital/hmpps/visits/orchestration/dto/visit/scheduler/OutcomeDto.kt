package uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.enums.OutcomeStatus

@Schema(description = "Contact Phone Number", example = "01234 567890", required = true)
class OutcomeDto(
  @param:Schema(description = "Outcome Status", example = "VISITOR_CANCELLED", required = true)
  val outcomeStatus: OutcomeStatus,

  @param:Schema(description = "Outcome text", example = "Because he got covid", required = false)
  val text: String? = null,
)
