package uk.gov.justice.digital.hmpps.orchestration.dto.visit.scheduler.visitnotification

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Visit notification event details.")
data class VisitNotificationEventDto(
  @param:Schema(description = "Notification Event Type", required = true)
  val type: NotificationEventType,
  @param:Schema(description = "Notification Event Reference", example = "aa-bb-cc-dd", required = true)
  val notificationEventReference: String,
  @param:Schema(description = "Created date and time", example = "2018-12-01T13:45:00", required = true)
  val createdDateTime: LocalDateTime,
  @param:Schema(description = "Additional data, empty list if no additional data associated", required = true)
  val additionalData: List<VisitNotificationEventAttributeDto>,
)
