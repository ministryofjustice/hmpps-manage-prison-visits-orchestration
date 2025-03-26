package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events

data class VisitFromExternalSystemEvent(
  val messageId: String,
  val eventType: String,
  val description: String? = null,
  val messageAttributes: Map<String, Any?> = emptyMap(),
  val who: String? = null,
)
