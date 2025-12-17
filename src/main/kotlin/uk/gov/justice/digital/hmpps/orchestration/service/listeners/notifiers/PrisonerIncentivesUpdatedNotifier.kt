package uk.gov.justice.digital.hmpps.orchestration.service.listeners.notifiers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.orchestration.service.listeners.events.DomainEvent

const val UPDATED_INCENTIVES_EVENT_TYPE = "incentives.iep-review.updated"

@Component(value = UPDATED_INCENTIVES_EVENT_TYPE)
class PrisonerIncentivesUpdatedNotifier : EventNotifier() {

  override fun processEvent(domainEvent: DomainEvent) {
  }

  override fun isProcessableEvent(domainEvent: DomainEvent): Boolean {
    // TODO - implement
    return true
  }
}
