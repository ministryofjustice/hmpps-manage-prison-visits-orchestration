package uk.gov.justice.digital.hmpps.orchestration.service.listeners.notifiers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.orchestration.dto.appointments.SupportedCourtVideoAppointmentCategoryCode
import uk.gov.justice.digital.hmpps.orchestration.service.listeners.events.DomainEvent
import uk.gov.justice.digital.hmpps.orchestration.service.listeners.events.additionalinfo.CourtVideoAppointmentInfo

const val COURT_VIDEO_APPOINTMENT_CANCELLED_EVENT_TYPE = "appointments.appointment-instance.cancelled"

@Component(value = COURT_VIDEO_APPOINTMENT_CANCELLED_EVENT_TYPE)
class CourtVideoAppointmentCancelledNotifier : EventNotifier() {
  override fun processEvent(domainEvent: DomainEvent) {
    val info: CourtVideoAppointmentInfo = getAdditionalInfo(domainEvent, CourtVideoAppointmentInfo::class.java)
    LOG.debug("Enter CourtVideoAppointmentCancelledNotifier Info: {}", info)

    getVisitSchedulerService().processCourtVideoAppointmentCancelledDeleted(info)
  }

  override fun isProcessableEvent(domainEvent: DomainEvent): Boolean {
    val info: CourtVideoAppointmentInfo = getAdditionalInfo(domainEvent, CourtVideoAppointmentInfo::class.java)
    return SupportedCourtVideoAppointmentCategoryCode.entries.map { it.name }.toSet().contains(info.categoryCode.uppercase())
  }
}
