package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.application

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.ContactDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitNoteDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitorDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitorSupportDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.ApplicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitRestriction
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitType
import java.time.LocalDateTime

@Schema(description = "Visit")
data class ApplicationDto(
  @Schema(description = "reference", example = "v9-d7-ed-7u", required = true)
  val reference: String,
  @Schema(description = "session template Reference", example = "dfs-wjs-eqr", required = false)
  val sessionTemplateReference: String? = null,
  @Schema(description = "Prisoner Id", example = "AF34567G", required = true)
  val prisonerId: String,
  @Schema(description = "Prison Id", example = "MDI", required = true)
  @JsonProperty("prisonId")
  @JsonAlias("prisonCode")
  val prisonCode: String,
  @Schema(description = "Visit Type", example = "SOCIAL", required = true)
  val visitType: VisitType,
  @Schema(description = "Visit Restriction", example = "OPEN", required = true)
  val visitRestriction: VisitRestriction,
  @Schema(description = "The date and time of the visit", example = "2018-12-01T13:45:00", required = true)
  @field:NotNull
  val startTimestamp: LocalDateTime,
  @Schema(description = "The finishing date and time of the visit", example = "2018-12-01T13:45:00", required = true)
  @field:NotNull
  val endTimestamp: LocalDateTime,
  @Schema(description = "Visit Notes", required = false)
  val visitNotes: List<VisitNoteDto> = listOf(),
  @Schema(description = "Contact associated with the visit", required = false)
  val visitContact: ContactDto? = null,
  @Schema(description = "List of visitors associated with the visit", required = false)
  val visitors: List<VisitorDto> = listOf(),
  @Schema(description = "Additional support associated with the application", required = false)
  val visitorSupport: VisitorSupportDto? = null,
  @Schema(description = "The visit created date and time", example = "2018-12-01T13:45:00", required = true)
  @field:NotNull
  val createdTimestamp: LocalDateTime,
  @Schema(description = "The visit modified date and time", example = "2018-12-01T13:45:00", required = true)
  @field:NotNull
  val modifiedTimestamp: LocalDateTime,
  @Schema(description = "Is the application reserved", example = "true", required = true)
  @field:NotNull
  val reserved: Boolean,
  @Schema(description = "Status of the application", example = "IN_PROGRESS", required = true)
  @field:NotNull
  val applicationStatus: ApplicationStatus,
  @Schema(description = "User type", example = "STAFF", required = true)
  @field:NotNull
  val userType: UserType,
)
