package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.DomainEvent
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.PrisonerAlertsUpdatedNotificationInfo

const val PRISONER_ALERTS_UPDATED = "prisoner-offender-search.prisoner.alerts-updated"

@Component(value = PRISONER_ALERTS_UPDATED)
class PrisonerAlertsUpdatedNotifier : EventNotifier() {
  override fun processEvent(domainEvent: DomainEvent) {
    val info = getAdditionalInfo(domainEvent, PrisonerAlertsUpdatedNotificationInfo::class.java)

    // ignore events which only have alerts removed
    if (info.alertsAdded.isNotEmpty()) {
      getVisitSchedulerService().processPrisonerAlertsUpdated(info)
    }
  }
}
