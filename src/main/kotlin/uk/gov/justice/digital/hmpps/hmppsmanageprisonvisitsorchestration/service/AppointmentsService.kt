package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.WhereAboutsApiClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.whereabouts.ScheduledEventDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.whereabouts.enums.HigherPriorityMedicalOrLegalEvents
import java.time.LocalDate

@Service
class AppointmentsService(
  private val whereAboutsApiClient: WhereAboutsApiClient,
) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
    const val APPOINTMENT_EVENT_TYPE = "APP"
  }

  fun getHigherPriorityAppointments(
    prisonerId: String,
    fromDate: LocalDate,
    toDate: LocalDate,
  ): List<ScheduledEventDto> {
    return getAppointments(prisonerId, fromDate, toDate).filter { event ->
      HigherPriorityMedicalOrLegalEvents.entries.map { it.code }.contains(event.eventSubType)
    }
  }

  private fun getAppointments(
    prisonerId: String,
    fromDate: LocalDate,
    toDate: LocalDate,
  ): List<ScheduledEventDto> {
    return whereAboutsApiClient.getEvents(prisonerId, fromDate, toDate).filter { event ->
      event.eventType.equals(APPOINTMENT_EVENT_TYPE)
    }
  }
}
