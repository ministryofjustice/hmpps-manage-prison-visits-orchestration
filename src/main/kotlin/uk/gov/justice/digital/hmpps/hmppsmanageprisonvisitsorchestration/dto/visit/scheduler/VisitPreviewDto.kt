package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.visitscheduler.dto.sessions.SessionTimeSlotDto

/**
 * A visit's preview with minimum visit details.
 */
data class VisitPreviewDto(
  @Schema(required = true, description = "Prisoner Number", example = "A1234AA")
  @NotNull
  val prisonerId: String,

  @Schema(description = "First name of the prisoner", example = "John", required = true)
  @NotNull
  val firstName: String,

  @Schema(description = "Last name of the prisoner", example = "Smith", required = true)
  @NotNull
  val lastName: String,

  @Schema(description = "Visit reference", example = "dp-we-rs-te", required = true)
  @NotNull
  val visitReference: String,

  @Schema(description = "Number of visitors added to the visit", example = "10", required = true)
  @NotNull
  val visitorCount: Int,

  @Schema(description = "Timeslot for the visit", required = true)
  @NotNull
  val visitTimeSlot: SessionTimeSlotDto,
) {
  constructor(prisonerId: String, visitReference: String, visitorCount: Int, visitTimeSlot: SessionTimeSlotDto) : this(prisonerId, prisonerId, prisonerId, visitReference, visitorCount, visitTimeSlot)
}
