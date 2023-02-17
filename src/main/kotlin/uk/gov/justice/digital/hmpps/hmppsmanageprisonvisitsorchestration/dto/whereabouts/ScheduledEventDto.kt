package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.whereabouts

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

/**
 * Scheduled Event
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ScheduledEventDto(
  @Schema(required = true, description = "Offender booking id")
  val bookingId: Long,

  @Schema(required = true, description = "Class of event")
  val eventClass: @NotBlank String? = null,

  @Schema(description = "Activity id if any. Used to attend or pay an activity.")
  val eventId: Long? = null,

  @Schema(required = true, description = "Status of event")
  val eventStatus: @NotBlank String? = null,

  @Schema(required = true, description = "Type of scheduled event (as a code)")
  val eventType: @NotBlank String? = null,

  @Schema(required = true, description = "Description of scheduled event type")
  val eventTypeDesc: @NotBlank String? = null,

  @Schema(required = true, description = "Sub type (or reason) of scheduled event (as a code)")
  val eventSubType: @NotBlank String? = null,

  @Schema(required = true, description = "Description of scheduled event sub type")
  val eventSubTypeDesc: @NotBlank String? = null,

  @Schema(required = true, description = "Date on which event occurs")
  val eventDate: @NotNull LocalDate? = null,

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

  @Schema(required = true, description = "Code identifying underlying source of event data")
  val eventSource: @NotBlank String? = null,

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
)
