package uk.gov.justice.digital.hmpps.visits.orchestration.service.listeners.notifiers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.visits.orchestration.service.listeners.events.DomainEvent

const val DELETE_INCENTIVES_EVENT_TYPE = "incentives.iep-review.deleted"

@Component(value = DELETE_INCENTIVES_EVENT_TYPE)
class PrisonerIncentivesDeletedNotifier : EventNotifier() {

  override fun processEvent(domainEvent: DomainEvent) {
  }

  override fun isProcessableEvent(domainEvent: DomainEvent): Boolean {
    // TODO - implement
    return true
  }
}
