package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitDetailsClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitSchedulerClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.BookingOrchestrationRequestDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.CancelVisitOrchestrationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.VisitHistoryDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.BookingRequestDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.CancelVisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.ChangeVisitSlotRequestDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.ReserveVisitSlotDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionCapacityDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionScheduleDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SupportTypeDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSchedulerReserveVisitSlotDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSessionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.filter.VisitSearchRequestFilter
import java.time.LocalDate
import java.time.LocalTime

@Service
class VisitSchedulerService(
  private val visitSchedulerClient: VisitSchedulerClient,
  private val visitDetailsClient: VisitDetailsClient,
  private val authenticationHelperService: AuthenticationHelperService,
  private val notificationService: NotificationService,
) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getVisitByReference(reference: String): VisitDto? {
    return visitSchedulerClient.getVisitByReference(reference)
  }

  /**
   * Gets further visit details like usernames, contact details etc. for a given visit reference.
   */
  fun getVisitHistoryByReference(reference: String): VisitHistoryDetailsDto? {
    return visitDetailsClient.getVisitHistoryByReference(reference)
  }

  fun visitsSearch(visitSearchRequestFilter: VisitSearchRequestFilter): Page<VisitDto>? {
    try {
      return visitSchedulerClient.getVisits(visitSearchRequestFilter)
    } catch (e: WebClientResponseException) {
      if (e.statusCode != HttpStatus.NOT_FOUND) {
        LOG.error("Exception thrown on visit-scheduler call - /visits/search - with parameters - $visitSearchRequestFilter", e)
        throw e
      }
    }

    return Page.empty()
  }

  fun reserveVisitSlot(reserveVisitSlotDto: ReserveVisitSlotDto): VisitDto? {
    return visitSchedulerClient.reserveVisitSlot(VisitSchedulerReserveVisitSlotDto(reserveVisitSlotDto, authenticationHelperService.currentUserName))
  }

  fun getVisitSupport(): List<SupportTypeDto>? {
    return visitSchedulerClient.getVisitSupport()
  }

  fun bookVisit(applicationReference: String, requestDto: BookingOrchestrationRequestDto): VisitDto? {
    return visitSchedulerClient.bookVisitSlot(
      applicationReference,
      BookingRequestDto(authenticationHelperService.currentUserName, requestDto.applicationMethodType),
    ).also { visit ->
      visit?.let {
        LOG.info("Successfully booked visit with reference - ${visit.reference}")
        sendConfirmation(NotificationService.NotificationEvent.VISIT_BOOKING, visit)
      }
    }
  }

  fun cancelVisit(reference: String, cancelVisitDto: CancelVisitOrchestrationDto): VisitDto? {
    return visitSchedulerClient.cancelVisit(
      reference,
      CancelVisitDto(
        cancelVisitDto.cancelOutcome,
        authenticationHelperService.currentUserName,
        cancelVisitDto.applicationMethodType,
      ),
    ).also { visit ->
      visit?.let {
        LOG.info("Successfully cancelled visit with reference - ${visit.reference}")
      }
    }
  }

  fun changeReservedVisitSlot(applicationReference: String, changeVisitSlotRequestDto: ChangeVisitSlotRequestDto): VisitDto? {
    return visitSchedulerClient.changeVisitSlot(applicationReference, changeVisitSlotRequestDto)
  }

  fun changeBookedVisit(reference: String, reserveVisitSlotDto: ReserveVisitSlotDto): VisitDto? {
    return visitSchedulerClient.changeBookedVisit(reference, VisitSchedulerReserveVisitSlotDto(reserveVisitSlotDto, authenticationHelperService.currentUserName))
  }

  fun getVisitSessions(prisonCode: String, prisonerId: String?, min: Long?, max: Long?): List<VisitSessionDto>? {
    return visitSchedulerClient.getVisitSessions(prisonCode, prisonerId, min, max)
  }

  fun getSupportedPrisons(): List<String>? {
    return visitSchedulerClient.getSupportedPrisons()
  }

  fun getSessionCapacity(prisonCode: String, sessionDate: LocalDate, sessionStartTime: LocalTime, sessionEndTime: LocalTime): SessionCapacityDto? {
    return visitSchedulerClient.getSessionCapacity(prisonCode, sessionDate, sessionStartTime, sessionEndTime)
  }

  fun getSessionSchedule(prisonCode: String, sessionDate: LocalDate): List<SessionScheduleDto>? {
    return visitSchedulerClient.getSessionSchedule(prisonCode, sessionDate)
  }

  fun sendConfirmation(notificationEvent: NotificationService.NotificationEvent, visit: VisitDto) {
    try {
      notificationService.sendConfirmation(notificationEvent, visit)
      LOG.info("${notificationEvent.description} SMS sent for ${visit.reference}")
    } catch (e: Exception) {
      LOG.error("Failed to send SMS for ${notificationEvent.description} event with reference - ${visit.reference}")
    }
  }
}
