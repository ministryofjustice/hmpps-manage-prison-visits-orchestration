package uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.visitnotification

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.ActionedByDto
import java.time.LocalDate

class PrisonerVisitsNotificationDto(
  @param:Schema(description = "Prisoner Number", example = "AF34567G", required = true)
  @field:NotBlank
  val prisonerNumber: String,
  @param:Schema(description = "username of the last user to action the visit booking (E.g. book, update)", example = "SMITH1", required = true)
  @field:NotBlank
  val lastActionedBy: ActionedByDto,
  @param:Schema(description = "The date of the visit", example = "2023-11-08", required = true)
  @field:NotBlank
  val visitDate: LocalDate,
  @param:Schema(description = "Visit Booking Reference", example = "v9-d7-ed-7u", required = true)
  val bookingReference: String,
  @param:Schema(description = "A list of all notification attributes for a given visit", required = false)
  val notificationEventAttributes: List<VisitNotificationEventAttributeDto>,
)
