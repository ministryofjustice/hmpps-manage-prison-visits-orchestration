package uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.visit.scheduler

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalTime

data class SessionTimeSlotDto(
  @field:JsonFormat(pattern = "HH:mm", shape = JsonFormat.Shape.STRING)
  @param:Schema(description = "The start time of the generated visit session(s)", example = "10:30", required = true)
  val startTime: LocalTime,

  @field:JsonFormat(pattern = "HH:mm", shape = JsonFormat.Shape.STRING)
  @param:Schema(description = "The end time of the generated visit session(s)", example = "11:30", required = true)
  val endTime: LocalTime,
)
