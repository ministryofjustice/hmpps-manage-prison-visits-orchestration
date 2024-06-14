package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.springframework.context.ApplicationEvent
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.deserializers.RawJsonDeserializer

data class DomainEvent(
  val eventType: String,

  @JsonDeserialize(using = RawJsonDeserializer::class)
  val additionalInformation: String,

  @JsonProperty("description")
  val description: String? = null,
) : ApplicationEvent("")
