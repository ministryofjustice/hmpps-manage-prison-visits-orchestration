package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.VisitNotificationEventAttributeDto
import java.time.LocalDate

class OrchestrationPrisonerVisitsNotificationDto(
  @Schema(description = "Prisoner Number", example = "AF34567G", required = true)
  @field:NotBlank
  val prisonerNumber: String,
  @Schema(description = "Booked by user name ", example = "SMITH1", required = true)
  @field:NotBlank
  val bookedByUserName: String,
  @Schema(description = "The date of the visit", example = "2023-11-08", required = true)
  @field:NotBlank
  val visitDate: LocalDate,
  @Schema(description = "Visit Booking Reference", example = "v9-d7-ed-7u", required = true)
  val bookingReference: String,
  @Schema(description = "Booked by name", example = "John Smith", required = true)
  @field:NotBlank
  val bookedByName: String,
  @Schema(description = "A list of all notification attributes for a given visit", required = false)
  val notificationEventAttributes: List<VisitNotificationEventAttributeDto>,
)
