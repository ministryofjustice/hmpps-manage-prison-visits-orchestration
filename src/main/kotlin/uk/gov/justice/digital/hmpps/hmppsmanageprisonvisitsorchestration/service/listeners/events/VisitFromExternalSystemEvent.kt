package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events

import tools.jackson.module.kotlin.jacksonObjectMapper
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.CancelVisitFromExternalSystemDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.CreateVisitFromExternalSystemDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.UpdateVisitFromExternalSystemDto

data class VisitFromExternalSystemEvent(
  val messageId: String,
  val eventType: String,
  val description: String? = null,
  val messageAttributes: Map<String, Any?> = emptyMap(),
  val who: String? = null,
) {
  fun toCreateVisitFromExternalSystemDto(): CreateVisitFromExternalSystemDto {
    val mapper = jacksonObjectMapper()
    return mapper.convertValue(this.messageAttributes, CreateVisitFromExternalSystemDto::class.java)
  }
  fun toUpdateVisitFromExternalSystemDto(): UpdateVisitFromExternalSystemDto {
    val mapper = jacksonObjectMapper()
    return mapper.convertValue(this.messageAttributes, UpdateVisitFromExternalSystemDto::class.java)
  }
  fun toCancelVisitFromExternalSystemDto(): CancelVisitFromExternalSystemDto {
    val mapper = jacksonObjectMapper()
    return mapper.convertValue(this.messageAttributes, CancelVisitFromExternalSystemDto::class.java)
  }
}
