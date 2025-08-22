package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo

import jakarta.validation.constraints.NotBlank

data class CourtVideoAppointmentInfo(
  @field:NotBlank
  val appointmentInstanceId: String,
) : EventInfo
