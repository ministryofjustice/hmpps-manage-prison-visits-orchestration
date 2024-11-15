package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitSchedulerClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.builder.OrchestrationVisitDtoBuilder
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.BookingOrchestrationRequestDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.CancelVisitOrchestrationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.EventAuditOrchestrationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.IgnoreVisitNotificationsOrchestrationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.OrchestrationNotificationGroupDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.OrchestrationPrisonerVisitsNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.OrchestrationVisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.VisitDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.VisitHistoryDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.ActionedByDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.BookingRequestDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.CancelVisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.IgnoreVisitNotificationsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.NonAssociationChangedNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.NotificationCountDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.NotificationEventType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.PersonRestrictionUpsertedNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.PrisonerAlertsAddedNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.PrisonerReceivedNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.PrisonerReleasedNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.PrisonerRestrictionChangeNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.VisitorApprovedUnapprovedNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.VisitorRestrictionUpsertedNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.filter.VisitSearchRequestFilter
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.NonAssociationChangedInfo
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.PersonRestrictionUpsertedInfo
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.PrisonerAlertsUpdatedNotificationInfo
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.PrisonerReceivedInfo
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.PrisonerReleasedInfo
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.PrisonerRestrictionChangeInfo
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.VisitorApprovedUnapprovedInfo
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.VisitorRestrictionUpsertedInfo
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.NonAssociationDomainEventType

