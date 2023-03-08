package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.SQSMessage

const val UPDATED_INCENTIVES_EVENT_TYPE = "incentives.iep-review.updated"

@Component(value = UPDATED_INCENTIVES_EVENT_TYPE)
class PrisonerIncentivesUpdatedNotifier(private val objectMapper: ObjectMapper) : EventNotifier(objectMapper) {

  override fun processEvent(sqsMessage: SQSMessage) {
    val domainEvent = getDomainEvent(sqsMessage)
  }
}
