package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.sessions

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

class SessionsAndScheduleDto(
  @param:Schema(description = "Session date", example = "2020-11-01", required = true)
  @field:NotNull
  val date: LocalDate,

  @param:Schema(description = "Visit sessions", required = true)
  val visitSessions: List<VisitSessionV2Dto>,

  @param:Schema(description = "Visit sessions", required = true)
  val scheduledEvents: List<PrisonerScheduledEventDto>,
)
