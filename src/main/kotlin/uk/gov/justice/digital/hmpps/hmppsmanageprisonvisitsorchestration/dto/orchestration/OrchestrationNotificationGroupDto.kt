package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitorSupportedRestrictionType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.NotificationEventType

class OrchestrationNotificationGroupDto(
  @Schema(description = "notification group Reference", example = "v9*d7*ed*7u", required = true)
  @field:NotBlank
  val reference: String,
  @Schema(description = "notification event type", example = "NON_ASSOCIATION_EVENT", required = true)
  @field:NotNull
  val type: NotificationEventType,
  @Schema(description = "List of details of affected visits", required = true)
  @field:NotEmpty
  val affectedVisits: List<OrchestrationPrisonerVisitsNotificationDto>,
  @Schema(description = "Description of the flagged event", example = "Visitor with id <id> has had restriction <restriction> added", required = false)
  @field:NotBlank
  val description: String? = null,
  @Schema(description = "For visitor specific events, the id of the affected visitor", example = "1234567L", required = false)
  val visitorId: Long? = null,
  @Schema(description = "For visitor specific events, the restriction type of the affected visitor", example = "BAN", required = false)
  val visitorRestrictionType: VisitorSupportedRestrictionType? = null,
)
