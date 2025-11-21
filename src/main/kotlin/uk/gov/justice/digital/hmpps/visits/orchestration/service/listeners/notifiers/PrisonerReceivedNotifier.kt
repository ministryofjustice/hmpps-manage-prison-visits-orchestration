package uk.gov.justice.digital.hmpps.visits.orchestration.service.listeners.notifiers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.visits.orchestration.service.listeners.events.DomainEvent
import uk.gov.justice.digital.hmpps.visits.orchestration.service.listeners.events.additionalinfo.PrisonerReceivedInfo

const val PRISONER_RECEIVED_TYPE = "prison-offender-events.prisoner.received"

@Component(value = PRISONER_RECEIVED_TYPE)
class PrisonerReceivedNotifier : EventNotifier() {
  override fun processEvent(domainEvent: DomainEvent) {
    val additionalInfo = getAdditionalInfo(domainEvent, PrisonerReceivedInfo::class.java)
    LOG.debug("Enter PrisonerReceivedInfo Info:$additionalInfo")
    getVisitSchedulerService().processPrisonerReceived(additionalInfo)
  }

  override fun isProcessableEvent(domainEvent: DomainEvent): Boolean {
    // TODO - implement
    return true
  }
}
