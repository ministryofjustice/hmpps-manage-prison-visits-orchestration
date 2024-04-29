package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.application

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.ContactDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitorDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionRestriction
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType
import java.time.LocalDate

open class CreateApplicationDto(
  @Schema(description = "Prisoner Id", example = "AF34567G", required = true)
  @field:NotBlank
  val prisonerId: String,
  @Schema(description = "Session template reference", example = "v9d.7ed.7u", required = true)
  @field:NotBlank
  val sessionTemplateReference: String,
  @Schema(description = "The date for the visit", example = "2018-12-01", required = true)
  @field:NotNull
  val sessionDate: LocalDate,
  @Schema(description = "Session Restriction", example = "OPEN", required = true)
  @field:NotNull
  val applicationRestriction: SessionRestriction,
  @Schema(description = "Contact associated with the visit", required = false)
  @field:Valid
  val visitContact: ContactDto?,
  @Schema(description = "List of visitors associated with the visit", required = true)
  @field:NotEmpty
  var visitors: Set<@Valid VisitorDto>,
  @Schema(description = "additional support associated with the visit, if null support will not be updated", required = false)
  @Valid
  var visitorSupport: ApplicationSupportDto? = null,
  @Schema(description = "User type", example = "STAFF", required = true)
  @field:NotNull
  val userType: UserType,
  @Schema(description = "actioned by (Booker reference - if PUBLIC user type Or User Name - if staff user type)", example = "asd-asd-asd or STAFF_USER", required = true)
  @field:NotBlank
  val actionedBy: String,
  @Schema(description = "allow over booking", required = false)
  @field:NotNull
  val allowOverBooking: Boolean = false,
)
