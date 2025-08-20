package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.DomainEvent
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.CourtVideoAppointmentCreatedInfo

const val COURT_VIDEO_APPOINTMENT_CREATED_EVENT_TYPE = "appointments.appointment-instance.created"

@Component(value = COURT_VIDEO_APPOINTMENT_CREATED_EVENT_TYPE)
class CourtVideoAppointmentCreatedNotifier : EventNotifier() {
  override fun processEvent(domainEvent: DomainEvent) {
    val info: CourtVideoAppointmentCreatedInfo = getAdditionalInfo(domainEvent, CourtVideoAppointmentCreatedInfo::class.java)
    LOG.debug("Enter CourtVideoAppointmentCreatedNotifier Info: {}", info)

    // TODO: VB-5754 - Add the eventCode to AdditionalInformation object, and filter to only process certain event codes (see JIRA for list).
    getVisitSchedulerService().processCourtVideoAppointmentCreated(info)
  }

  override fun isProcessableEvent(domainEvent: DomainEvent): Boolean {
    // TODO - implement
    return true
  }
}
