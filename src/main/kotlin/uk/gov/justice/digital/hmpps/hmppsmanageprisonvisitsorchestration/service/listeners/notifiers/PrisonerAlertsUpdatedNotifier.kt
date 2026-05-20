package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.AlertsService.Companion.prisonerSupportedAlertCodes
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.DomainEvent
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.PrisonerAlertsUpdatedNotificationInfo

const val PRISONER_ALERTS_UPDATED = "prisoner-offender-search.prisoner.alerts-updated"

@Component(value = PRISONER_ALERTS_UPDATED)
class PrisonerAlertsUpdatedNotifier : EventNotifier() {
  override fun processEvent(domainEvent: DomainEvent) {
    val info = getAdditionalInfo(domainEvent, PrisonerAlertsUpdatedNotificationInfo::class.java)
    getVisitSchedulerService().processPrisonerAlertsUpdated(filterCodes(info), domainEvent.description)
  }

  override fun isProcessableEvent(domainEvent: DomainEvent): Boolean {
    // TODO - implement
    return true
  }

  private fun filterCodes(prisonerAlertsUpdatedNotificationInfo: PrisonerAlertsUpdatedNotificationInfo): PrisonerAlertsUpdatedNotificationInfo {
    // A temporary helper function to filter the alert codes
    fun List<String>.filterSupported() = filter { it in prisonerSupportedAlertCodes }

    return prisonerAlertsUpdatedNotificationInfo.apply {
      alertsAdded = alertsAdded.filterSupported()
      alertsRemoved = alertsRemoved.filterSupported()
    }
  }
}
