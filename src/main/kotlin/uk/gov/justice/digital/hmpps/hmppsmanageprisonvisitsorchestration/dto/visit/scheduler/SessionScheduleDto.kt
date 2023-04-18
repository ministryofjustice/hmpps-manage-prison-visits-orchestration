package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionTemplateFrequency
import java.time.LocalDate
import java.time.LocalTime

@Schema(description = "Session schedule")
data class SessionScheduleDto(

  @Schema(description = "Session Template Reference", example = "v9d.7ed.7u", required = true)
  val sessionTemplateReference: String,

  @Schema(description = "The start time for this visit session", example = "12:00:00", required = true)
  val startTime: LocalTime,

  @Schema(description = "The end timestamp for this visit session", example = "14:30:00", required = true)
  val endTime: LocalTime,

  @Schema(
    description = "The capacity for the session",
    required = true,
  )
  val capacity: SessionCapacityDto,

  @Schema(
    description = "The session is for enhanced privileges",
    required = true,
  )
  val enhanced: Boolean,

  @Schema(description = "prisoner location group", example = "Wing C", required = false)
  val prisonerLocationGroupNames: List<String>,

  @Schema(description = "prisoner category groups", example = "Category A Prisoners", required = false)
  val prisonerCategoryGroupNames: List<String>,

  @Schema(description = "The session template frequency", example = "BI_WEEKLY", required = true)
  val sessionTemplateFrequency: SessionTemplateFrequency,

  @Schema(description = "The end date of sessionTemplate", example = "2020-11-01", required = false)
  val sessionTemplateEndDate: LocalDate?,
)
