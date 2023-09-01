package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events

data class DomainEvent(
  val eventType: String,
  val additionalInformation: String,
)
