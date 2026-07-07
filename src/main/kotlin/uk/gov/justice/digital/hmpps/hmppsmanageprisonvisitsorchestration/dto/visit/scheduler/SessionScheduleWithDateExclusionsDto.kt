package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.prisons.ExcludeDateDto

@Schema(description = "Session schedule with current or future future date exclusions")
data class SessionScheduleWithDateExclusionsDto(
  @param:Schema(description = "Session schedule details that have future date exclusions", required = true)
  val sessionSchedule: SessionScheduleDto,

  @param:Schema(description = "Future exclude dates for the session.", required = true)
  val excludeDates: List<ExcludeDateDto>,
)
