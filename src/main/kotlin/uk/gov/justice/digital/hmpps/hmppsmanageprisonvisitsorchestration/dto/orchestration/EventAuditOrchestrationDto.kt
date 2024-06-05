package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.ApplicationMethodType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.EventAuditType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.validation.NullableNotBlank
import java.time.LocalDateTime

@Schema(description = "Event Audit")
class EventAuditOrchestrationDto(

  @Schema(description = "The type of event", required = true)
  @field:NotNull
  val type: EventAuditType,

  @Schema(description = "What was the application method for this event", required = true)
  @field:NotNull
  val applicationMethodType: ApplicationMethodType,

  @Schema(description = "Actioned by full name", example = "Aled Evans", required = false)
  val actionedByFullName: String? = null,

  @Schema(description = "User type", example = "STAFF", required = false)
  @field:NotNull
  val userType: UserType,

  @Schema(description = "Session template used for this event ", required = false)
  var sessionTemplateReference: String? = null,

  @Schema(description = "Notes added against the event", required = false)
  @NullableNotBlank
  var text: String? = null,

  @Schema(description = "event creat date and time", example = "2018-12-01T13:45:00", required = true)
  @field:NotBlank
  val createTimestamp: LocalDateTime = LocalDateTime.now(),
)
