package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.prisons

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "Prison exclude date")
data class ExcludeDateDto(
  @Schema(description = "exclude date", example = "2024-26-12", required = true)
  val excludeDate: LocalDate,

  @Schema(description = "full name of user who added the exclude date or username if full name is not available.", required = true)
  var actionedBy: String,
)
