package uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.visit.scheduler

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min

@Schema(description = "Prison dto")
data class VisitSchedulerPrisonDto(

  @param:Schema(description = "prison code", example = "BHI", required = true)
  val code: String,

  @param:Schema(description = "is prison active", example = "true", required = true)
  val active: Boolean = false,

  @param:Schema(description = "minimum number of days notice from the current date to booked a visit", example = "2", required = true)
  val policyNoticeDaysMin: Int,

  @param:Schema(description = "maximum number of days notice from the current date to booked a visit", example = "28", required = true)
  val policyNoticeDaysMax: Int,

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

  @param:Schema(description = "prison user client", required = false)
  val clients: List<PrisonUserClientDto> = listOf(),
)
