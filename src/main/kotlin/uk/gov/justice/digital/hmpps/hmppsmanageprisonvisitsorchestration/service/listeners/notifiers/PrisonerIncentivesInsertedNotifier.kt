package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.DomainEvent

const val INSERTED_INCENTIVES_EVENT_TYPE = "incentives.iep-review.inserted"

@Component(value = INSERTED_INCENTIVES_EVENT_TYPE)
class PrisonerIncentivesInsertedNotifier : EventNotifier() {

  override fun processEvent(domainEvent: DomainEvent) {
  }
}
