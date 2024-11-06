package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

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

  @Schema(description = "Date the visit was first booked or migrated", example = "2018-12-01T13:45:00", required = false)
  val firstBookedDateTime: LocalDateTime? = null,
) {
  constructor(prisonerId: String, visitReference: String, visitorCount: Int, visitTimeSlot: SessionTimeSlotDto, firstBookedDateTime: LocalDateTime?) :
    this(
      prisonerId = prisonerId,
      firstName = prisonerId,
      lastName = prisonerId,
      visitReference = visitReference,
      visitorCount = visitorCount,
      visitTimeSlot = visitTimeSlot,
      firstBookedDateTime = firstBookedDateTime,
    )
}
