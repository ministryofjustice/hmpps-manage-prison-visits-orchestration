package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.DomainEvent
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.SQSMessage

const val DELETE_INCENTIVES_EVENT_TYPE = "incentives.iep-review.deleted"

@Component(value = DELETE_INCENTIVES_EVENT_TYPE)
class PrisonerIncentivesDeletedNotifier(private val objectMapper: ObjectMapper) : EventNotifier(objectMapper) {

  override fun processEvent(sqsMessage: SQSMessage) {
    val domainEvent: DomainEvent = objectMapper.readValue(sqsMessage.message)
  }
}
