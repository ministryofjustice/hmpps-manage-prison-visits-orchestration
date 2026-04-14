package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.DomainEvent

const val PRISONER_CONTACT_RESTRICTION_UPDATED_TYPE = "contacts-api.prisoner-contact-restriction.updated"

@Component(value = PRISONER_CONTACT_RESTRICTION_UPDATED_TYPE)
class PrisonerContactRestrictionUpdatedNotifier : EventNotifier() {

  override fun processEvent(domainEvent: DomainEvent) {
    val contactRestrictionUpdatedInfo = getContactRestrictionUpsertedInfo(domainEvent)
    if (contactRestrictionUpdatedInfo.prisonerNumber == null || contactRestrictionUpdatedInfo.contactId == null) {
      LOG.error("Prisoner or Contact ID not found in contact restriction updated event, {}", domainEvent)
      throw RuntimeException("Prisoner or Contact ID not found in event, cannot process ContactRestrictionUpdated event")
    }

    LOG.debug("Enter ContactRestrictionUpdatedNotifier Info: {}", contactRestrictionUpdatedInfo)
    getVisitSchedulerService().processPrisonerContactRestrictionUpserted(contactRestrictionUpdatedInfo)
  }

  override fun isProcessableEvent(domainEvent: DomainEvent): Boolean = true
}
