package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.NotificationEventAttributeType

data class VisitNotificationEventAttributeDto(
  @Schema(description = "Name of the attribute associated with the notification event", example = "VISITOR_RESTRICTION", required = true)
  @field:NotNull
  val attributeName: NotificationEventAttributeType,

  @Schema(description = "Value of the attribute associated with the notification event", example = "BAN", required = true)
  @field:NotNull
  val attributeValue: String,
)
