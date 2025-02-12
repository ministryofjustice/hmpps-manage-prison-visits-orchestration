package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.whereabouts

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Scheduled Event
 */
data class ScheduledEventDto(
  @Schema(required = true, description = "Offender booking id")
  val bookingId: Long,

  @Schema(description = "Class of event")
  val eventClass: String? = null,

  @Schema(description = "Activity id if any. Used to attend or pay an activity.")
  val eventId: Long? = null,

  @Schema(description = "Status of event")
  val eventStatus: String? = null,

  @Schema(description = "Type of scheduled event (as a code)")
  val eventType: String? = null,

  @Schema(description = "Description of scheduled event type")
  val eventTypeDesc: String? = null,

  @Schema(description = "Sub type (or reason) of scheduled event (as a code)")
  val eventSubType: String? = null,

  @Schema(description = "Description of scheduled event sub type")
  val eventSubTypeDesc: String? = null,

  @Schema(description = "Date on which event occurs")
  val eventDate: LocalDate? = null,

  @Schema(description = "Date and time at which event starts")
  val startTime: LocalDateTime? = null,

  @Schema(description = "Date and time at which event ends")
  val endTime: LocalDateTime? = null,

  @Schema(description = "Location at which event takes place (could be an internal location, agency or external address).")
  val eventLocation: String? = null,

  @Schema(description = "Id of an internal event location")
  val eventLocationId: Long? = null,

  @Schema(description = "The agency ID for the booked internal location", example = "WWI")
  val agencyId: String? = null,

  @Schema(description = "Code identifying underlying source of event data")
  val eventSource: String? = null,

  @Schema(description = "Source-specific code for the type or nature of the event")
  val eventSourceCode: String? = null,

  @Schema(description = "Source-specific description for type or nature of the event")
  val eventSourceDesc: String? = null,

  @Schema(description = "Activity attendance, possible values are the codes in the 'PS_PA_OC' reference domain.")
  val eventOutcome: String? = null,

  @Schema(description = "Activity performance, possible values are the codes in the 'PERFORMANCE' reference domain.")
  val performance: String? = null,

  @Schema(description = "Activity no-pay reason.")
  val outcomeComment: String? = null,

  @Schema(description = "Activity paid flag.")
  val paid: Boolean? = null,

  @Schema(description = "Amount paid per activity session in pounds")
  val payRate: BigDecimal? = null,

  @Schema(description = "The code for the activity location")
  val locationCode: String? = null,

  @Schema(description = "Staff member who created the appointment")
  val createUserId: String? = null,
) {
  override fun toString(): String = "Event with booking id - $bookingId, type - $eventTypeDesc ($eventType), subtype - $eventSubTypeDesc ($eventSubType), dated - $eventDate from - ${startTime?.toLocalTime()} to - ${endTime?.toLocalTime()}"
}
