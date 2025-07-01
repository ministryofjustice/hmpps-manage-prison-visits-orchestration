package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.ContactDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSubStatus
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitorSupportDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.OutcomeStatus
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitStatus
import java.time.LocalDateTime

@Schema(description = "Visit")
data class OrchestrationVisitDto(
  @Schema(description = "Visit Reference", example = "v9-d7-ed-7u", required = true)
  val reference: String,
  @Schema(description = "Prisoner Id", example = "AF34567G", required = true)
  val prisonerId: String,
  @Schema(description = "Prisoner first name", example = "James", required = false)
  val prisonerFirstName: String?,
  @Schema(description = "Prisoner last name", example = "Smith", required = false)
  val prisonerLastName: String?,
  @JsonProperty("prisonId")
  @Schema(description = "Prison Id", example = "MDI", required = true)
  val prisonCode: String,
  @Schema(description = "Visit Status", example = "BOOKED", required = true)
  val visitStatus: VisitStatus,
  @Schema(description = "Visit Sub Status", example = "AUTO_APPROVED", required = true)
  val visitSubStatus: VisitSubStatus,
  @Schema(description = "Outcome Status", example = "VISITOR_CANCELLED", required = false)
  val outcomeStatus: OutcomeStatus? = null,
  @Schema(description = "The date and time of the visit", example = "2018-12-01T13:45:00", required = true)
  @field:NotNull
  val startTimestamp: LocalDateTime,
  @Schema(description = "The finishing date and time of the visit", example = "2018-12-01T13:45:00", required = true)
  @field:NotNull
  val endTimestamp: LocalDateTime,
  @Schema(description = "Contact associated with the visit", required = true)
  val visitContact: ContactDto? = null,
  @Schema(description = "List of visitors associated with the visit", required = true)
  val visitors: List<OrchestrationVisitorDto> = listOf(),
  @Schema(description = "Additional support associated with the visit", required = false)
  val visitorSupport: VisitorSupportDto? = null,
)
