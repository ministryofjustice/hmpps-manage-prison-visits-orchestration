package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.prisons

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Is the date passed excluded")
data class IsExcludeDateDto(
  @Schema(description = "True if the date is excluded, false if not excluded by prison for visits.", required = true)
  var isExcluded: Boolean = false,
)
