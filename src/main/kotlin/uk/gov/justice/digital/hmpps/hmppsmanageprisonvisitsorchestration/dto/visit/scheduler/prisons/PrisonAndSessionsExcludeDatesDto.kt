package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.prisons

import io.swagger.v3.oas.annotations.media.Schema

data class PrisonAndSessionsExcludeDatesDto(
  @param:Schema(description = "Dates excluded for visits (full day exclusions), empty if none.", required = true)
  val fullDateExclusions: List<ExcludeDateDto>,

  @param:Schema(description = "List of sessions that have future exclusions, empty if none.", required = true)
  val sessionExclusions: List<SessionExcludeDateDto>,
)
