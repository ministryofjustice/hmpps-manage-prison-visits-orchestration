package uk.gov.justice.digital.hmpps.prison.visits.orchestration.service.listeners.notifiers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.service.listeners.events.DomainEvent
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.service.listeners.events.additionalinfo.VisitorApprovedUnapprovedInfo

const val VISITOR_UNAPPROVED_EVENT_TYPE = "prison-offender-events.prisoner.contact-unapproved"

@Component(value = VISITOR_UNAPPROVED_EVENT_TYPE)
class VisitorUnapprovedNotifier : EventNotifier() {
  override fun processEvent(domainEvent: DomainEvent) {
    val info: VisitorApprovedUnapprovedInfo = getAdditionalInfo(domainEvent, VisitorApprovedUnapprovedInfo::class.java)
    LOG.debug("Enter VisitorUnapprovedNotifier Info: {}", info)
    getVisitSchedulerService().processVisitorUnapproved(info)
  }

  override fun isProcessableEvent(domainEvent: DomainEvent): Boolean {
    // TODO - implement
    return true
  }
}
