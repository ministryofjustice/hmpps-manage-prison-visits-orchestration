package uk.gov.justice.digital.hmpps.orchestration.service.listeners.events

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import uk.gov.justice.digital.hmpps.orchestration.service.listeners.events.deserializers.RawJsonDeserializer

data class DomainEvent(
  val eventType: String,

  @param:JsonDeserialize(using = RawJsonDeserializer::class)
  val additionalInformation: String,

  @param:JsonProperty("description")
  val description: String? = null,
)
