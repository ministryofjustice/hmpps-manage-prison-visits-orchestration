package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.AlertsService.Companion.prisonerSupportedAlertCodes
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.DomainEvent

const val PRISONER_ALERT_INACTIVATED = "person.alert.inactive"

@Component(value = PRISONER_ALERT_INACTIVATED)
class PrisonerAlertInactivatedNotifier : EventNotifier() {
  override fun processEvent(domainEvent: DomainEvent) {
    val info = getPrisonerAlertNotificationInfo(domainEvent)
    getVisitSchedulerService().processPrisonerAlertInactivated(info, domainEvent.description)
  }

  override fun isProcessableEvent(domainEvent: DomainEvent): Boolean {
    val prisonerAlertInactivatedInfo = getPrisonerAlertNotificationInfo(domainEvent)
    return if (prisonerAlertInactivatedInfo.prisonerNumber != null) {
      if (prisonerSupportedAlertCodes.contains(prisonerAlertInactivatedInfo.alertCode)) {
        true
      } else {
        LOG.info("Alert code {} not in list of supported alert codes, ignoring alert inactivated event for prisoner {}", prisonerAlertInactivatedInfo.alertCode, prisonerAlertInactivatedInfo.prisonerNumber)
        false
      }
    } else {
      LOG.info("Prisoner Number not found in prisoner alert inactivated event, ignoring event")
      false
    }
  }
}
