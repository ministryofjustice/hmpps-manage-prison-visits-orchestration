package uk.gov.justice.digital.hmpps.visits.orchestration.service.listeners.notifiers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.visits.orchestration.service.listeners.events.DomainEvent
import uk.gov.justice.digital.hmpps.visits.orchestration.service.listeners.events.additionalinfo.NonAssociationChangedInfo

@Component(value = PRISONER_NON_ASSOCIATION_DETAIL_CREATED_TYPE)
class PrisonerNonAssociationNotifierCreatedNotifier : PrisonerNonAssociationNotifier()

@Component(value = PRISONER_NON_ASSOCIATION_DETAIL_AMENDED_TYPE)
class PrisonerNonAssociationNotifierAmendedNotifier : PrisonerNonAssociationNotifier()

@Component(value = PRISONER_NON_ASSOCIATION_DETAIL_CLOSED_TYPE)
class PrisonerNonAssociationNotifierClosedNotifier : PrisonerNonAssociationNotifier()

@Component(value = PRISONER_NON_ASSOCIATION_DETAIL_DELETED_TYPE)
class PrisonerNonAssociationNotifierDeletedNotifier : PrisonerNonAssociationNotifier()

abstract class PrisonerNonAssociationNotifier : EventNotifier() {
  override fun processEvent(domainEvent: DomainEvent) {
    val type = NonAssociationDomainEventType.getFromValue(domainEvent.eventType)!!
    val additionalInfo = getAdditionalInfo(domainEvent, NonAssociationChangedInfo::class.java)
    LOG.debug("Enter PrisonerNonAssociationNotifier ${type.value} Info:$additionalInfo")
    getVisitSchedulerService().processNonAssociations(additionalInfo, type)
  }

  override fun isProcessableEvent(domainEvent: DomainEvent): Boolean {
    // TODO - implement
    return true
  }
}
