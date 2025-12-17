package uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.orchestration

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.visit.scheduler.visitnotification.VisitNotificationEventDto
import java.time.LocalDate

class OrchestrationVisitNotificationsDto(
  @param:Schema(description = "Visit Booking Reference", example = "v9-d7-ed-7u", required = true)
  val visitReference: String,
  @param:Schema(description = "Prisoner Number", example = "AF34567G", required = true)
  val prisonerNumber: String,
  @param:Schema(description = "username of the last user to book the visit", example = "SMITH1", required = true)
  val bookedByUserName: String,
  @param:Schema(description = "The date of the visit", example = "2023-11-08", required = true)
  val visitDate: LocalDate,
  @param:Schema(description = "Booked by name", example = "John Smith", required = true)
  @field:NotBlank
  val bookedByName: String,
  @param:Schema(description = "A list of filtered notifications for a visit", required = true)
  val notifications: List<VisitNotificationEventDto>,
)
