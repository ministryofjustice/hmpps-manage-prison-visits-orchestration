package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.RestPage
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.alerts.api.AlertDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.alerts.api.AlertResponseDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.ContactWithOptionalPrisonerRelationshipDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.EventAuditOrchestrationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.VisitBookingDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.VisitContactDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.OffenderRestrictionsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.register.PrisonRegisterPrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.NotFoundException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.AlertsService.Companion.predicateFilterSupportedCodes
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.ManageUsersService
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.ManageUsersService.Companion.userFullNameFilterPredicate
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.utils.Comparators.Companion.alertsComparatorDateUpdatedOrCreatedDateDesc
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.utils.Comparators.Companion.restrictionsComparatorDatCreatedDesc
import java.time.Duration
import java.time.LocalDateTime
import kotlin.jvm.optionals.getOrNull

@Component
class VisitBookingDetailsClient(
  private val prisonApiClient: PrisonApiClient,
  private val alertsApiClient: AlertsApiClient,
  private val prisonerSearchClient: PrisonerSearchClient,
  private val visitSchedulerClient: VisitSchedulerClient,
  private val prisonerContactRegistryClient: PrisonerContactRegistryClient,
  private val prisonRegisterClient: PrisonRegisterClient,
  @param:Value("\${prisoner.profile.timeout:10s}") private val apiTimeout: Duration,
  private val manageUsersService: ManageUsersService,
) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getFullVisitBookingDetails(
    visitReference: String,
  ): VisitBookingDetailsDto? {
    LOG.debug("getVisitDetails for visit reference - {}", visitReference)

    val visit = visitSchedulerClient.getVisitByReference(visitReference) ?: throw NotFoundException("Visit with reference - $visitReference not found")

    val prisonDetailsMono = prisonRegisterClient.getPrisonAsMonoEmptyIfNotFound(visit.prisonCode)
    val prisonerDetailsMono = prisonerSearchClient.getPrisonerByIdAsMono(visit.prisonerId)
    val eventsMono = visitSchedulerClient.getVisitHistoryByReferenceAsMono(visitReference)
    val notificationsMono = visitSchedulerClient.getNotificationEventsForBookingReferenceAsMono(visitReference)

    val visitBookingDetails = Mono.zip(
      prisonDetailsMono,
      prisonerDetailsMono,
      eventsMono,
      notificationsMono,
    ).flatMap { visitBookingDetailsCoreInfo ->

      val prison = visitBookingDetailsCoreInfo.t1.getOrNull() ?: PrisonRegisterPrisonDto(prisonId = visit.prisonCode, prisonName = visit.prisonCode)
      val prisoner = visitBookingDetailsCoreInfo.t2
      val events = visitBookingDetailsCoreInfo.t3
      val notifications = visitBookingDetailsCoreInfo.t4

      val isPastVisit = visit.startTimestamp.isBefore(LocalDateTime.now())
      val prisonerOutOfPrison = prisoner.inOutStatus == "OUT"
      val skipAlertsAndRestrictions = isPastVisit || prisonerOutOfPrison

      val prisonerAlertsMono: Mono<RestPage<AlertResponseDto>> =
        if (skipAlertsAndRestrictions) {
          Mono.just(RestPage.empty())
        } else {
          alertsApiClient.getPrisonerAlertsAsMono(visit.prisonerId)
        }

      val prisonerRestrictionsMono: Mono<OffenderRestrictionsDto> =
        if (skipAlertsAndRestrictions) {
          Mono.just(OffenderRestrictionsDto(offenderRestrictions = emptyList()))
        } else {
          prisonApiClient.getPrisonerRestrictionsAsMono(visit.prisonerId)
        }

      val visitorsMono = prisonerContactRegistryClient.searchPrisonerContacts(
        prisonerId = visit.prisonerId,
        contactIds = visit.visitors!!.map { it.nomisPersonId },
        withRestrictions = !skipAlertsAndRestrictions,
      )

      Mono.zip(prisonerAlertsMono, prisonerRestrictionsMono, visitorsMono)
        .map { alertsRestrictionsAndVisitors ->
          val prisonerAlerts = alertsRestrictionsAndVisitors.t1.content
            .filter { predicateFilterSupportedCodes.test(it) }
            .sortedWith(alertsComparatorDateUpdatedOrCreatedDateDesc)
            .map { alertResponse -> AlertDto(alertResponse) }

          val prisonerRestrictions = (alertsRestrictionsAndVisitors.t2.offenderRestrictions ?: emptyList())
            .sortedWith(restrictionsComparatorDatCreatedDesc)

          val allVisitorsForPrisoner = alertsRestrictionsAndVisitors.t3

          val visitors = mutableListOf<ContactWithOptionalPrisonerRelationshipDto>()
          visit.visitors.forEach { visitVisitor ->
            allVisitorsForPrisoner.firstOrNull { it.contactId == visitVisitor.nomisPersonId }?.let {
              visitors.add(it)
            }
          }

          val eventAuditDetails = events.map {
            EventAuditOrchestrationDto(
              eventAuditDto = it,
              actionedByFullName = it.actionedBy.userName,
            )
          }

          val visitContact = visit.visitContact?.let { contact ->
            val contactId = visit.visitors?.firstOrNull { it.visitContact == true }?.nomisPersonId
            VisitContactDto(contact, contactId)
          }

          VisitBookingDetailsDto(
            visit = visit,
            prison = prison,
            prisonerDto = prisoner,
            prisonerAlerts = prisonerAlerts,
            prisonerRestrictions = prisonerRestrictions,
            visitVisitors = visitors,
            visitContact = visitContact,
            events = eventAuditDetails,
            notifications = notifications,
            skipAlertsAndRestrictions = skipAlertsAndRestrictions,
          )
        }
    }
      .block(apiTimeout)

    return visitBookingDetails?.let {
      updateEventUserNames(it)
    }
  }

  private fun updateEventUserNames(visitBookingDetailsDto: VisitBookingDetailsDto): VisitBookingDetailsDto {
    val userNameMap = manageUsersService.getFullNamesFromEventAuditOrchestrationDetails(visitBookingDetailsDto.events)
    visitBookingDetailsDto.events
      .filter { userFullNameFilterPredicate.test(it.userType, it.actionedByFullName) }
      .forEach { event ->
        event.actionedByFullName = userNameMap[event.actionedByFullName] ?: event.actionedByFullName
      }

    return visitBookingDetailsDto
  }
}
