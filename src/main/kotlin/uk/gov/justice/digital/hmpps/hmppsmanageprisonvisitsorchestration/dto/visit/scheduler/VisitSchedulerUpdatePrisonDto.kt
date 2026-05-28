package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import java.time.DayOfWeek

@Schema(description = "Prison update dto")
data class VisitSchedulerUpdatePrisonDto(
  @param:Schema(description = "The week day of which the prison week starts on. Enum value, any day of the week MONDAY - SUNDAY")
  var weekStartDay: DayOfWeek?,
  @param:Schema(description = "The limit per prison week, the number of remand visits that can be booked per week")
  @field:Min(1)
  var remandVisitLimitPerWeek: Int?,
)
