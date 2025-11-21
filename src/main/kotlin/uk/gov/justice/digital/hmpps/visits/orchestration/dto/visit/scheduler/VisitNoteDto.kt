package uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.enums.VisitNoteType

@Schema(description = "VisitNote")
class VisitNoteDto(
  @param:Schema(description = "Note type", example = "VISITOR_CONCERN", required = true)
  val type: VisitNoteType,
  @param:Schema(description = "Note text", example = "Visitor is concerned that his mother in-law is coming!", required = true)
  val text: String,
)
