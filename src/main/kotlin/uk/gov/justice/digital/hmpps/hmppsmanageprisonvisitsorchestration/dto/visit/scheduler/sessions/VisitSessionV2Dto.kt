package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.sessions

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionTimeSlotDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSessionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionConflict

data class VisitSessionV2Dto(
  @Schema(description = "Session Template Reference", example = "v9d.7ed.7u", required = true)
  @field:NotBlank
  val sessionTemplateReference: String,

  @Schema(description = "Visit Room", example = "Visits Main Hall", required = true)
  @field:NotBlank
  val visitRoom: String,

  @Schema(description = "The number of concurrent visits which may take place within this session", example = "1", required = true)
  @field:NotNull
  val openVisitCapacity: Int,

  @Schema(description = "The count of open visit bookings already reserved or booked for this session", example = "1", required = false)
  var openVisitBookedCount: Int? = 0,

  @Schema(description = "The number of closed visits which may take place within this session", example = "1", required = true)
  @field:NotNull
  val closedVisitCapacity: Int,

  @Schema(description = "The count of closed visit bookings already reserved or booked for this session", example = "1", required = false)
  var closedVisitBookedCount: Int? = 0,

  @Schema(description = "The timeslot for the session", required = true)
  @field:NotNull
  val sessionTimeSlot: SessionTimeSlotDto,

  @Schema(description = "Session conflicts", required = false)
  val sessionConflicts: MutableSet<@Valid SessionConflict>? = mutableSetOf(),
) {
  constructor(visitSessionDto: VisitSessionDto) : this (
    sessionTemplateReference = visitSessionDto.sessionTemplateReference,
    visitRoom = visitSessionDto.visitRoom,
    openVisitCapacity = visitSessionDto.openVisitCapacity,
    openVisitBookedCount = visitSessionDto.openVisitBookedCount,
    closedVisitCapacity = visitSessionDto.closedVisitCapacity,
    closedVisitBookedCount = visitSessionDto.closedVisitBookedCount,
    sessionTimeSlot = SessionTimeSlotDto(visitSessionDto.startTimestamp.toLocalTime(), visitSessionDto.endTimestamp.toLocalTime()),
    sessionConflicts = visitSessionDto.sessionConflicts,
  )
}
