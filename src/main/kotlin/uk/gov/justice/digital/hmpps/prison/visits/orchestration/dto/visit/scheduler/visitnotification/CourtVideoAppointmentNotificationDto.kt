package uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.visit.scheduler.visitnotification

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.service.listeners.events.additionalinfo.CourtVideoAppointmentInfo

data class CourtVideoAppointmentNotificationDto(
  @field:Schema(description = "The appointment ID", example = "23598237", required = true)
  @field:NotBlank
  val appointmentInstanceId: String,
) {

  constructor(info: CourtVideoAppointmentInfo) : this(
    info.appointmentInstanceId,
  )
}
