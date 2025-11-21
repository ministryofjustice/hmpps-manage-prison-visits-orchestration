package uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.sessions

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.VisitSessionDto
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.enums.SessionConflict
import java.time.LocalTime

data class VisitSessionV2Dto(
  @param:Schema(description = "Session Template Reference", example = "v9d.7ed.7u", required = true)
  @field:NotBlank
  val sessionTemplateReference: String,

  @param:Schema(description = "Visit Room", example = "Visits Main Hall", required = true)
  @field:NotBlank
  val visitRoom: String,

  @param:Schema(description = "The number of concurrent visits which may take place within this session", example = "1", required = true)
  @field:NotNull
  val openVisitCapacity: Int,

  @param:Schema(description = "The count of open visit bookings already reserved or booked for this session", example = "1", required = false)
  var openVisitBookedCount: Int? = 0,

  @param:Schema(description = "The number of closed visits which may take place within this session", example = "1", required = true)
  @field:NotNull
  val closedVisitCapacity: Int,

  @param:Schema(description = "The count of closed visit bookings already reserved or booked for this session", example = "1", required = false)
  var closedVisitBookedCount: Int? = 0,

  @param:JsonFormat(pattern = "HH:mm", shape = JsonFormat.Shape.STRING)
  @param:Schema(description = "The start time of the visit session", example = "10:30", required = true)
  val startTime: LocalTime,

  @param:JsonFormat(pattern = "HH:mm", shape = JsonFormat.Shape.STRING)
  @param:Schema(description = "The end time of the visit session", example = "11:30", required = true)
  val endTime: LocalTime,

  @param:Schema(description = "Session conflicts", required = false)
  val sessionConflicts: MutableSet<@Valid SessionConflict>? = mutableSetOf(),
) {
  constructor(visitSessionDto: VisitSessionDto) : this (
    sessionTemplateReference = visitSessionDto.sessionTemplateReference,
    visitRoom = visitSessionDto.visitRoom,
    openVisitCapacity = visitSessionDto.openVisitCapacity,
    openVisitBookedCount = visitSessionDto.openVisitBookedCount,
    closedVisitCapacity = visitSessionDto.closedVisitCapacity,
    closedVisitBookedCount = visitSessionDto.closedVisitBookedCount,
    startTime = visitSessionDto.startTimestamp.toLocalTime(),
    endTime = visitSessionDto.endTimestamp.toLocalTime(),
    sessionConflicts = visitSessionDto.sessionConflicts,
  )
}
