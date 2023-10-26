package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.DomainEvent

const val INSERTED_INCENTIVES_EVENT_TYPE = "incentives.iep-review.inserted"

@Component(value = INSERTED_INCENTIVES_EVENT_TYPE)
class PrisonerIncentivesInsertedNotifier(private val objectMapper: ObjectMapper) : EventNotifier(objectMapper) {

  override fun processEvent(domainEvent: DomainEvent) {
  }
}
