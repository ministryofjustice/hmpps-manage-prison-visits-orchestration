package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.DomainEvent
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.PrisonerMergedInfo

const val PRISONER_MERGED_TYPE = "prison-offender-events.prisoner.merged"

@Component(value = PRISONER_MERGED_TYPE)
class PrisonerMergedNotifier : EventNotifier() {
  override fun processEvent(domainEvent: DomainEvent) {
    val additionalInfo = getAdditionalInfo(domainEvent, PrisonerMergedInfo::class.java)
    LOG.debug("Enter PrisonerReleasedInfo Info: {}", additionalInfo)
    getVisitSchedulerService().processPrisonerMerged(additionalInfo)
  }

  override fun isProcessableEvent(domainEvent: DomainEvent): Boolean = true
}
