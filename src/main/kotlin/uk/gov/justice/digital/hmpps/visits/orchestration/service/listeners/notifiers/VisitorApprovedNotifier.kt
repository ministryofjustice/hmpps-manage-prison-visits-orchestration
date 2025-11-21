package uk.gov.justice.digital.hmpps.visits.orchestration.service.listeners.notifiers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.visits.orchestration.service.listeners.events.DomainEvent
import uk.gov.justice.digital.hmpps.visits.orchestration.service.listeners.events.additionalinfo.VisitorApprovedUnapprovedInfo

const val VISITOR_APPROVED_EVENT_TYPE = "prison-offender-events.prisoner.contact-approved"

@Component(value = VISITOR_APPROVED_EVENT_TYPE)
class VisitorApprovedNotifier : EventNotifier() {
  override fun processEvent(domainEvent: DomainEvent) {
    val info: VisitorApprovedUnapprovedInfo = getAdditionalInfo(domainEvent, VisitorApprovedUnapprovedInfo::class.java)
    LOG.debug("Enter VisitorApprovedNotifier Info: {}", info)
    getVisitSchedulerService().processVisitorApproved(info)
  }

  override fun isProcessableEvent(domainEvent: DomainEvent): Boolean {
    // TODO - implement
    return true
  }
}
