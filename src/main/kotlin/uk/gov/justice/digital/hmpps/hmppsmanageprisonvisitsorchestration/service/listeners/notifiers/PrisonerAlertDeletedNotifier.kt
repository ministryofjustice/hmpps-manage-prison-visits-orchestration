package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.AlertsService.Companion.prisonerSupportedAlertCodes
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.DomainEvent

const val PRISONER_ALERT_DELETED = "person.alert.deleted"

@Component(value = PRISONER_ALERT_DELETED)
class PrisonerAlertDeletedNotifier : EventNotifier() {
  override fun processEvent(domainEvent: DomainEvent) {
    val info = getPrisonerAlertUpsertedInfo(domainEvent)
    getVisitSchedulerService().processPrisonerAlertDeleted(info, domainEvent.description)
  }

  override fun isProcessableEvent(domainEvent: DomainEvent): Boolean {
    val prisonerAlertDeletedInfo = getPrisonerAlertUpsertedInfo(domainEvent)
    return if (prisonerAlertDeletedInfo.prisonerNumber != null) {
      if (prisonerSupportedAlertCodes.contains(prisonerAlertDeletedInfo.alertCode)) {
        true
      } else {
        LOG.info("Alert code {} not in list of supported alert codes, ignoring delete alert event for prisoner {}", prisonerAlertDeletedInfo.alertCode, prisonerAlertDeletedInfo.prisonerNumber)
        false
      }
    } else {
      LOG.info("Prisoner Number not found in prisoner alert deleted event, ignoring event")
      false
    }
  }
}
