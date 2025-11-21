package uk.gov.justice.digital.hmpps.visits.orchestration.service.listeners.notifiers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.visits.orchestration.service.listeners.events.DomainEvent
import uk.gov.justice.digital.hmpps.visits.orchestration.service.listeners.events.additionalinfo.PersonRestrictionUpsertedInfo

const val PERSON_RESTRICTION_UPSERTED_TYPE = "prison-offender-events.prisoner.person-restriction.upserted"

@Component(value = PERSON_RESTRICTION_UPSERTED_TYPE)
class PersonRestrictionUpsertedNotifier : EventNotifier() {
  override fun processEvent(domainEvent: DomainEvent) {
    val info = getAdditionalInfo(domainEvent, PersonRestrictionUpsertedInfo::class.java)
    getVisitSchedulerService().processPersonRestrictionUpserted(info)
  }

  override fun isProcessableEvent(domainEvent: DomainEvent): Boolean {
    // TODO - implement
    return true
  }
}
