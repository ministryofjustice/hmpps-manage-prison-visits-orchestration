package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import org.springframework.http.MediaType

class MockUtils {
  companion object {
    private val objectMapper: ObjectMapper = ObjectMapper()
      .registerModules(JavaTimeModule(), kotlinModule())
      .setSerializationInclusion(JsonInclude.Include.NON_NULL)

    fun getJsonString(obj: Any): String = objectMapper.writer().writeValueAsString(obj)

    fun createJsonResponseBuilder(): ResponseDefinitionBuilder = WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
  }
}
