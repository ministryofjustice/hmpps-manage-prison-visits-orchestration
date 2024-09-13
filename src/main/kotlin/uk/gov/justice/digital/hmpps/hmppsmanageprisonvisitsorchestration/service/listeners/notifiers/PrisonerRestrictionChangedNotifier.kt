package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.DomainEvent
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.PrisonerRestrictionChangeInfo

const val PRISONER_RESTRICTION_CHANGED_TYPE = "prison-offender-events.prisoner.restriction.changed"

@Component(value = PRISONER_RESTRICTION_CHANGED_TYPE)
class PrisonerRestrictionChangedNotifier : EventNotifier() {
  override fun processEvent(domainEvent: DomainEvent) {
    val additionalInfo = getAdditionalInfo(domainEvent, PrisonerRestrictionChangeInfo::class.java)
    LOG.debug("Enter PrisonerRestrictionChangeInfo Info:$additionalInfo")
    getVisitSchedulerService().processPrisonerRestrictionChange(additionalInfo)
  }

  override fun isProcessableEvent(domainEvent: DomainEvent): Boolean {
    // TODO - implement
    return true
  }
}
