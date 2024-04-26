package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitType
import uk.gov.justice.digital.hmpps.visitscheduler.dto.sessions.SessionDateRangeDto

@Schema(description = "Session schedule")
data class SessionScheduleDto(

  @Schema(description = "Session Template Reference", example = "v9d.7ed.7u", required = true)
  @field:NotBlank
  val sessionTemplateReference: String,

  @Schema(description = "The time slot of the generated visit session(s)", required = true)
  @NotNull
  val sessionTimeSlot: SessionTimeSlotDto,

  @Schema(description = "Validity period for the session template", required = true)
  @NotNull
  val sessionDateRange: SessionDateRangeDto,

  @Schema(
    description = "The capacity for the session",
    required = true,
  )
  @NotNull
  val capacity: SessionCapacityDto,

  @Schema(description = "visit type", example = "Social", required = true)
  @NotNull
  val visitType: VisitType,

  @Schema(description = "Determines behaviour of location groups. True will mean the location groups are inclusive, false means they are exclusive.", required = true)
  val areLocationGroupsInclusive: Boolean,

  @Schema(description = "prisoner location group", example = "Wing C", required = false)
  val prisonerLocationGroupNames: List<String>,

  @Schema(description = "prisoner category groups", example = "Category A Prisoners", required = false)
  val prisonerCategoryGroupNames: List<String>,

  @Schema(description = "prisoner incentive level groups", example = "Enhanced Incentive Level Prisoners", required = false)
  val prisonerIncentiveLevelGroupNames: List<String>,

  @Schema(description = "number of weeks until the weekly day is repeated", example = "1", required = true)
  @NotNull
  val weeklyFrequency: Int,
)
