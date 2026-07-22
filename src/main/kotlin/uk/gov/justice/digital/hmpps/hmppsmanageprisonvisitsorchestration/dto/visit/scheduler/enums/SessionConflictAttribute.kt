package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums

import io.swagger.v3.oas.annotations.media.Schema

enum class SessionConflictAttribute {
  @Schema(description = "Prisoner Number")
  PRISONER_NUMBER,

  @Schema(description = "Conflict type i.e In Progress Application or a booked Visit")
  CONFLICT_TYPE,

  @Schema(description = "Booked Visit reference")
  REFERENCE,
}
