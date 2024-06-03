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
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.IgnoreVisitNotificationsOrchestrationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.OrchestrationNotificationGroupDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.OrchestrationPrisonerVisitsNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.VisitHistoryDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.BookingRequestDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.CancelVisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.EventAuditDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.IgnoreVisitNotificationsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.NonAssociationChangedNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.NotificationCountDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.NotificationEventType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.PersonRestrictionChangeNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.PrisonerAlertsAddedNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.PrisonerReceivedNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.PrisonerReleasedNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.PrisonerRestrictionChangeNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.VisitorRestrictionChangeNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.filter.VisitSearchRequestFilter
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.NonAssociationChangedInfo
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.PersonRestrictionChangeInfo
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.PrisonerAlertsUpdatedNotificationInfo
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.PrisonerReceivedInfo
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.PrisonerReleasedInfo
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.PrisonerRestrictionChangeInfo
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.VisitorRestrictionChangeInfo
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.NonAssociationDomainEventType

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
   * Gets further visit details like usernames, contact details etc. for a given visit reference.
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
            text = it.text,
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

  fun findFutureVisitsForPrisoner(prisonerId: String): List<VisitDto> {
    return visitSchedulerClient.getFutureVisitsForPrisoner(prisonerId) ?: emptyList()
  }

  fun bookVisit(applicationReference: String, requestDto: BookingOrchestrationRequestDto): VisitDto? {
    return visitSchedulerClient.bookVisitSlot(
      applicationReference,
      BookingRequestDto(authenticationHelperService.currentUserName, requestDto.applicationMethodType, requestDto.allowOverBooking),
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

  fun ignoreVisitNotifications(reference: String, ignoreVisitNotifications: IgnoreVisitNotificationsOrchestrationDto): VisitDto? {
    return visitSchedulerClient.ignoreVisitNotification(
      reference,
      IgnoreVisitNotificationsDto(
        ignoreVisitNotifications.reason,
        authenticationHelperService.currentUserName,
      ),
    )
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

  fun processPrisonerAlertsUpdated(info: PrisonerAlertsUpdatedNotificationInfo) {
    visitSchedulerClient.processPrisonerAlertsUpdated(PrisonerAlertsAddedNotificationDto(info))
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

  fun getNotificationsTypesForBookingReference(reference: String): List<NotificationEventType>? {
    return visitSchedulerClient.getNotificationsTypesForBookingReference(reference)
  }
}
