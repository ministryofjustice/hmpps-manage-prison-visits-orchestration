package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events

import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.databind.annotation.JsonDeserialize
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.deserializers.RawJsonDeserializer

data class DomainEvent(
  val eventType: String,

  @param:JsonDeserialize(using = RawJsonDeserializer::class)
  val additionalInformation: String,

  @param:JsonProperty("description")
  val description: String? = null,

  val personReference: PersonReference?,
)

data class PersonReference(
  val identifiers: List<PersonIdentifier>,
)

data class PersonIdentifier(val type: Identifier, val value: String)

@Suppress("unused")
enum class Identifier {
  NOMS,
  NOMIS,
  DPS_CONTACT_ID,
}
