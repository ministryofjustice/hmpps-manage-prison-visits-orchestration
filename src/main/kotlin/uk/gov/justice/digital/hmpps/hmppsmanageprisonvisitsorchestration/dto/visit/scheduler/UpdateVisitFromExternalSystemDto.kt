package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler

import com.fasterxml.jackson.annotation.JsonAlias
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.ApplicationMethodType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitRestriction
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitType
import java.time.LocalDateTime

data class UpdateVisitFromExternalSystemDto(
  val visitReference: String,
  @param:Schema(description = "External system that owns the visit", required = true)
  @field:NotBlank
  val clientName: String,
  @param:Schema(description = "Visit Room", example = "A1", required = true)
  @field:NotBlank
  val visitRoom: String,
  @param:Schema(description = "Visit Type", example = "SOCIAL", required = true)
  @field:NotNull
  val visitType: VisitType,
  @param:Schema(description = "Visit Restriction", example = "OPEN", required = true)
  @field:NotNull
  val visitRestriction: VisitRestriction,
  @param:Schema(description = "The date and time of the visit", example = "2018-12-01T13:45:00", required = true)
  @field:NotNull
  val startTimestamp: LocalDateTime,
  @param:Schema(description = "The finishing date and time of the visit", example = "2018-12-01T13:45:00", required = true)
  @field:NotNull
  val endTimestamp: LocalDateTime,
  @param:Schema(description = "Visit Notes")
  @field:Valid
  val visitNotes: List<VisitNoteDto> = emptyList(),
  @param:Schema(description = "Contact associated with the visit", required = true)
  @field:Valid
  val visitContact: ContactDto,
  @param:Schema(description = "List of visitors associated with the visit", required = false)
  @field:Valid
  val visitors: Set<VisitorDto>? = setOf(),
  @param:Schema(description = "Additional support associated with the visit")
  @field:Valid
  val visitorSupport: VisitorSupportDto? = null,
  @param:Schema(description = "Username or Identifier for user who actioned this request. Also accepts username. Required and nonblank for STAFF or PUBLIC users.", required = false)
  @param:JsonAlias("username")
  val actionedBy: String? = null,
  @param:Schema(description = "application method", required = false)
  val applicationMethodType: ApplicationMethodType? = null,
  @param:Schema(description = "User type for user who actioned this request", required = false)
  val userType: UserType? = null,
)
