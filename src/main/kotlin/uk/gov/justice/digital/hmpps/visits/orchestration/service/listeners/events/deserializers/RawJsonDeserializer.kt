package uk.gov.justice.digital.hmpps.visits.orchestration.service.listeners.events.deserializers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.IOException

class RawJsonDeserializer : JsonDeserializer<String>() {
  @Throws(IOException::class, JsonProcessingException::class)
  override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): String {
    val mapper = jp.codec as ObjectMapper
    val node = mapper.readTree<JsonNode>(jp)
    return mapper.writeValueAsString(node)
  }
}
