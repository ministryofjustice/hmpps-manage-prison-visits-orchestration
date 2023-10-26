package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.DomainEvent
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.VisitorRestrictionChangeInfo

const val VISITOR_RESTRICTION_CHANGED_TYPE = "prison-offender-events.visitor.restriction.changed"

@Component(value = VISITOR_RESTRICTION_CHANGED_TYPE)
class VisitorRestrictionChangedNotifier : EventNotifier() {
  override fun processEvent(domainEvent: DomainEvent) {
    val info: VisitorRestrictionChangeInfo = getAdditionalInfo(domainEvent, VisitorRestrictionChangeInfo::class.java)
    LOG.debug("Enter VisitorRestrictionChangeInfo Info:$info")
    getVisitSchedulerService().processVisitorRestrictionChange(info)
  }
}
