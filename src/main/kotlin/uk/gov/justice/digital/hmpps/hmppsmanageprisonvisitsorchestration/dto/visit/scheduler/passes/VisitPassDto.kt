package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.passes

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitRestriction
import java.time.LocalTime

@Schema(description = "Visit Pass Details")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class VisitPassDto(
  @param:Schema(description = "Visit Reference", example = "v9-d7-ed-7u", required = true)
  val reference: String,
  @param:Schema(description = "Visit Start time", example = "11:00", required = true)
  val startTime: LocalTime,
  @param:Schema(description = "Visit End time", example = "13:00", required = true)
  val endTime: LocalTime,
  @param:Schema(description = "Prisoner Id", example = "AF34567G", required = true)
  val prisonerId: String,
  @param:Schema(description = "Prisoner First Name", example = "John", required = true)
  val prisonerFirstName: String,
  @param:Schema(description = "Prisoner Last Name", example = "Smith", required = true)
  val prisonerLastName: String,
  @param:Schema(description = "Visit Restriction", example = "OPEN", required = true)
  val visitRestriction: VisitRestriction,
  @param:Schema(description = "Visitor Details", required = true)
  val visitors: List<VisitPassVisitorDto>,
)
