package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.application

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.ContactDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitorDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitorSupportDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.CreateApplicationRestriction
import java.time.LocalDate

data class ChangeApplicationDto(
  @Schema(description = "Visit Restriction", example = "OPEN", required = false)
  val applicationRestriction: CreateApplicationRestriction? = null,
  @Schema(description = "Session template reference", example = "v9d.7ed.7u", required = true)
  @field:NotBlank
  val sessionTemplateReference: String,
  @Schema(description = "The date for the visit", example = "2018-12-01", required = true)
  @field:NotNull
  val sessionDate: LocalDate,
  @Schema(description = "Contact associated with the visit", required = false)
  @field:Valid
  val visitContact: ContactDto? = null,
  @Schema(description = "List of visitors associated with the visit", required = false)
  @field:NotNull
  val visitors: Set<@Valid VisitorDto>? = null,
  @Schema(description = "List of additional support associated with the visit", required = false)
  val visitorSupport: Set<@Valid VisitorSupportDto>? = null,
)
