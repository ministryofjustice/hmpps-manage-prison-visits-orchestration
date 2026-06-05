package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.prisons

import io.swagger.v3.oas.annotations.media.Schema

data class PrisonAndSessionsExcludeDatesDto(
  @param:Schema(description = "Dates excluded for visits (full day exclusions), empty if none.", required = true)
  val fullDateExclusions: List<ExcludeDateDto>,

  @param:Schema(description = "Map of session template reference and list of dates for which the session has been excluded for visits, empty if none.", required = true)
  val sessionExclusions: Map<String, List<ExcludeDateDto>>,
)
