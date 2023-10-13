package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.DomainEvent

const val UPDATED_INCENTIVES_EVENT_TYPE = "incentives.iep-review.updated"

@Component(value = UPDATED_INCENTIVES_EVENT_TYPE)
class PrisonerIncentivesUpdatedNotifier : EventNotifier() {

  override fun processEvent(domainEvent: DomainEvent) {
  }
}
