package uk.gov.justice.digital.hmpps.prison.visits.orchestration.service.listeners.events.additionalinfo

import jakarta.validation.constraints.NotBlank

data class CourtVideoAppointmentInfo(
  @field:NotBlank
  val appointmentInstanceId: String,

  @field:NotBlank
  val categoryCode: String,
) : EventInfo
