package uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.sessions

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.whereabouts.ScheduledEventDto
import java.time.LocalTime

data class PrisonerScheduledEventDto(
  @param:Schema(required = false, description = "Type of scheduled event (as a code)")
  val eventType: String?,

  @param:Schema(required = false, description = "Description of scheduled event sub type")
  val eventSubTypeDesc: String?,

  @param:Schema(required = false, description = "Source-specific description for type or nature of the event")
  val eventSourceDesc: String?,

  @param:JsonFormat(pattern = "HH:mm", shape = JsonFormat.Shape.STRING)
  @param:Schema(required = false, description = "Date and time at which event starts")
  val startTime: LocalTime?,

  @param:JsonFormat(pattern = "HH:mm", shape = JsonFormat.Shape.STRING)
  @param:Schema(required = false, description = "Date and time at which event ends")
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
