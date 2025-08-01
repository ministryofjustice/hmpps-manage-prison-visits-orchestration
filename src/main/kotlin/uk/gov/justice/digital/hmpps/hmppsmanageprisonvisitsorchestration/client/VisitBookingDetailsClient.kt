package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.alerts.api.AlertDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.PrisonerContactDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.EventAuditOrchestrationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.VisitBookingDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.VisitContactDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.register.PrisonRegisterPrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.NotFoundException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.AlertsService.Companion.predicateFilterSupportedCodes
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.ManageUsersService
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.ManageUsersService.Companion.userFullNameFilterPredicate
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.utils.Comparators.Companion.alertsComparatorDateUpdatedOrCreatedDateDesc
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.utils.Comparators.Companion.restrictionsComparatorDatCreatedDesc
import java.time.Duration
import kotlin.jvm.optionals.getOrNull

@Component
class VisitBookingDetailsClient(
  private val prisonApiClient: PrisonApiClient,
  private val alertsApiClient: AlertsApiClient,
  private val prisonerSearchClient: PrisonerSearchClient,
  private val visitSchedulerClient: VisitSchedulerClient,
  private val prisonerContactRegistryClient: PrisonerContactRegistryClient,
  private val prisonRegisterClient: PrisonRegisterClient,
  @Value("\${prisoner.profile.timeout:10s}") private val apiTimeout: Duration,
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
    val prisonerAlertsMono = alertsApiClient.getPrisonerAlertsAsMono(visit.prisonerId)
    val prisonerRestrictionsMono = prisonApiClient.getPrisonerRestrictionsAsMono(visit.prisonerId)
    val visitorsMono = prisonerContactRegistryClient.getPrisonersSocialContactsAsMono(
      prisonerId = visit.prisonerId,
      withAddress = true,
    )
    val eventsMono = visitSchedulerClient.getVisitHistoryByReferenceAsMono(visitReference)

    val notificationsMono = visitSchedulerClient.getNotificationEventsForBookingReferenceAsMono(visitReference)

    val visitBookingDetails = Mono.zip(prisonDetailsMono, prisonerDetailsMono, prisonerAlertsMono, prisonerRestrictionsMono, visitorsMono, eventsMono, notificationsMono)
      .map { visitBookingDetailsMono ->
        val prison = visitBookingDetailsMono.t1.getOrNull() ?: PrisonRegisterPrisonDto(prisonId = visit.prisonCode, prisonName = visit.prisonCode)
        val prisoner = visitBookingDetailsMono.t2
        val prisonerAlerts = visitBookingDetailsMono.t3.content.filter {
          predicateFilterSupportedCodes.test(it)
        }.sortedWith(alertsComparatorDateUpdatedOrCreatedDateDesc)
          .map { alertResponse -> AlertDto(alertResponse) }

        val prisonerRestrictions = (visitBookingDetailsMono.t4.offenderRestrictions ?: emptyList()).sortedWith(
          restrictionsComparatorDatCreatedDesc,
        )
        val allVisitorsForPrisoner = visitBookingDetailsMono.t5
        val events = visitBookingDetailsMono.t6
        val notifications = visitBookingDetailsMono.t7
        val visitors = mutableListOf<PrisonerContactDto>()

        visit.visitors?.forEach { visitVisitor ->
          allVisitorsForPrisoner.firstOrNull { it.personId == visitVisitor.nomisPersonId }?.let {
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
        )
      }
      .block(apiTimeout)

    return visitBookingDetails?.let {
      // update the full names for event audit entries
      updateEventUserNames(visitBookingDetails)
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
