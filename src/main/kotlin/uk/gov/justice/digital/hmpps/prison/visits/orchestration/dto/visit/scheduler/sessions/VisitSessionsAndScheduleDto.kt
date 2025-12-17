package uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.visit.scheduler.sessions

import io.swagger.v3.oas.annotations.media.Schema

class VisitSessionsAndScheduleDto(
  @param:Schema(description = "If scheduled events are available", example = "true", required = true)
  val scheduledEventsAvailable: Boolean,

  @param:Schema(description = "List of visit sessions and prisoner schedules", required = true)
  val sessionsAndSchedule: List<SessionsAndScheduleDto>,
)
