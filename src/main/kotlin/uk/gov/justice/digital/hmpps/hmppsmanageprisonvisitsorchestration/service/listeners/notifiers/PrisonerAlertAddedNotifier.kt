package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.AlertsService.Companion.prisonerSupportedAlertCodes
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.DomainEvent

const val PRISONER_ALERT_ADDED = "person.alert.created"

@Component(value = PRISONER_ALERT_ADDED)
class PrisonerAlertAddedNotifier : EventNotifier() {
  override fun processEvent(domainEvent: DomainEvent) {
    val info = getPrisonerAlertAddedInfo(domainEvent)
    getVisitSchedulerService().processPrisonerAlertAdded(info, domainEvent.description)
  }

  override fun isProcessableEvent(domainEvent: DomainEvent): Boolean {
    val prisonerAlertAddedInfo = getPrisonerAlertAddedInfo(domainEvent)
    return if (prisonerAlertAddedInfo.prisonerNumber != null) {
      if (prisonerSupportedAlertCodes.contains(prisonerAlertAddedInfo.alertCode)) {
        true
      } else {
        LOG.info("Alert code {} not in list of supported alert codes, ignoring add alert event for prisoner {}", prisonerAlertAddedInfo.alertCode, prisonerAlertAddedInfo.prisonerNumber)
        false
      }
    } else {
      LOG.info("Prisoner Number not found in prisoner alert added event, ignoring event")
      false
    }
  }
}
