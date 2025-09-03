package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.sessions

import io.swagger.v3.oas.annotations.media.Schema

class VisitSessionsAndScheduleDto(
  @Schema(description = "If scheduled events are available", example = "true", required = true)
  val scheduledEventsAvailable: Boolean,

  @Schema(description = "List of visit sessions and prisoner schedules", required = true)
  val sessionsAndSchedule: List<SessionsAndScheduleDto>,
)
