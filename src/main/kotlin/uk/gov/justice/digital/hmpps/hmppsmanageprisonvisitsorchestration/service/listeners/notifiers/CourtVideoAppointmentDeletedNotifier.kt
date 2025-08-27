package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.DomainEvent
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.CourtVideoAppointmentInfo

const val COURT_VIDEO_APPOINTMENT_DELETED_EVENT_TYPE = "appointments.appointment-instance.deleted"

@Component(value = COURT_VIDEO_APPOINTMENT_DELETED_EVENT_TYPE)
class CourtVideoAppointmentDeletedNotifier : EventNotifier() {
  override fun processEvent(domainEvent: DomainEvent) {
    val info: CourtVideoAppointmentInfo = getAdditionalInfo(domainEvent, CourtVideoAppointmentInfo::class.java)
    LOG.debug("Enter CourtVideoAppointmentDeletedNotifier Info: {}", info)

    getVisitSchedulerService().processCourtVideoAppointmentCancelledDeleted(info)
  }

  // TODO: VB-5754 - Add the eventCode to AdditionalInformation object, and filter to only process certain event codes (see JIRA for list).
  override fun isProcessableEvent(domainEvent: DomainEvent): Boolean = true
}
