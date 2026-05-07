package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.AlertsService.Companion.prisonerSupportedAlertCodes
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.DomainEvent

const val PRISONER_ALERT_UPDATED = "person.alert.updated"

@Component(value = PRISONER_ALERT_UPDATED)
class PrisonerAlertUpdatedNotifier : EventNotifier() {
  override fun processEvent(domainEvent: DomainEvent) {
    val info = getPrisonerAlertUpsertedInfo(domainEvent)
    getVisitSchedulerService().processPrisonerAlertUpdated(info, domainEvent.description)
  }

  override fun isProcessableEvent(domainEvent: DomainEvent): Boolean {
    val prisonerAlertUpdatedInfo = getPrisonerAlertUpsertedInfo(domainEvent)
    return if (prisonerAlertUpdatedInfo.prisonerNumber != null) {
      if (prisonerSupportedAlertCodes.contains(prisonerAlertUpdatedInfo.alertCode)) {
        true
      } else {
        LOG.info("Alert code {} not in list of supported alert codes, ignoring update alert event for prisoner {}", prisonerAlertUpdatedInfo.alertCode, prisonerAlertUpdatedInfo.prisonerNumber)
        false
      }
    } else {
      LOG.info("Prisoner Number not found in prisoner alert updated event, ignoring event")
      false
    }
  }
}
