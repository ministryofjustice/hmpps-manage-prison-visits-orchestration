package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "VisitNote")
class VisitNoteDto(
  @Schema(description = "Note type", example = "VISITOR_CONCERN", required = true)
  val type: String,
  @Schema(description = "Note text", example = "Visitor is concerned that his mother in-law is coming!", required = true)
  val text: String
)
