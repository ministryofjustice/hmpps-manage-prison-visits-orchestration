package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.prisons

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionScheduleDto

class SessionExcludeDateDto(
  @param:Schema(description = "Exclude date details", required = true)
  val excludeDate: ExcludeDateDto,

  @param:Schema(description = "Session schedule details", required = true)
  val sessionSchedule: SessionScheduleDto,
)
