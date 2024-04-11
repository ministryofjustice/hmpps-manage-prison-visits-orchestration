package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import java.time.LocalDate

@Schema(description = "Prison dto")
data class PrisonDto(

  @Schema(description = "prison code", example = "BHI", required = true)
  var code: String,

  @Schema(description = "is prison active", example = "true", required = true)
  var active: Boolean = false,

  @Schema(description = "minimum number of days notice from the current date to booked a visit", example = "2", required = true)
  var policyNoticeDaysMin: Int,
  @Schema(description = "maximum number of days notice from the current date to booked a visit", example = "28", required = true)
  var policyNoticeDaysMax: Int,

  @Schema(description = "Max number of total visitors")
  @field:Min(1)
  val maxTotalVisitors: Int,
  @Schema(description = "Max number of adults")
  @field:Min(1)
  val maxAdultVisitors: Int,
  @Schema(description = "Max number of children, if -1 then no limit is applied")
  @field:Min(-1)
  val maxChildVisitors: Int,
  @Schema(description = "Age of adults in years")
  @field:Min(6)
  val adultAgeYears: Int,

  @Schema(description = "exclude dates", required = false)
  var excludeDates: Set<LocalDate> = mutableSetOf(),
)
