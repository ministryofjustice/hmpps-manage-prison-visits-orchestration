package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.sessions

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.whereabouts.ScheduledEventDto
import java.time.LocalTime

data class PrisonerScheduledEventDto(
  @Schema(required = false, description = "Type of scheduled event (as a code)")
  val eventType: String?,

  @Schema(required = false, description = "Description of scheduled event sub type")
  val eventSubTypeDesc: String?,

  @Schema(required = false, description = "Source-specific description for type or nature of the event")
  val eventSourceDesc: String?,

  @JsonFormat(pattern = "HH:mm", shape = JsonFormat.Shape.STRING)
  @Schema(required = false, description = "Date and time at which event starts")
  val startTime: LocalTime?,

  @JsonFormat(pattern = "HH:mm", shape = JsonFormat.Shape.STRING)
  @Schema(required = false, description = "Date and time at which event ends")
  val endTime: LocalTime?,
) {
  constructor(scheduledEventDto: ScheduledEventDto) : this (
    eventType = scheduledEventDto.eventType,
    eventSubTypeDesc = scheduledEventDto.eventSubTypeDesc,
    eventSourceDesc = scheduledEventDto.eventSourceDesc,
    startTime = scheduledEventDto.startTime?.toLocalTime(),
    endTime = scheduledEventDto.endTime?.toLocalTime(),
  )
}
