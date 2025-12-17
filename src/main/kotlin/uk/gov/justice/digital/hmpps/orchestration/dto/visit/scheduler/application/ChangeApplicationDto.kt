package uk.gov.justice.digital.hmpps.orchestration.dto.visit.scheduler.application

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.orchestration.dto.visit.scheduler.ContactDto
import uk.gov.justice.digital.hmpps.orchestration.dto.visit.scheduler.VisitorDto
import uk.gov.justice.digital.hmpps.orchestration.dto.visit.scheduler.enums.SessionRestriction
import java.time.LocalDate

data class ChangeApplicationDto(
  @param:Schema(description = "Session Restriction", example = "OPEN", required = false)
  val applicationRestriction: SessionRestriction? = null,
  @param:Schema(description = "Session template reference", example = "v9d.7ed.7u", required = true)
  @field:NotBlank
  val sessionTemplateReference: String,
  @param:Schema(description = "The date for the visit", example = "2018-12-01", required = true)
  @field:NotNull
  val sessionDate: LocalDate,
  @param:Schema(description = "Contact associated with the visit", required = false)
  @field:Valid
  val visitContact: ContactDto? = null,
  @param:Schema(description = "List of visitors associated with the visit", required = false)
  @field:NotNull
  val visitors: Set<@Valid VisitorDto>? = null,
  @param:Schema(description = "additional support associated with the visit, if null support will not be updated", required = false)
  @field:Valid
  var visitorSupport: ApplicationSupportDto? = null,
  @param:Schema(description = "allow over booking", required = false)
  @field:NotNull
  val allowOverBooking: Boolean = false,
)
