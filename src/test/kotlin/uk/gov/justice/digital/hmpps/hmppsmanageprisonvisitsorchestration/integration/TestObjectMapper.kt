package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.kotlinModule

object TestObjectMapper {
  val mapper: ObjectMapper =
    JsonMapper.builder()
      .addModule(JavaTimeModule())
      .addModule(kotlinModule())
      .defaultPropertyInclusion(
        JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL),
      )
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
      .build()
}
