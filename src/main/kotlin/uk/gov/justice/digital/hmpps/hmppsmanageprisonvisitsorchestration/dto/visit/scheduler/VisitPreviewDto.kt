package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitRestriction
import java.time.LocalDateTime

/**
 * A visit's preview with minimum visit details.
 */
data class VisitPreviewDto internal constructor(
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

  @Schema(description = "Date the visit was first booked or migrated", example = "2018-12-01T13:45:00", required = true)
  val firstBookedDateTime: LocalDateTime,

  @Schema(description = "Visit Restriction", example = "OPEN", required = true)
  val visitRestriction: VisitRestriction,
) {
  constructor(visit: VisitDto, prisoner: PrisonerDto) :
    this(
      prisonerId = visit.prisonerId,
      firstName = prisoner.firstName,
      lastName = prisoner.lastName,
      visitReference = visit.reference,
      visitorCount = visit.visitors?.size ?: 0,
      visitTimeSlot = SessionTimeSlotDto(visit.startTimestamp.toLocalTime(), visit.endTimestamp.toLocalTime()),
      firstBookedDateTime = visit.firstBookedDateTime ?: visit.createdTimestamp,
      visitRestriction = visit.visitRestriction,
    )

  constructor(visit: VisitDto) :
    this(
      prisonerId = visit.prisonerId,
      firstName = visit.prisonerId,
      lastName = visit.prisonerId,
      visitReference = visit.reference,
      visitorCount = visit.visitors?.size ?: 0,
      visitTimeSlot = SessionTimeSlotDto(visit.startTimestamp.toLocalTime(), visit.endTimestamp.toLocalTime()),
      firstBookedDateTime = visit.firstBookedDateTime ?: visit.createdTimestamp,
      visitRestriction = visit.visitRestriction,
    )
}
