package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.DomainEvent
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.PersonRestrictionDeletedInfo

const val PERSON_RESTRICTION_DELETED_TYPE = "prison-offender-events.prisoner.person-restriction.deleted"

@Component(value = PERSON_RESTRICTION_DELETED_TYPE)
class PersonRestrictionDeletedNotifier : EventNotifier() {
  override fun processEvent(domainEvent: DomainEvent) {
    val info = getAdditionalInfo(domainEvent, PersonRestrictionDeletedInfo::class.java)
    getVisitSchedulerService().processPersonRestrictionDeleted(info)
  }
}
