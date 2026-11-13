package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events

import tools.jackson.module.kotlin.jacksonObjectMapper
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.CancelVisitFromExternalSystemDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.CreateVisitFromExternalSystemDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.UpdateVisitFromExternalSystemDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType

data class VisitFromExternalSystemEvent(
  val messageId: String,
  val eventType: String,
  val description: String? = null,
  val messageAttributes: Map<String, Any?> = emptyMap(),
  val who: String? = null,
) {
  fun toCreateVisitFromExternalSystemDto(): CreateVisitFromExternalSystemDto {
    val mapper = jacksonObjectMapper()
    return mapper.convertValue(this.messageAttributes, CreateVisitFromExternalSystemDto::class.java).also {
      validateActionedBy(it.userType, it.actionedBy)
    }
  }
  fun toUpdateVisitFromExternalSystemDto(): UpdateVisitFromExternalSystemDto {
    val mapper = jacksonObjectMapper()
    return mapper.convertValue(this.messageAttributes, UpdateVisitFromExternalSystemDto::class.java).also {
      require(it.clientName.isNotBlank()) { "clientName must be provided for external visit updates" }
      validateActionedBy(it.userType, it.actionedBy)
    }
  }
  fun toCancelVisitFromExternalSystemDto(): CancelVisitFromExternalSystemDto {
    val mapper = jacksonObjectMapper()
    return mapper.convertValue(this.messageAttributes, CancelVisitFromExternalSystemDto::class.java).also {
      require(it.clientName.isNotBlank()) { "clientName must be provided for external visit cancellations" }
    }
  }

  private fun validateActionedBy(userType: UserType?, actionedBy: String?) {
    require((userType != UserType.STAFF && userType != UserType.PUBLIC) || !actionedBy.isNullOrBlank()) {
      "actionedBy must be provided for STAFF or PUBLIC users"
    }
  }
}
