package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.DomainEvent
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.VisitorRestrictionUpsertedInfo

const val VISITOR_RESTRICTION_UPSERTED_TYPE = "prison-offender-events.visitor.restriction.upserted"

@Component(value = VISITOR_RESTRICTION_UPSERTED_TYPE)
class VisitorRestrictionChangedNotifier : EventNotifier() {
  override fun processEvent(domainEvent: DomainEvent) {
    val info: VisitorRestrictionUpsertedInfo = getAdditionalInfo(domainEvent, VisitorRestrictionUpsertedInfo::class.java)
    LOG.debug("Enter VisitorRestrictionUpsertedInfo Info: {}", info)
    getVisitSchedulerService().processVisitorRestrictionUpserted(info)
  }
}
