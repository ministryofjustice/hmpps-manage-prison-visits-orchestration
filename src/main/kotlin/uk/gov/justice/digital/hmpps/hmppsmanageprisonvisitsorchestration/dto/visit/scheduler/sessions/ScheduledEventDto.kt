package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.sessions

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalTime

data class ScheduledEventDto(
  @Schema(required = false, description = "Type of scheduled event (as a code)")
  val eventType: String?,

  @Schema(required = true, description = "Description of scheduled event type")
  val eventTypeDesc: String? = null,

  @Schema(required = false, description = "Description of scheduled event sub type")
  val eventSubTypeDesc: String?,

  @Schema(required = false, description = "Date on which event occurs")
  val eventDate: LocalDate?,

  @Schema(required = false, description = "Date and time at which event starts")
  val startTime: LocalTime?,

  @Schema(required = false, description = "Date and time at which event ends")
  val endTime: LocalTime?,
)
