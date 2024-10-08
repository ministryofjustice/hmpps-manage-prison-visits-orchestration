package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.ActionedByDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitorSupportedRestrictionType
import java.time.LocalDate

class PrisonerVisitsNotificationDto(
  @Schema(description = "Prisoner Number", example = "AF34567G", required = true)
  @field:NotBlank
  val prisonerNumber: String,
  @Schema(description = "username of the last user to action the visit booking (E.g. book, update)", example = "SMITH1", required = true)
  @field:NotBlank
  val lastActionedBy: ActionedByDto,
  @Schema(description = "The date of the visit", example = "2023-11-08", required = true)
  @field:NotBlank
  val visitDate: LocalDate,
  @Schema(description = "Visit Booking Reference", example = "v9-d7-ed-7u", required = true)
  val bookingReference: String,
  @Schema(description = "Description of the flagged event", example = "Visitor with id <id> has had restriction <restriction> added", required = false)
  @field:NotBlank
  val description: String? = null,
  @Schema(description = "For visitor specific events, the id of the affected visitor", example = "1234567", required = false)
  val visitorId: Long? = null,
  @Schema(description = "For visitor specific events, the restriction type of the affected visitor", example = "BAN", required = false)
  val visitorRestrictionType: VisitorSupportedRestrictionType? = null,
)
