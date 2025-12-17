package uk.gov.justice.digital.hmpps.prison.visits.orchestration.service.listeners.notifiers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.appointments.SupportedCourtVideoAppointmentCategoryCode
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.service.listeners.events.DomainEvent
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.service.listeners.events.additionalinfo.CourtVideoAppointmentInfo

const val COURT_VIDEO_APPOINTMENT_CREATED_EVENT_TYPE = "appointments.appointment-instance.created"

@Component(value = COURT_VIDEO_APPOINTMENT_CREATED_EVENT_TYPE)
class CourtVideoAppointmentCreatedNotifier : EventNotifier() {
  override fun processEvent(domainEvent: DomainEvent) {
    val info: CourtVideoAppointmentInfo = getAdditionalInfo(domainEvent, CourtVideoAppointmentInfo::class.java)
    LOG.debug("Enter CourtVideoAppointmentCreatedNotifier Info: {}", info)

    getVisitSchedulerService().processCourtVideoAppointmentCreated(info)
  }

  override fun isProcessableEvent(domainEvent: DomainEvent): Boolean {
    val info: CourtVideoAppointmentInfo = getAdditionalInfo(domainEvent, CourtVideoAppointmentInfo::class.java)
    return SupportedCourtVideoAppointmentCategoryCode.entries.map { it.name }.toSet().contains(info.categoryCode.uppercase())
  }
}
