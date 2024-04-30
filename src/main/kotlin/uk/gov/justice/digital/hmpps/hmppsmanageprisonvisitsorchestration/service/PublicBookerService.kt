package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonVisitBookerRegistryClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.AuthDetailDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.BookerReference
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.PrisonerContactDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.VisitorBasicInfoDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerBasicInfoDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.PrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.BookerAuthFailureException
import java.time.LocalDate

@Service
class PublicBookerService(
  private val prisonVisitBookerRegistryClient: PrisonVisitBookerRegistryClient,
  private val prisonerSearchClient: PrisonerSearchClient,
  private val prisonerContactService: PrisonerContactService,
  private val prisonService: PrisonService,
) {
  companion object {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
    const val BANNED_RESTRICTION_TYPE = "BAN"
  }

  fun bookerAuthorisation(createBookerAuthDetail: AuthDetailDto): BookerReference {
    return prisonVisitBookerRegistryClient.bookerAuthorisation(createBookerAuthDetail) ?: throw BookerAuthFailureException("Failed to authorise booker with details - $createBookerAuthDetail")
  }

  fun getBookersPrisoners(bookerReference: String): List<PrisonerBasicInfoDto> {
    val prisonerDetailsList = mutableListOf<PrisonerBasicInfoDto>()
    val prisoners = prisonVisitBookerRegistryClient.getPrisonersForBooker(bookerReference)

    prisoners?.forEach {
      val prisonerDetails = getPrisonerDetails(it.prisonerNumber)
      prisonerDetails?.let { prisoner ->
        prisonerDetailsList.add(PrisonerBasicInfoDto(it.prisonerNumber, prisoner))
      }
    }

    return prisonerDetailsList
  }

  fun getVisitorsForBookersPrisoner(prisonCode: String, bookerReference: String, prisonerNumber: String): List<VisitorBasicInfoDto> {
    val prison = prisonService.getPrison(prisonCode)
    // TODO - check if prison is ACTIVE for public?

    val visitorDetailsList = mutableListOf<VisitorBasicInfoDto>()
    val associatedVisitors = prisonVisitBookerRegistryClient.getVisitorsForBookersAssociatedPrisoner(bookerReference, prisonerNumber) ?: emptyList()

    if (associatedVisitors.isNotEmpty()) {
      // get approved visitors for a prisoner with a DOB and not BANNED
      val allValidContacts = getAllValidContacts(prison, prisonerNumber)

      // filter them through the associated visitor list
      allValidContacts?.let {
        associatedVisitors.forEach { associatedVisitor ->
          allValidContacts.firstOrNull { it.personId == associatedVisitor.personId }?.let { contact ->
            visitorDetailsList.add(VisitorBasicInfoDto(contact))
          }
        }
      }
    }

    return visitorDetailsList
  }

  private fun getPrisonerDetails(prisonerNumber: String): PrisonerDto? {
    var prisoner: PrisonerDto? = null

    try {
      prisoner = prisonerSearchClient.getPrisonerById(prisonerNumber)
    } catch (e: WebClientResponseException) {
      logger.info("Failed to get details for prisoner - $prisonerNumber, error = ${e.message}")
      if (e.statusCode != HttpStatus.NOT_FOUND) {
        throw e
      }
    }

    return prisoner
  }

  private fun getAllValidContacts(prison: PrisonDto, prisonerNumber: String): List<PrisonerContactDto>? {
    val lastBookableDate = getLastBookableSessionDate(prison)

    return prisonerContactService.getAllPrisonersSocialContacts(prisonerNumber)
      ?.filter { isContactApproved(it) }
      ?.filter { hasContactGotDateOfBirth(it) }
      ?.filterNot { prisonerContactService.isContactBannedBeforeDate(it, lastBookableDate) }
  }

  private fun isContactApproved(contact: PrisonerContactDto): Boolean {
    return contact.approvedVisitor
  }

  private fun hasContactGotDateOfBirth(contact: PrisonerContactDto): Boolean {
    return contact.dateOfBirth != null
  }

  private fun getLastBookableSessionDate(prison: PrisonDto): LocalDate {
    return prisonService.getLastBookableSessionDate(prison, LocalDate.now())
  }
}
