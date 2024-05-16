package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.FutureOrPresent
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitRestriction
import java.time.LocalDate

@Schema(description = "Visit Session")
data class AvailableVisitSessionDto(
  @Schema(description = "Session date", example = "2020-11-01", required = true)
  @field:NotNull
  @FutureOrPresent
  val sessionDate: LocalDate,

  @Schema(description = "sessionTemplateReference", example = "v9d.7ed.7u", required = true)
  val sessionTemplateReference: String,

  @Schema(description = "Session time slot", required = true)
  @field:NotNull
  @Valid
  val sessionTimeSlot: SessionTimeSlotDto,

  @Schema(description = "Visit Restriction", example = "OPEN", required = true)
  @field:NotNull
  val visitRestriction: VisitRestriction,
)
