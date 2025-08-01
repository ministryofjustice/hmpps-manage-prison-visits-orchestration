package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitBookingDetailsClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitSchedulerClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.builder.OrchestrationVisitDtoBuilder
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.BookingOrchestrationRequestDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.CancelVisitOrchestrationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.IgnoreVisitNotificationsOrchestrationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.OrchestrationApproveRejectVisitRequestResponseDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.OrchestrationVisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.OrchestrationVisitNotificationsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.OrchestrationVisitRequestSummaryDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.VisitBookingDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.ActionedByDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.ApproveVisitRequestBodyDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.BookingRequestDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.CancelVisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.IgnoreVisitNotificationsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.RejectVisitRequestBodyDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitRequestsCountDto
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
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.NotFoundException
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
  private val visitBookingDetailsClient: VisitBookingDetailsClient,
) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getVisitByReference(reference: String): VisitDto? = visitSchedulerClient.getVisitByReference(reference)

  fun getVisitReferenceByClientReference(clientReference: String): List<String?>? {
    val visitReferences = visitSchedulerClient.getVisitReferenceByClientReference(clientReference)
    if (visitReferences.isNullOrEmpty()) {
      LOG.info("No visit found for client reference: $clientReference")
      throw NotFoundException("No visit found for client reference: $clientReference")
    }
    return visitReferences
  }

  fun getFullVisitBookingDetailsByReference(reference: String): VisitBookingDetailsDto? {
    LOG.info("Retrieving visit booking details for visit reference: $reference")
    return visitBookingDetailsClient.getFullVisitBookingDetails(reference)
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

  fun getFuturePublicBookedVisitsByBookerReference(bookerReference: String): List<OrchestrationVisitDto> = mapVisitDtoToOrchestrationVisitDto(visitSchedulerClient.getFuturePublicBookedVisitsByBookerReference(bookerReference))

  fun getPastPublicBookedVisitsByBookerReference(bookerReference: String): List<OrchestrationVisitDto> = mapVisitDtoToOrchestrationVisitDto(visitSchedulerClient.getPastPublicBookedVisitsByBookerReference(bookerReference))

  fun getCancelledPublicVisitsByBookerReference(bookerReference: String): List<OrchestrationVisitDto> = mapVisitDtoToOrchestrationVisitDto(visitSchedulerClient.getCancelledPublicVisitsByBookerReference(bookerReference))

  fun findFutureVisitsForPrisoner(prisonerId: String): List<VisitDto> = visitSchedulerClient.getFutureVisitsForPrisoner(prisonerId) ?: emptyList()

  fun bookVisit(applicationReference: String, requestDto: BookingOrchestrationRequestDto): VisitDto? = visitSchedulerClient.bookVisitSlot(
    applicationReference,
    BookingRequestDto(requestDto.actionedBy, requestDto.applicationMethodType, requestDto.allowOverBooking, requestDto.userType, requestDto.isRequestBooking),
  )

  fun updateVisit(applicationReference: String, requestDto: BookingOrchestrationRequestDto): VisitDto? = visitSchedulerClient.updateBookedVisit(
    applicationReference,
    BookingRequestDto(requestDto.actionedBy, requestDto.applicationMethodType, requestDto.allowOverBooking, requestDto.userType),
  )

  fun cancelVisit(reference: String, cancelVisitDto: CancelVisitOrchestrationDto): VisitDto? = visitSchedulerClient.cancelVisit(
    reference,
    CancelVisitDto(cancelVisitDto),
  )

  fun ignoreVisitNotifications(reference: String, ignoreVisitNotifications: IgnoreVisitNotificationsOrchestrationDto): VisitDto? = visitSchedulerClient.ignoreVisitNotification(
    reference,
    IgnoreVisitNotificationsDto(ignoreVisitNotifications),
  )

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
    val prisonerAlertsAddedNotificationDto = PrisonerAlertsAddedNotificationDto(
      info,
      alertService.getSupportedPrisonerActiveAlertCodes(info.nomsNumber),
      description ?: "${info.alertsAdded.size} alert(s) added, ${info.alertsRemoved.size} alert(s) removed.",
    )

    visitSchedulerClient.processPrisonerAlertsUpdated(sendDto = prisonerAlertsAddedNotificationDto)
  }

  fun getNotificationCountForPrison(prisonCode: String, notificationEventTypes: List<NotificationEventType>?): NotificationCountDto? = visitSchedulerClient.getNotificationCountForPrison(prisonCode, notificationEventTypes?.map { it.name }?.toList())

  fun getVisitRequestsCountForPrison(prisonCode: String): VisitRequestsCountDto? = visitSchedulerClient.getVisitRequestsCountForPrison(prisonCode)

  fun getFutureVisitsWithNotifications(prisonCode: String, notificationEventTypes: List<NotificationEventType>?): List<OrchestrationVisitNotificationsDto> {
    val visitsWithNotifications = visitSchedulerClient.getFutureVisitsWithNotifications(prisonCode, notificationEventTypes?.map { it.name }?.toList())

    return visitsWithNotifications?.map {
      OrchestrationVisitNotificationsDto(
        prisonerNumber = it.prisonerNumber,
        bookedByUserName = getUsernameFromActionedBy(it.bookedBy),
        visitDate = it.visitDate,
        visitReference = it.visitReference,
        bookedByName = manageUsersService.getFullNameFromActionedBy(it.bookedBy),
        notifications = it.notifications,
      )
    } ?: emptyList()
  }

  fun getVisitRequestsForPrison(prisonCode: String): List<OrchestrationVisitRequestSummaryDto> {
    val visitRequestsList = visitSchedulerClient.getVisitRequestsForPrison(prisonCode)

    return visitRequestsList?.map { visitRequest ->
      OrchestrationVisitRequestSummaryDto(
        visitReference = visitRequest.visitReference,
        visitDate = visitRequest.visitDate,
        requestedOnDate = visitRequest.requestedOnDate,
        prisonerFirstName = visitRequest.prisonerFirstName,
        prisonerLastName = visitRequest.prisonerLastName,
        prisonNumber = visitRequest.prisonNumber,
        mainContact = visitRequest.mainContact,
      )
    } ?: emptyList()
  }

  fun approveVisitRequestByReference(approveVisitRequestResponseDto: ApproveVisitRequestBodyDto): OrchestrationApproveRejectVisitRequestResponseDto? {
    visitSchedulerClient.approveVisitRequestByReference(approveVisitRequestResponseDto)?.let {
      val prisoner = try {
        prisonerSearchService.getPrisoner(it.prisonerId)
      } catch (e: Exception) {
        LOG.error("Exception thrown on visit-scheduler call - /visits/${approveVisitRequestResponseDto.visitReference}/approve. Catching and using placeholder to avoid failing - exception $e")
        null
      }

      return OrchestrationApproveRejectVisitRequestResponseDto(
        visitReference = it.reference,
        prisonerFirstName = prisoner?.firstName ?: it.prisonerId,
        prisonerLastName = prisoner?.lastName ?: it.prisonerId,
      )
    }

    return null
  }

  fun rejectVisitRequestByReference(rejectVisitRequestBodyDto: RejectVisitRequestBodyDto): OrchestrationApproveRejectVisitRequestResponseDto? {
    visitSchedulerClient.rejectVisitRequestByReference(rejectVisitRequestBodyDto)?.let {
      val prisoner = try {
        prisonerSearchService.getPrisoner(it.prisonerId)
      } catch (e: Exception) {
        LOG.error("Exception thrown on visit-scheduler call - /visits/${rejectVisitRequestBodyDto.visitReference}/reject. Catching and using placeholder to avoid failing - exception $e")
        null
      }

      return OrchestrationApproveRejectVisitRequestResponseDto(
        visitReference = it.reference,
        prisonerFirstName = prisoner?.firstName ?: it.prisonerId,
        prisonerLastName = prisoner?.lastName ?: it.prisonerId,
      )
    }

    return null
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

  private fun getUsernameFromActionedBy(actionedByDto: ActionedByDto): String = when (actionedByDto.userType) {
    UserType.STAFF -> actionedByDto.userName!!
    UserType.PUBLIC -> actionedByDto.bookerReference!!
    UserType.SYSTEM -> ""
    UserType.PRISONER -> actionedByDto.userName!!
  }
}
