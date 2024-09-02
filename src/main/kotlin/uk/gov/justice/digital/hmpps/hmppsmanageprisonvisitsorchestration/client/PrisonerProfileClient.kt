package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.PrisonerProfileDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.alerts.api.AlertDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.PrisonerContactDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSummaryDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.InvalidPrisonerProfileException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.filter.VisitSearchRequestFilter
import java.time.Duration

@Component
class PrisonerProfileClient(
  private val prisonApiClient: PrisonApiClient,
  private val alertsApiClient: AlertsApiClient,
  private val prisonerSearchClient: PrisonerSearchClient,
  private val visitSchedulerClient: VisitSchedulerClient,
  private val prisonerContactRegistryClient: PrisonerContactRegistryClient,
  private val prisonRegisterClient: PrisonRegisterClient,
  @Value("\${prisoner.profile.timeout:10s}") private val apiTimeout: Duration,
) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getPrisonerProfile(
    prisonId: String,
    prisonerId: String,
    visitSearchRequestFilter: VisitSearchRequestFilter,
  ): PrisonerProfileDto? {
    LOG.info("getPrisonerProfile - Beginning calls to collect prisoner profile data for prisoner {} in {}", prisonerId, prisonId)
    val prisonerMono = prisonerSearchClient.getPrisonerByIdAsMono(prisonerId)
    val inmateDetailMono = prisonApiClient.getInmateDetailsAsMono(prisonerId)
    val visitBalancesMono = prisonApiClient.getVisitBalancesAsMono(prisonerId)
    val prisonerBookingSummaryMono = prisonApiClient.getBookingsAsMono(prisonId, prisonerId)
    val visitSchedulerMono = visitSchedulerClient.getVisitsAsMono(visitSearchRequestFilter)
    val alertsMono = alertsApiClient.getPrisonerAlertsAsMono(prisonerId)

    return Mono.zip(prisonerMono, inmateDetailMono, visitBalancesMono, prisonerBookingSummaryMono, visitSchedulerMono, alertsMono)
      .map { prisonerProfileMonos ->
        val prisoner = prisonerProfileMonos.t1 ?: throw InvalidPrisonerProfileException("Unable to retrieve offender details from Prisoner Search API")
        val inmateDetails = prisonerProfileMonos.t2 ?: throw InvalidPrisonerProfileException("Unable to retrieve inmate details from Prison API")
        val visitBalances = if (prisonerProfileMonos.t3.isEmpty) null else prisonerProfileMonos.t3.get()
        val prisonerBookingSummary = prisonerProfileMonos.t4.content.firstOrNull()
        val visits = prisonerProfileMonos.t5.content.map { visitDto -> VisitSummaryDto(visitDto = visitDto) }
        val prisonerAlerts = prisonerProfileMonos.t6.content.map { alertResponse -> AlertDto(alertResponse) }

        PrisonerProfileDto(
          prisoner,
          inmateDetails,
          visitBalances,
          prisonerBookingSummary,
          visits,
          prisonerAlerts,
        )
      }
      .block(apiTimeout)?.also { prisonerProfile ->
        LOG.info("getPrisonerProfile - Finishing calls to collect prisoner profile data - Returning prisoner profile - {}, for prisoner {} in {}", prisonerProfile, prisonerId, prisonId)
        setVisitDetails(prisonerProfile)
      }
  }

  private fun setVisitDetails(prisonerProfile: PrisonerProfileDto) {
    val contactsMap = getContactsForPrisoner(prisonerProfile)
    val prisonNamesMap = getPrisonNamesMap()

    prisonerProfile.visits.forEach { visit ->
      contactsMap?.let { setVisitorNames(visit, contactsMap) }
      prisonNamesMap?.let { setPrisonName(visit, prisonNamesMap) }
    }
  }

  private fun getContactsForPrisoner(prisonerProfile: PrisonerProfileDto): Map<Long?, PrisonerContactDto>? {
    try {
      val contacts = prisonerContactRegistryClient.getPrisonersSocialContacts(prisonerProfile.prisonerId, withAddress = false, approvedVisitorsOnly = false)
      return contacts.associateBy { it.personId }
    } catch (e: Exception) {
      // log a message if there is an error but do not terminate the call
      LOG.warn("Exception thrown on prisoner contact registry call - /prisoners/${prisonerProfile.prisonerId}/contacts", e)
    }

    return null
  }

  private fun getPrisonNamesMap(): Map<String, String>? {
    return try {
      val prisonNames = prisonRegisterClient.getPrisonNames()
      prisonNames?.let {
        prisonNames.associateBy { it.prisonId }.mapValues { it.value.prisonName }
      }
    } catch (e: Exception) {
      // log a message if there is an error but do not terminate the call
      LOG.warn("Exception thrown on get prisons call - /prisons/names", e)
      null
    }
  }

  private fun setVisitorNames(visitSummary: VisitSummaryDto, contactsMap: Map<Long?, PrisonerContactDto>) {
    visitSummary.visitors?.forEach { visitor ->
      visitor.firstName = contactsMap[visitor.nomisPersonId]?.firstName
      visitor.lastName = contactsMap[visitor.nomisPersonId]?.lastName
    }
  }

  private fun setPrisonName(visitSummary: VisitSummaryDto, prisonsMap: Map<String, String>) {
    visitSummary.prisonName = prisonsMap[visitSummary.prisonCode]
  }
}
