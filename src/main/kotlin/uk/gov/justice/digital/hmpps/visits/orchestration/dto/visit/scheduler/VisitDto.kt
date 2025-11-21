package uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.enums.OutcomeStatus
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.enums.VisitRestriction
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.enums.VisitStatus
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.enums.VisitType
import java.time.LocalDateTime

@Schema(description = "Visit")
@JsonInclude(JsonInclude.Include.NON_NULL)
class VisitDto(
  @param:Schema(description = "Application Reference", example = "dfs-wjs-eqr", required = true)
  val applicationReference: String? = null,
  @param:Schema(description = "Visit Reference", example = "v9-d7-ed-7u", required = true)
  val reference: String,
  @param:Schema(description = "Prisoner Id", example = "AF34567G", required = true)
  val prisonerId: String,
  @param:JsonProperty("prisonId")
  @param:JsonAlias("prisonCode")
  @param:Schema(description = "Prison Id", example = "MDI", required = true)
  val prisonCode: String,
  @param:Schema(description = "Prison Name", example = "Moorland (HMP & YOI)", required = false)
  var prisonName: String? = null,
  @param:Schema(description = "Session Template Reference", example = "v9d.7ed.7u", required = false)
  val sessionTemplateReference: String? = null,
  @param:Schema(description = "Visit Room", example = "Visits Main Hall", required = true)
  @field:NotBlank
  val visitRoom: String,
  @param:Schema(description = "Visit Type", example = "SOCIAL", required = true)
  val visitType: VisitType,
  @param:Schema(description = "Visit Status", example = "RESERVED", required = true)
  val visitStatus: VisitStatus,
  @param:Schema(description = "Visit Sub Status", example = "AUTO_APPROVED", required = true)
  val visitSubStatus: VisitSubStatus,
  @param:Schema(description = "Outcome Status", example = "VISITOR_CANCELLED", required = false)
  val outcomeStatus: OutcomeStatus?,
  @param:Schema(description = "Visit Restriction", example = "OPEN", required = true)
  val visitRestriction: VisitRestriction,
  @param:Schema(description = "The date and time of the visit", example = "2018-12-01T13:45:00", required = true)
  @field:NotBlank
  val startTimestamp: LocalDateTime,
  @param:Schema(description = "The finishing date and time of the visit", example = "2018-12-01T13:45:00", required = true)
  @field:NotBlank
  val endTimestamp: LocalDateTime,
  @param:Schema(description = "Visit Notes", required = false)
  val visitNotes: List<VisitNoteDto>? = listOf(),
  @param:Schema(description = "Contact associated with the visit", required = false)
  val visitContact: ContactDto? = null,
  @param:Schema(description = "List of visitors associated with the visit", required = false)
  val visitors: List<VisitorDto>? = listOf(),
  @param:Schema(description = "Additional support associated with the visit", required = false)
  val visitorSupport: VisitorSupportDto? = null,
  @param:Schema(description = "The visit created date and time", example = "2018-12-01T13:45:00", required = true)
  @field:NotBlank
  val createdTimestamp: LocalDateTime,
  @param:Schema(description = "The visit modified date and time", example = "2018-12-01T13:45:00", required = true)
  @field:NotBlank
  val modifiedTimestamp: LocalDateTime,
  @param:Schema(description = "Date the visit was first booked or migrated", example = "2018-12-01T13:45:00", required = false)
  val firstBookedDateTime: LocalDateTime? = null,
  @param:Schema(description = "External system details associated with the visit")
  val visitExternalSystemDetails: VisitExternalSystemDetails?,
)

data class VisitExternalSystemDetails(
  @param:Schema(description = "Client name", example = "client_name")
  val clientName: String?,
  @param:Schema(description = "Client visit reference", example = "Reference ID in the client system")
  val clientVisitReference: String?,
)
