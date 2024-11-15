package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.ContactDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitNoteDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitorDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitorSupportDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.OutcomeStatus
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitRestriction
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitStatus
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitType
import java.time.LocalDateTime

@Schema(description = "Visit")
@JsonInclude(JsonInclude.Include.NON_NULL)
class VisitDetailsDto(
  @Schema(description = "Visit Reference", example = "v9-d7-ed-7u", required = true)
  val reference: String,
  @Schema(description = "Prisoner Id", example = "AF34567G", required = true)
  val prisonerId: String,
  @JsonProperty("prisonId")
  @JsonAlias("prisonCode")
  @Schema(description = "Prison Id", example = "MDI", required = true)
  val prisonCode: String,
  @Schema(description = "Visit Type", example = "SOCIAL", required = true)
  val visitType: VisitType,
  @Schema(description = "Visit Status", example = "RESERVED", required = true)
  val visitStatus: VisitStatus,
  @Schema(description = "Outcome Status", example = "VISITOR_CANCELLED", required = false)
  val outcomeStatus: OutcomeStatus?,
  @Schema(description = "Visit Restriction", example = "OPEN", required = true)
  val visitRestriction: VisitRestriction,
  @Schema(description = "The date and time of the visit", example = "2018-12-01T13:45:00", required = true)
  @field:NotBlank
  val startTimestamp: LocalDateTime,
  @Schema(description = "The finishing date and time of the visit", example = "2018-12-01T13:45:00", required = true)
  @field:NotBlank
  val endTimestamp: LocalDateTime,
  @Schema(description = "Visit Notes", required = false)
  val visitNotes: List<VisitNoteDto>? = listOf(),
  @Schema(description = "Contact associated with the visit", required = false)
  val visitContact: ContactDto? = null,
  @Schema(description = "List of visitors associated with the visit", required = false)
  val visitors: List<VisitorDto>? = listOf(),
  @Schema(description = "Additional support associated with the visit", required = false)
  val visitorSupport: VisitorSupportDto? = null,
  @Schema(description = "Date the visit was first booked or migrated", example = "2018-12-01T13:45:00", required = false)
  val firstBookedDateTime: LocalDateTime,
) {
  constructor(visitDto: VisitDto) : this (
    reference = visitDto.reference,
    prisonerId = visitDto.prisonerId,
    prisonCode = visitDto.prisonCode,
    visitType = visitDto.visitType,
    visitStatus = visitDto.visitStatus,
    outcomeStatus = visitDto.outcomeStatus,
    visitRestriction = visitDto.visitRestriction,
    startTimestamp = visitDto.startTimestamp,
    endTimestamp = visitDto.endTimestamp,
    visitNotes = visitDto.visitNotes,
    visitContact = visitDto.visitContact,
    visitors = visitDto.visitors,
    visitorSupport = visitDto.visitorSupport,
    firstBookedDateTime = visitDto.firstBookedDateTime ?: visitDto.createdTimestamp,
  )
}
