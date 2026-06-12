package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import java.time.DayOfWeek

@Schema(description = "Prison dto")
data class VisitSchedulerPrisonDto(

  @param:Schema(description = "prison code", example = "BHI", required = true)
  val code: String,

  @param:Schema(description = "is prison active", example = "true", required = true)
  val active: Boolean = false,

  @param:Schema(description = "Max number of total visitors")
  @field:Min(1)
  val maxTotalVisitors: Int,

  @param:Schema(description = "Max number of adults")
  @field:Min(1)
  val maxAdultVisitors: Int,

  @param:Schema(description = "Max number of children")
  @field:Min(0)
  val maxChildVisitors: Int,

  @param:Schema(description = "Age of adults in years")
  val adultAgeYears: Int,

  @param:Schema(description = "The week day of which the prison week starts on. Enum value, any day of the week MONDAY - SUNDAY")
  var weekStartDay: DayOfWeek,

  @param:Schema(description = "The limit per prison week, the number of remand visits that can be booked per week")
  @field:Min(1)
  var remandVisitLimitPerWeek: Int,

  @param:Schema(description = "prison user client", required = false)
  val clients: List<PrisonUserClientDto> = listOf(),
)
