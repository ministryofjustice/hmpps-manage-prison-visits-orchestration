package uk.gov.justice.digital.hmpps.prison.visits.orchestration.service.listeners.notifiers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.service.listeners.events.DomainEvent
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.service.listeners.events.additionalinfo.VisitorRestrictionUpsertedInfo

const val VISITOR_RESTRICTION_UPSERTED_TYPE = "prison-offender-events.visitor.restriction.upserted"

@Component(value = VISITOR_RESTRICTION_UPSERTED_TYPE)
class VisitorRestrictionChangedNotifier : EventNotifier() {
  override fun processEvent(domainEvent: DomainEvent) {
    val info: VisitorRestrictionUpsertedInfo = getAdditionalInfo(domainEvent, VisitorRestrictionUpsertedInfo::class.java)
    LOG.debug("Enter VisitorRestrictionUpsertedInfo Info: {}", info)
    getVisitSchedulerService().processVisitorRestrictionUpserted(info)
  }

  override fun isProcessableEvent(domainEvent: DomainEvent): Boolean {
    // TODO - implement
    return true
  }
}
