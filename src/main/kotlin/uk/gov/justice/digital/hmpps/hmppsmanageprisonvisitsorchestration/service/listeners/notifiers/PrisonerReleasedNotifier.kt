package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.DomainEvent
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.PrisonerReleasedInfo

const val PRISONER_RELEASED_TYPE = "prison-offender-events.prisoner.released"

@Component(value = PRISONER_RELEASED_TYPE)
class PrisonerReleasedNotifier : EventNotifier() {
  override fun processEvent(domainEvent: DomainEvent) {
    val additionalInfo = getAdditionalInfo(domainEvent, PrisonerReleasedInfo::class.java)
    LOG.debug("Enter PrisonerReleasedInfo Info:$additionalInfo")
    getVisitSchedulerService().processPrisonerReleased(additionalInfo)
  }
}
