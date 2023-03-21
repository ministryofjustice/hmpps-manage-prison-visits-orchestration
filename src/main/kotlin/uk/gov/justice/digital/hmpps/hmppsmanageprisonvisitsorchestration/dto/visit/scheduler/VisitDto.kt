package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

@Schema(description = "Visit")
class VisitDto(
  @Schema(description = "Application Reference", example = "dfs-wjs-eqr", required = true)
  val applicationReference: String,
  @Schema(description = "Visit Reference", example = "v9-d7-ed-7u", required = true)
  val reference: String,
  @Schema(description = "Prisoner Id", example = "AF34567G", required = true)
  val prisonerId: String,
  @JsonProperty("prisonId")
  @JsonAlias("prisonCode")
  @Schema(description = "Prison Id", example = "MDI", required = true)
  val prisonCode: String,
  @Schema(description = "Visit Room", example = "A1 L3", required = true)
  val visitRoom: String,
  @Schema(description = "Visit Type", example = "SOCIAL", required = true)
  val visitType: String,
  @Schema(description = "Visit Status", example = "RESERVED", required = true)
  val visitStatus: String,
  @Schema(description = "Outcome Status", example = "VISITOR_CANCELLED", required = false)
  val outcomeStatus: String?,
  @Schema(description = "Visit Restriction", example = "OPEN", required = true)
  val visitRestriction: String,
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
  @Schema(description = "List of additional support associated with the visit", required = false)
  val visitorSupport: List<VisitorSupportDto>? = listOf(),
  @Schema(description = "Created By - user id for the user who created the visit", example = "AB12345A", required = true)
  val createdBy: String,
  @Schema(description = "Updated By - user id for the user who last updated the visit", example = "AB12345A", required = false)
  val updatedBy: String? = null,
  @Schema(description = "Cancelled By - user id for the user who cancelled the visit", example = "AB12345A", required = false)
  val cancelledBy: String? = null,
  @Schema(description = "The visit created date and time", example = "2018-12-01T13:45:00", required = true)
  @field:NotBlank
  val createdTimestamp: LocalDateTime,
  @Schema(description = "The visit modified date and time", example = "2018-12-01T13:45:00", required = true)
  @field:NotBlank
  val modifiedTimestamp: LocalDateTime,
)
