package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.DomainEvent
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.SQSMessage

@Component(value = "incentives.iep-review.deleted")
class PrisonerIncentivesDeletedNotifier(private val objectMapper: ObjectMapper) : EventNotifier(objectMapper) {

  override fun processEvent(sqsMessage: SQSMessage) {
    val domainEvent: DomainEvent = objectMapper.readValue(sqsMessage.message)
  }
}
