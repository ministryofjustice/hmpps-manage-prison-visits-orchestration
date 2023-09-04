package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StringDeserializer

data class DomainEvent(
  val eventType: String,

  @JsonDeserialize(using = StringDeserializer::class)
  val additionalInformation: String,
)
