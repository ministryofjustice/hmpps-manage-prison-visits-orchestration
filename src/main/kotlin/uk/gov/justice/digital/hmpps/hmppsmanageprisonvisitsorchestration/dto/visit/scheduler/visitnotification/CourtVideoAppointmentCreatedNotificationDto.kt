package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.CourtVideoAppointmentCreatedInfo

data class CourtVideoAppointmentCreatedNotificationDto(
  @field:Schema(description = "The appointment ID", example = "23598237", required = true)
  @field:NotBlank
  val appointmentInstanceId: String,
) {

  constructor(info: CourtVideoAppointmentCreatedInfo) : this(
    info.appointmentInstanceId,
  )
}
