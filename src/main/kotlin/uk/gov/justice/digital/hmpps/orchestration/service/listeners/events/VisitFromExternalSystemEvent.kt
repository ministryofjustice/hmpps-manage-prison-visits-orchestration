package uk.gov.justice.digital.hmpps.orchestration.service.listeners.events

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import uk.gov.justice.digital.hmpps.orchestration.dto.visit.scheduler.CancelVisitFromExternalSystemDto
import uk.gov.justice.digital.hmpps.orchestration.dto.visit.scheduler.CreateVisitFromExternalSystemDto
import uk.gov.justice.digital.hmpps.orchestration.dto.visit.scheduler.UpdateVisitFromExternalSystemDto

data class VisitFromExternalSystemEvent(
  val messageId: String,
  val eventType: String,
  val description: String? = null,
  val messageAttributes: Map<String, Any?> = emptyMap(),
  val who: String? = null,
) {
  fun toCreateVisitFromExternalSystemDto(): CreateVisitFromExternalSystemDto {
    val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build()).registerModule(JavaTimeModule())
    return mapper.convertValue(this.messageAttributes, CreateVisitFromExternalSystemDto::class.java)
  }
  fun toUpdateVisitFromExternalSystemDto(): UpdateVisitFromExternalSystemDto {
    val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build()).registerModule(JavaTimeModule())
    return mapper.convertValue(this.messageAttributes, UpdateVisitFromExternalSystemDto::class.java)
  }
  fun toCancelVisitFromExternalSystemDto(): CancelVisitFromExternalSystemDto {
    val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
    return mapper.convertValue(this.messageAttributes, CancelVisitFromExternalSystemDto::class.java)
  }
}
