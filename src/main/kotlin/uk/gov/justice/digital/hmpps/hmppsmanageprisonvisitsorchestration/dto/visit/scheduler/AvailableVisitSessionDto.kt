package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.FutureOrPresent
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.PublicSessionConflict
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionRestriction
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionTemplateVisitOrderRestrictionType
import java.time.LocalDate

@Schema(description = "Visit Session")
data class AvailableVisitSessionDto(
  @param:Schema(description = "Session date", example = "2020-11-01", required = true)
  @field:NotNull
  @field:FutureOrPresent
  val sessionDate: LocalDate,

  @param:Schema(description = "sessionTemplateReference", example = "v9d.7ed.7u", required = true)
  val sessionTemplateReference: String,

  @param:Schema(description = "Session time slot", required = true)
  @field:NotNull
  @field:Valid
  val sessionTimeSlot: SessionTimeSlotDto,

  @param:Schema(description = "Session Restriction", example = "OPEN", required = true)
  @field:NotNull
  val sessionRestriction: SessionRestriction,

  @param:Schema(description = "Does session need review, defaults to false", example = "true", required = true)
  var sessionForReview: Boolean = false,

  @param:Schema(description = "Session vo restriction", required = true)
  val visitOrderRestriction: SessionTemplateVisitOrderRestrictionType,

  @param:Schema(description = "Determines if the age restriction is enabled for this session", example = "true", required = true)
  val isAgeRestricted: Boolean = false,

  @param:Schema(description = "Minimum required age for attending the session", example = "18", required = true)
  val ageRestriction: Int = 18,

  @param:Schema(description = "Session conflicts", required = false)
  val sessionConflicts: Set<PublicSessionConflict> = setOf(),
)
