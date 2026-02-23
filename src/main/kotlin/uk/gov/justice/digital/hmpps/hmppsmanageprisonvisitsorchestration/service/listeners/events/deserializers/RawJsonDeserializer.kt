package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.deserializers

import tools.jackson.core.JacksonException
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ValueDeserializer
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.io.IOException

class RawJsonDeserializer : ValueDeserializer<String>() {
  @Throws(IOException::class, JacksonException::class)
  override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): String {
    val mapper = jacksonObjectMapper()
    val node = jp.readValueAsTree<JsonNode>()
    return mapper.writeValueAsString(node)
  }
}
