package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitor

import io.swagger.v3.oas.annotations.media.Schema

data class VisitorLastApprovedDateRequestDto(
  @Schema(description = "List of Nomis Person Ids for whom last visit approved date is needed", required = true)
  val nomisPersonIds: List<Long>,
)
