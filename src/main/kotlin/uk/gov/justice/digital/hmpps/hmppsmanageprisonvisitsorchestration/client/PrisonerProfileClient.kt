package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.PrisonerProfileDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.ContactDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.register.PrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.InvalidPrisonerProfileException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.filter.VisitSearchRequestFilter
import java.time.Duration

@Component
class PrisonerProfileClient(
  private val prisonApiClient: PrisonApiClient,
  private val prisonerOffenderSearchClient: PrisonerOffenderSearchClient,
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
    val prisonerMono = prisonerOffenderSearchClient.getPrisonerByIdAsMono(prisonerId)
    val inmateDetailMono = prisonApiClient.getInmateDetailsAsMono(prisonerId)
    val visitBalancesMono = prisonApiClient.getVisitBalancesAsMono(prisonerId)
    val prisonerBookingSummaryMono = prisonApiClient.getBookingsAsMono(prisonId, prisonerId)
    val visitSchedulerMono = visitSchedulerClient.getVisitsAsMono(visitSearchRequestFilter)

    return Mono.zip(prisonerMono, inmateDetailMono, visitBalancesMono, prisonerBookingSummaryMono, visitSchedulerMono)
      .map {
        PrisonerProfileDto(
          it.t1
            ?: throw InvalidPrisonerProfileException("Unable to retrieve offender details from Prison Offender Search API"),
          it.t2 ?: throw InvalidPrisonerProfileException("Unable to retrieve inmate details from Prison API"),
          if (it.t3.isEmpty) null else it.t3.get(),
          it.t4.content.firstOrNull(),
          it.t5.content,
        )
      }
      .block(apiTimeout)?.also { prisonerProfile ->
        setVisitDetails(prisonerProfile)
      }
  }

  private fun setVisitDetails(prisonerProfile: PrisonerProfileDto) {
    val contactsMap = getContactsForPrisoner(prisonerProfile)
    val prisonsMap = getAvailablePrisons()

    prisonerProfile.visits.forEach { visit ->
      contactsMap?.let { setVisitorNames(visit, contactsMap) }
      prisonsMap?.let { setPrisonNames(visit, prisonsMap) }
    }
  }

  private fun getContactsForPrisoner(prisonerProfile: PrisonerProfileDto): Map<Long?, ContactDto>? {
    try {
      val contacts = prisonerContactRegistryClient.getPrisonersSocialContacts(prisonerProfile.prisonerId)
      contacts?.let {
        return contacts.associateBy { it.personId }
      }
    } catch (e: Exception) {
      // log a message if there is an error but do not terminate the call
      LOG.warn("Exception thrown on prisoner contact registry call - /prisoners/${prisonerProfile.prisonerId}/contacts", e)
    }

    return null
  }

  private fun getAvailablePrisons(): Map<String, PrisonDto>? {
    try {
      val prisons = prisonRegisterClient.getPrisons()
      prisons?.let {
        return prisons.associateBy { it.prisonId }
      }
    } catch (e: Exception) {
      // log a message if there is an error but do not terminate the call
      LOG.warn("Exception thrown on get prisons call - /prisons", e)
    }

    return null
  }

  private fun setVisitorNames(visit: VisitDto, contactsMap: Map<Long?, ContactDto>) {
    visit.visitors?.forEach { visitor ->
      visitor.firstName = contactsMap[visitor.nomisPersonId]?.firstName
      visitor.lastName = contactsMap[visitor.nomisPersonId]?.lastName
    }
  }

  private fun setPrisonNames(visit: VisitDto, prisonsMap: Map<String, PrisonDto>) {
    visit.prisonName = prisonsMap[visit.prisonCode]?.prisonName
  }
}
