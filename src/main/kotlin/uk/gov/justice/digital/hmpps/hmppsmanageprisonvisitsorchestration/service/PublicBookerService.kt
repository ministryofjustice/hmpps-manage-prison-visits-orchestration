package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonVisitBookerRegistryClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonerContactRegistryClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.AuthDetailDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.BookerReference
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.PrisonerContactDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.VisitorBasicInfoDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerBasicInfoDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.BookerAuthFailureException

@Service
class PublicBookerService(
  private val prisonVisitBookerRegistryClient: PrisonVisitBookerRegistryClient,
  private val prisonerSearchClient: PrisonerSearchClient,
  private val prisonerContactRegistryClient: PrisonerContactRegistryClient,
) {
  companion object {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
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

  fun getVisitorsForBookersPrisoner(bookerReference: String, prisonerNumber: String): List<VisitorBasicInfoDto> {
    val visitorDetailsList = mutableListOf<VisitorBasicInfoDto>()
    val associatedVisitors = prisonVisitBookerRegistryClient.getVisitorsForBookersAssociatedPrisoner(bookerReference, prisonerNumber) ?: emptyList()

    if (associatedVisitors.isNotEmpty()) {
      // get approved visitors only and those who have a DOB
      val allValidContacts =
        getPrisonersSocialContacts(prisonerNumber)
          ?.filter { isContactValidForPublicBooking(it) }

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

  private fun getPrisonersSocialContacts(prisonerNumber: String): List<PrisonerContactDto>? {
    var contacts: List<PrisonerContactDto>? = null

    try {
      contacts = prisonerContactRegistryClient.getPrisonersSocialContacts(prisonerNumber, false)
    } catch (e: WebClientResponseException) {
      logger.info("Failed to get social contacts for prisoner - $prisonerNumber, error = ${e.message}")
      if (e.statusCode != HttpStatus.NOT_FOUND) {
        throw e
      }
    }

    return contacts
  }

  private fun isContactValidForPublicBooking(contact: PrisonerContactDto): Boolean {
    // TODO - confirm if required or we send back the flag and the front end decides to show it or not
    return contact.approvedVisitor && contact.dateOfBirth != null
  }
}
