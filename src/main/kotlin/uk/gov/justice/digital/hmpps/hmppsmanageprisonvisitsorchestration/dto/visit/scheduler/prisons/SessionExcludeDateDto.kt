package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.prisons

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionScheduleDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionTimeSlotDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionTemplateVisitOrderRestrictionType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitType

class SessionExcludeDateDto(
  @param:Schema(description = "Exclude date details", required = true)
  val excludeDate: ExcludeDateDto,

  @param:Schema(description = "Session Template Reference", example = "v9d.7ed.7u", required = true)
  @field:NotBlank
  val sessionTemplateReference: String,

  @param:Schema(description = "The time slot of the generated visit session(s)", required = true)
  @field:NotNull
  val sessionTimeSlot: SessionTimeSlotDto,

  @param:Schema(description = "visit type", example = "Social", required = true)
  @field:NotNull
  val visitType: VisitType,

  @param:Schema(description = "Determines behaviour of location groups. True will mean the location groups are inclusive, false means they are exclusive.", required = true)
  val areLocationGroupsInclusive: Boolean,

  @param:Schema(description = "prisoner location group", example = "Wing C", required = false)
  val prisonerLocationGroupNames: List<String>,

  @param:Schema(description = "Determines behaviour of category groups. True will mean the category groups are inclusive, false means they are exclusive.", required = true)
  val areCategoryGroupsInclusive: Boolean,

  @param:Schema(description = "prisoner category groups", example = "Category A Prisoners", required = false)
  val prisonerCategoryGroupNames: List<String>,

  @param:Schema(description = "Determines behaviour of incentive groups. True will mean the incentive groups are inclusive, false means they are exclusive.", required = true)
  val areIncentiveGroupsInclusive: Boolean,

  @param:Schema(description = "prisoner incentive level groups", example = "Enhanced Incentive Level Prisoners", required = false)
  val prisonerIncentiveLevelGroupNames: List<String>,

  @param:Schema(description = "number of weeks until the weekly day is repeated", example = "1", required = true)
  @field:NotNull
  val weeklyFrequency: Int,

  @param:Schema(description = "visit room name", example = "Visits Room", required = true)
  val visitRoom: String,

  @param:Schema(description = "Session vo restriction", required = true)
  val visitOrderRestriction: SessionTemplateVisitOrderRestrictionType,
) {
  constructor(sessionSchedule: SessionScheduleDto, excludeDate: ExcludeDateDto) : this(
    sessionTemplateReference = sessionSchedule.sessionTemplateReference,
    sessionTimeSlot = sessionSchedule.sessionTimeSlot,
    visitType = sessionSchedule.visitType,
    areLocationGroupsInclusive = sessionSchedule.areLocationGroupsInclusive,
    prisonerLocationGroupNames = sessionSchedule.prisonerLocationGroupNames,
    areCategoryGroupsInclusive = sessionSchedule.areCategoryGroupsInclusive,
    prisonerCategoryGroupNames = sessionSchedule.prisonerCategoryGroupNames,
    areIncentiveGroupsInclusive = sessionSchedule.areIncentiveGroupsInclusive,
    prisonerIncentiveLevelGroupNames = sessionSchedule.prisonerIncentiveLevelGroupNames,
    weeklyFrequency = sessionSchedule.weeklyFrequency,
    visitRoom = sessionSchedule.visitRoom,
    visitOrderRestriction = sessionSchedule.visitOrderRestriction,
    excludeDate = excludeDate,
  )
}
