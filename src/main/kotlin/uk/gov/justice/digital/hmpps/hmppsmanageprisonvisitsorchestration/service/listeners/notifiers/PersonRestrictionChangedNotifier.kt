package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.DomainEvent
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.PersonRestrictionChangeInfo

const val PERSON_RESTRICTION_CHANGED_TYPE = "prison-offender-events.prisoner.person-restriction.changed"

@Component(value = PERSON_RESTRICTION_CHANGED_TYPE)
class PersonRestrictionChangedNotifier : EventNotifier() {
  override fun processEvent(domainEvent: DomainEvent) {
    val info = getAdditionalInfo(domainEvent, PersonRestrictionChangeInfo::class.java)
    getVisitSchedulerService().processPersonRestrictionChange(info)
  }
}
