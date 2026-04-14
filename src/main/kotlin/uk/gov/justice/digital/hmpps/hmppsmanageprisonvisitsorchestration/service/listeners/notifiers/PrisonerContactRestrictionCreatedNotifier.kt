package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.DomainEvent

const val PRISONER_CONTACT_RESTRICTION_CREATED_TYPE = "contacts-api.prisoner-contact-restriction.created"

@Component(value = PRISONER_CONTACT_RESTRICTION_CREATED_TYPE)
class PrisonerContactRestrictionCreatedNotifier : EventNotifier() {

  override fun processEvent(domainEvent: DomainEvent) {
    val contactRestrictionCreatedInfo = getContactRestrictionUpsertedInfo(domainEvent)
    if (contactRestrictionCreatedInfo.prisonerNumber == null || contactRestrictionCreatedInfo.contactId == null) {
      LOG.error("Prisoner or Contact ID not found in contact restriction created event, {}", domainEvent)
      throw RuntimeException("Prisoner or Contact ID not found in event, cannot process ContactRestrictionCreated event")
    }

    LOG.debug("Enter ContactRestrictionCreatedNotifier Info: {}", contactRestrictionCreatedInfo)
    getVisitSchedulerService().processPrisonerContactRestrictionUpsert(contactRestrictionCreatedInfo)
  }

  override fun isProcessableEvent(domainEvent: DomainEvent): Boolean = true
}
