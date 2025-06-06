package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.VisitNotificationEventDto
import java.time.LocalDate

class OrchestrationVisitNotificationsDto(
  @Schema(description = "Visit Booking Reference", example = "v9-d7-ed-7u", required = true)
  val visitReference: String,
  @Schema(description = "Prisoner Number", example = "AF34567G", required = true)
  val prisonerNumber: String,
  @Schema(description = "username of the last user to book the visit", example = "SMITH1", required = true)
  val bookedByUserName: String,
  @Schema(description = "The date of the visit", example = "2023-11-08", required = true)
  val visitDate: LocalDate,
  @Schema(description = "Booked by name", example = "John Smith", required = true)
  @field:NotBlank
  val bookedByName: String,
  @Schema(description = "A list of filtered notifications for a visit", required = true)
  val notifications: List<VisitNotificationEventDto>,
)
