package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.DomainEvent

const val CONTACT_RESTRICTION_CREATED_TYPE = "contacts-api.contact-restriction.created"

@Component(value = CONTACT_RESTRICTION_CREATED_TYPE)
class ContactRestrictionCreatedNotifier : EventNotifier() {

  override fun processEvent(domainEvent: DomainEvent) {
    val contactRestrictionCreatedInfo = getContactRestrictionUpsertedInfo(domainEvent)
    if (contactRestrictionCreatedInfo.contactId == null) {
      LOG.error("Contact ID not found in contact restriction created event, {}", domainEvent)
      throw RuntimeException("Contact ID not found in event, cannot process ContactRestrictionCreated event")
    }

    LOG.debug("Enter ContactRestrictionCreatedNotifier Info: {}", contactRestrictionCreatedInfo)
    getVisitSchedulerService().processContactRestrictionUpserted(contactRestrictionCreatedInfo)
  }

  override fun isProcessableEvent(domainEvent: DomainEvent): Boolean = true
}
