package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import jakarta.validation.ValidationException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonVisitBookerRegistryClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.AuthDetailDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.BookerReference
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.PrisonerContactDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.VisitorInfoDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerInfoDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.PrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.BookerAuthFailureException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.NotFoundException
import java.text.MessageFormat
import java.time.LocalDate

@Service
class PublicBookerService(
  private val prisonVisitBookerRegistryClient: PrisonVisitBookerRegistryClient,
  private val prisonerSearchService: PrisonerSearchService,
  private val prisonerContactService: PrisonerContactService,
  private val prisonService: PrisonService,
) {
  companion object {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
    const val BANNED_RESTRICTION_TYPE = "BAN"
    const val PRISONER_VALIDATION_ERROR_MSG = "prisoner validation for prisoner number - {0} failed with error - {1}"
    const val PRISON_VALIDATION_ERROR_MSG = "prison validation for prison code - {0} for prisoner number - {1} failed with error - {2}"
  }

  fun bookerAuthorisation(createBookerAuthDetail: AuthDetailDto): BookerReference {
    return prisonVisitBookerRegistryClient.bookerAuthorisation(createBookerAuthDetail) ?: throw BookerAuthFailureException("Failed to authorise booker with details - $createBookerAuthDetail")
  }

  fun getBookersPrisoners(bookerReference: String): List<PrisonerInfoDto> {
    val prisonerDetailsList = mutableListOf<PrisonerInfoDto>()
    val prisoners = prisonVisitBookerRegistryClient.getPrisonersForBooker(bookerReference)

    prisoners?.forEach { prisoner ->
      // get the offender details from prisoner search and validate but do not throw an exception
      val offenderSearchPrisoner = prisonerSearchService.getPrisoner(prisoner.prisonerNumber)

      validatePrisoner(prisoner.prisonerNumber, offenderSearchPrisoner)?.let {
        logger.error(MessageFormat.format(PRISONER_VALIDATION_ERROR_MSG, prisoner.prisonerNumber, it))
      } ?: run {
        if (offenderSearchPrisoner?.prisonId != null) {
          val prisonCode = offenderSearchPrisoner.prisonId
          val prison = prisonService.getPrison(prisonCode)
          validatePrison(prisonCode, prison)?.let {
            logger.error(MessageFormat.format(PRISONER_VALIDATION_ERROR_MSG, prisonCode, prisoner.prisonerNumber, it))
          } ?: run {
            prisonerDetailsList.add(PrisonerInfoDto(prisoner.prisonerNumber, offenderSearchPrisoner))
          }
        }
      }
    }

    return prisonerDetailsList
  }

  fun getVisitorsForBookersPrisoner(bookerReference: String, prisonerNumber: String): List<VisitorInfoDto> {
    // get the prisoner from booker registry
    prisonVisitBookerRegistryClient.getPrisonersForBooker(bookerReference)
      ?.firstOrNull { it.prisonerNumber == prisonerNumber }
      ?: throw NotFoundException("Prisoner with number - $prisonerNumber not found for booker reference - $bookerReference")

    // get the offender details from prisoner search and validate
    val offenderSearchPrisoner = prisonerSearchService.getPrisoner(prisonerNumber)

    validatePrisoner(prisonerNumber, offenderSearchPrisoner)?.let {
      val message = MessageFormat.format(PRISONER_VALIDATION_ERROR_MSG, prisonerNumber, it)
      logger.error(message)
      throw ValidationException(message)
    }

    val prisonCode = offenderSearchPrisoner!!.prisonId!!
    // get the prison and validate
    val prison = prisonService.getPrison(prisonCode)
    validatePrison(prisonCode, prison)?.let {
      val message = MessageFormat.format(PRISON_VALIDATION_ERROR_MSG, prisonCode, prisonerNumber, it)
      logger.error(message)
      throw ValidationException(message)
    }
    val visitorDetailsList = mutableListOf<VisitorInfoDto>()
    val associatedVisitors = prisonVisitBookerRegistryClient.getVisitorsForBookersAssociatedPrisoner(bookerReference, prisonerNumber) ?: emptyList()

    if (associatedVisitors.isNotEmpty()) {
      // get approved visitors for a prisoner with a DOB and not BANNED
      val allValidContacts = getAllValidContacts(prison!!, prisonerNumber)

      // filter them through the associated visitor list
      allValidContacts?.let {
        associatedVisitors.forEach { associatedVisitor ->
          allValidContacts.firstOrNull { it.personId == associatedVisitor.personId }?.let { contact ->
            visitorDetailsList.add(VisitorInfoDto(contact))
          }
        }
      }
    }

    return visitorDetailsList
  }

  private fun validatePrisoner(prisonerNumber: String, offenderSearchPrisoner: PrisonerDto?): String? {
    var errorMessage: String? = null
    if (offenderSearchPrisoner != null) {
      if (offenderSearchPrisoner.prisonId == null) {
        // if the offender was found but without a prison code throw an exception
        errorMessage = "Prisoner - $prisonerNumber on prisoner search does not have a valid prison"
      }
    } else {
      errorMessage = "Prisoner with id - $prisonerNumber not found on offender search"
    }

    return errorMessage
  }

  private fun validatePrison(prisonCode: String, prison: PrisonDto?): String? {
    var errorMessage: String? = null
    if (prison != null) {
      if (!prisonService.isActive(prison)) {
        errorMessage = "Prison with code - ${prison.code}, is not active on visit-scheduler"
      } else {
        if (!prisonService.isActive(prison, UserType.PUBLIC)) {
          errorMessage = "Prison with code - ${prison.code}, is not active for public users on visit-scheduler"
        }
      }
    } else {
      errorMessage = "Prison with code - $prisonCode, not found on visit-scheduler"
    }

    return errorMessage
  }

  private fun getAllValidContacts(prison: PrisonDto, prisonerNumber: String): List<PrisonerContactDto>? {
    val lastBookableDate = getLastBookableSessionDate(prison)

    return prisonerContactService.getAllPrisonersSocialContacts(prisonerNumber)
      ?.filter {
        isContactApproved(it)
          .and(hasContactGotDateOfBirth(it))
          .and(!prisonerContactService.isContactBannedBeforeDate(it, lastBookableDate))
      }
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
