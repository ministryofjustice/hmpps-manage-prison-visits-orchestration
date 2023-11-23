package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitSchedulerClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.BookingOrchestrationRequestDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.CancelVisitOrchestrationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.OrchestrationNotificationGroupDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.OrchestrationPrisonerVisitsNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.VisitHistoryDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.BookingRequestDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.CancelVisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.ChangeVisitSlotRequestDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.EventAuditDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.PrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.ReserveVisitSlotDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionCapacityDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionScheduleDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SupportTypeDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSchedulerReserveVisitSlotDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSessionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.NonAssociationChangedNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.NotificationCountDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.PersonRestrictionChangeNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.PrisonerReceivedNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.PrisonerReleasedNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.PrisonerRestrictionChangeNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.VisitorRestrictionChangeNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.filter.VisitSearchRequestFilter
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.NonAssociationChangedInfo
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.PersonRestrictionChangeInfo
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.PrisonerReceivedInfo
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.PrisonerReleasedInfo
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.PrisonerRestrictionChangeInfo
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.VisitorRestrictionChangeInfo
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.NonAssociationDomainEventType
import java.time.LocalDate
import java.time.LocalTime

@Service
class VisitSchedulerService(
  private val visitSchedulerClient: VisitSchedulerClient,
  private val authenticationHelperService: AuthenticationHelperService,
  private val manageUsersService: ManageUsersService,
) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getVisitByReference(reference: String): VisitDto? {
    return visitSchedulerClient.getVisitByReference(reference)
  }

  /**
   * Gets further visit details like usernames, contact details etc for a given visit reference.
   */
  fun getVisitHistoryByReference(
    reference: String,
  ): VisitHistoryDetailsDto? {
    val visit = visitSchedulerClient.getVisitByReference(reference)

    visit?.let {
      val eventAuditList = visitSchedulerClient.getVisitHistoryByReference(reference)
      if (!eventAuditList.isNullOrEmpty()) {
        val names = manageUsersService.getFullNamesFromVisitHistory(eventAuditList)
        val eventAuditListWithNames = eventAuditList.map {
          EventAuditDto(
            type = it.type,
            applicationMethodType = it.applicationMethodType,
            actionedBy = names[it.actionedBy] ?: it.actionedBy,
            sessionTemplateReference = it.sessionTemplateReference,
            createTimestamp = it.createTimestamp,
          )
        }
        return VisitHistoryDetailsDto(
          eventsAudit = eventAuditListWithNames,
          visit = visit,
        )
      }
      return VisitHistoryDetailsDto(
        visit = visit,
      )
    }
    return null
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
    )
  }

  fun cancelVisit(reference: String, cancelVisitDto: CancelVisitOrchestrationDto): VisitDto? {
    return visitSchedulerClient.cancelVisit(
      reference,
      CancelVisitDto(
        cancelVisitDto.cancelOutcome,
        authenticationHelperService.currentUserName,
        cancelVisitDto.applicationMethodType,
      ),
    )
  }

  fun changeReservedVisitSlot(applicationReference: String, changeVisitSlotRequestDto: ChangeVisitSlotRequestDto): VisitDto? {
    return visitSchedulerClient.changeVisitSlot(applicationReference, changeVisitSlotRequestDto)
  }

  fun changeBookedVisit(reference: String, reserveVisitSlotDto: ReserveVisitSlotDto): VisitDto? {
    return visitSchedulerClient.changeBookedVisit(reference, VisitSchedulerReserveVisitSlotDto(reserveVisitSlotDto, authenticationHelperService.currentUserName))
  }

  fun getVisitSessions(prisonCode: String, prisonerId: String?, min: Int?, max: Int?): List<VisitSessionDto>? {
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

  fun processNonAssociations(info: NonAssociationChangedInfo, type: NonAssociationDomainEventType) {
    visitSchedulerClient.processNonAssociations(NonAssociationChangedNotificationDto(info, type))
  }

  fun processPrisonerReceived(info: PrisonerReceivedInfo) {
    visitSchedulerClient.processPrisonerReceived(PrisonerReceivedNotificationDto(info))
  }

  fun processPrisonerReleased(info: PrisonerReleasedInfo) {
    visitSchedulerClient.processPrisonerReleased(PrisonerReleasedNotificationDto(info))
  }

  fun processPersonRestrictionChange(info: PersonRestrictionChangeInfo) {
    visitSchedulerClient.processPersonRestrictionChange(PersonRestrictionChangeNotificationDto(info))
  }

  fun processPrisonerRestrictionChange(info: PrisonerRestrictionChangeInfo) {
    visitSchedulerClient.processPrisonerRestrictionChange(PrisonerRestrictionChangeNotificationDto(info))
  }

  fun processVisitorRestrictionChange(info: VisitorRestrictionChangeInfo) {
    visitSchedulerClient.processVisitorRestrictionChange(VisitorRestrictionChangeNotificationDto(info))
  }

  fun getNotificationCountForPrison(prisonCode: String): NotificationCountDto? {
    return visitSchedulerClient.getNotificationCountForPrison(prisonCode)
  }

  fun getNotificationCount(): NotificationCountDto? {
    return visitSchedulerClient.getNotificationCount()
  }

  fun getFutureNotificationVisitGroups(prisonCode: String): List<OrchestrationNotificationGroupDto>? {
    val groups = visitSchedulerClient.getFutureNotificationVisitGroups(prisonCode)

    return groups?.map { group ->
      val affectedVisits = group.affectedVisits.map {
        OrchestrationPrisonerVisitsNotificationDto(
          it.prisonerNumber,
          it.bookedByUserName,
          it.visitDate,
          it.bookingReference,
          manageUsersService.getUserFullName(it.bookedByUserName),
        )
      }
      OrchestrationNotificationGroupDto(group.reference, group.type, affectedVisits)
    }
  }

  fun getPrison(prisonCode: String): PrisonDto? {
    return visitSchedulerClient.getPrison(prisonCode)
  }
}
