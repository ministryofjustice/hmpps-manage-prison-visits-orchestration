package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.DomainEvent

const val CONTACT_RESTRICTION_UPDATED_TYPE = "contacts-api.contact-restriction.updated"

@Component(value = CONTACT_RESTRICTION_UPDATED_TYPE)
class ContactRestrictionUpdatedNotifier : EventNotifier() {

  override fun processEvent(domainEvent: DomainEvent) {
    val contactRestrictionUpdatedInfo = getContactRestrictionUpsertedInfo(domainEvent)
    if (contactRestrictionUpdatedInfo.contactId == null) {
      LOG.error("Contact ID not found in contact restriction updated event, {}", domainEvent)
      throw RuntimeException("Contact ID not found in event, cannot process ContactRestrictionUpdated event")
    }

    LOG.debug("Enter ContactRestrictionUpdatedNotifier Info: {}", contactRestrictionUpdatedInfo)
    getVisitSchedulerService().processContactRestrictionUpserted(contactRestrictionUpdatedInfo)
  }

  override fun isProcessableEvent(domainEvent: DomainEvent): Boolean = true
}