@Service
class VisitSchedulerService(
  private val visitSchedulerClient: VisitSchedulerClient,
  private val prisonerContactService: PrisonerContactService,
  private val manageUsersService: ManageUsersService,
  private val alertService: AlertsService,
  private val orchestrationVisitDtoBuilder: OrchestrationVisitDtoBuilder,
  private val prisonerSearchService: PrisonerSearchService,
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
          EventAuditOrchestrationDto(
            type = it.type,
            applicationMethodType = it.applicationMethodType,
            actionedByFullName = names[it.actionedBy.userName] ?: it.actionedBy.userName,
            userType = it.actionedBy.userType,
            sessionTemplateReference = it.sessionTemplateReference,
            createTimestamp = it.createTimestamp,
            text = it.text,
          )
        }
        return VisitHistoryDetailsDto(
          eventsAudit = eventAuditListWithNames,
          visit = VisitDetailsDto(visit),
        )
      }
      return VisitHistoryDetailsDto(
        visit = VisitDetailsDto(visit),
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

  fun getFuturePublicBookedVisitsByBookerReference(bookerReference: String): List<OrchestrationVisitDto> {
    return mapVisitDtoToOrchestrationVisitDto(visitSchedulerClient.getFuturePublicBookedVisitsByBookerReference(bookerReference))
  }

  fun getPastPublicBookedVisitsByBookerReference(bookerReference: String): List<OrchestrationVisitDto> {
    return mapVisitDtoToOrchestrationVisitDto(visitSchedulerClient.getPastPublicBookedVisitsByBookerReference(bookerReference))
  }

  fun getCancelledPublicVisitsByBookerReference(bookerReference: String): List<OrchestrationVisitDto> {
    return mapVisitDtoToOrchestrationVisitDto(visitSchedulerClient.getCancelledPublicVisitsByBookerReference(bookerReference))
  }

  fun findFutureVisitsForPrisoner(prisonerId: String): List<VisitDto> {
    return visitSchedulerClient.getFutureVisitsForPrisoner(prisonerId) ?: emptyList()
  }

  fun bookVisit(applicationReference: String, requestDto: BookingOrchestrationRequestDto): VisitDto? {
    val existingBooking = visitSchedulerClient.getBookedVisitByApplicationReference(applicationReference)
    return existingBooking?.let {
      visitSchedulerClient.updateBookedVisit(
        applicationReference,
        BookingRequestDto(requestDto.actionedBy, requestDto.applicationMethodType, requestDto.allowOverBooking),
      )
    } ?: visitSchedulerClient.bookVisitSlot(
      applicationReference,
      BookingRequestDto(requestDto.actionedBy, requestDto.applicationMethodType, requestDto.allowOverBooking),
    )
  }

  fun cancelVisit(reference: String, cancelVisitDto: CancelVisitOrchestrationDto): VisitDto? {
    return visitSchedulerClient.cancelVisit(
      reference,
      CancelVisitDto(cancelVisitDto),
    )
  }

  fun ignoreVisitNotifications(reference: String, ignoreVisitNotifications: IgnoreVisitNotificationsOrchestrationDto): VisitDto? {
    return visitSchedulerClient.ignoreVisitNotification(
      reference,
      IgnoreVisitNotificationsDto(ignoreVisitNotifications),
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

  fun processPersonRestrictionUpserted(info: PersonRestrictionUpsertedInfo) {
    visitSchedulerClient.processPersonRestrictionUpserted(PersonRestrictionUpsertedNotificationDto(info))
  }

  fun processPrisonerRestrictionChange(info: PrisonerRestrictionChangeInfo) {
    visitSchedulerClient.processPrisonerRestrictionChange(PrisonerRestrictionChangeNotificationDto(info))
  }

  fun processVisitorRestrictionUpserted(info: VisitorRestrictionUpsertedInfo) {
    visitSchedulerClient.processVisitorRestrictionUpserted(VisitorRestrictionUpsertedNotificationDto(info))
  }

  fun processVisitorUnapproved(info: VisitorApprovedUnapprovedInfo) {
    visitSchedulerClient.processVisitorUnapproved(VisitorApprovedUnapprovedNotificationDto(info))
  }

  fun processVisitorApproved(info: VisitorApprovedUnapprovedInfo) {
    visitSchedulerClient.processVisitorApproved(VisitorApprovedUnapprovedNotificationDto(info))
  }

  fun processPrisonerAlertsUpdated(info: PrisonerAlertsUpdatedNotificationInfo, description: String?) {
    val alertDescription = description ?: "${info.alertsAdded.size} alert(s) added, ${info.alertsRemoved.size} alert(s) removed."
    val activeAlerts = alertService.getPrisonerActiveAlertCodes(info.nomsNumber)
    val prisonerAlertsAddedNotificationDto = PrisonerAlertsAddedNotificationDto(info, activeAlerts, alertDescription)

    visitSchedulerClient.processPrisonerAlertsUpdated(sendDto = prisonerAlertsAddedNotificationDto)
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
          getUsernameFromActionedBy(it.lastActionedBy),
          it.visitDate,
          it.bookingReference,
          manageUsersService.getFullNameFromActionedBy(it.lastActionedBy),
        )
      }
      OrchestrationNotificationGroupDto(group.reference, group.type, affectedVisits)
    }
  }

  fun getNotificationsTypesForBookingReference(reference: String): List<NotificationEventType>? {
    return visitSchedulerClient.getNotificationsTypesForBookingReference(reference)
  }

  private fun mapVisitDtoToOrchestrationVisitDto(visits: List<VisitDto>?): List<OrchestrationVisitDto> {
    val prisonerIds = visits?.map { it.prisonerId }?.toSet() ?: emptySet()

    val prisonersInfoMap = prisonerSearchService.getPrisoners(prisonerIds)
    val prisonerContactsMap = prisonerContactService.getPrisonersContacts(prisonerIds)

    return visits?.map { visit ->
      val contacts = prisonerContactsMap[visit.prisonerId] ?: emptyList()
      val prisoner = prisonersInfoMap[visit.prisonerId]

      orchestrationVisitDtoBuilder.build(visit, contacts, prisoner)
    }?.toList() ?: emptyList()
  }

  private fun getUsernameFromActionedBy(actionedByDto: ActionedByDto): String {
    return when (actionedByDto.userType) {
      UserType.STAFF -> actionedByDto.userName!!
      UserType.PUBLIC -> actionedByDto.bookerReference!!
      UserType.SYSTEM -> ""
    }
  }
}
