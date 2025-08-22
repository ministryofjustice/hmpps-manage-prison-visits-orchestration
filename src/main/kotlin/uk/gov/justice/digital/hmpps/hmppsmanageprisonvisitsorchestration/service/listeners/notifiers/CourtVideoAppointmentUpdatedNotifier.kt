package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.DomainEvent
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.CourtVideoAppointmentInfo

const val COURT_VIDEO_APPOINTMENT_UPDATED_EVENT_TYPE = "appointments.appointment-instance.updated"

@Component(value = COURT_VIDEO_APPOINTMENT_UPDATED_EVENT_TYPE)
class CourtVideoAppointmentUpdatedNotifier : EventNotifier() {
  override fun processEvent(domainEvent: DomainEvent) {
    val info: CourtVideoAppointmentInfo = getAdditionalInfo(domainEvent, CourtVideoAppointmentInfo::class.java)
    LOG.debug("Enter CourtVideoAppointmentUpdatedNotifier Info: {}", info)

    getVisitSchedulerService().processCourtVideoAppointmentUpdated(info)
  }

  // TODO: VB-5754 - Add the eventCode to AdditionalInformation object, and filter to only process certain event codes (see JIRA for list).
  override fun isProcessableEvent(domainEvent: DomainEvent): Boolean = true
}
