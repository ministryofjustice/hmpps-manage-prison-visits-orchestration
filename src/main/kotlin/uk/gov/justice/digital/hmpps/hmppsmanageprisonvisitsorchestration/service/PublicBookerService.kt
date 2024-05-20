package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import jakarta.validation.ValidationException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonVisitBookerRegistryClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.AuthDetailDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.BookerReference
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.PermittedPrisonerForBookerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.PrisonerContactDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.VisitorInfoDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerInfoDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSchedulerPrisonDto
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
    const val PRISONER_VALIDATION_ERROR_MSG = "prisoner validation for prisoner number - {0} failed with error - {1}"
    const val PRISON_VALIDATION_ERROR_MSG = "prison validation for prison code - {0} for prisoner number - {1} failed with error - {2}"
  }

  fun bookerAuthorisation(createBookerAuthDetail: AuthDetailDto): BookerReference {
    return prisonVisitBookerRegistryClient.bookerAuthorisation(createBookerAuthDetail) ?: throw BookerAuthFailureException("Failed to authorise booker with details - $createBookerAuthDetail")
  }

  fun getPermittedPrisonersForBooker(bookerReference: String): List<PrisonerInfoDto> {
    val prisonerDetailsList = mutableListOf<PrisonerInfoDto>()
    val prisoners = prisonVisitBookerRegistryClient.getPermittedVisitorsForPermittedPrisonerAndBooker(bookerReference)
    logger.debug("getPermittedPrisonersForBooker ${prisoners.size} prisoners found for bookerReference : $bookerReference")

    prisoners.forEach { prisoner ->
      // get the offender details from prisoner search and validate but do not throw an exception
      var offenderSearchPrisoner: PrisonerDto? = null
      try {
        offenderSearchPrisoner = prisonerSearchService.getPrisoner(prisoner.prisonerId)
      } catch (nfe: NotFoundException) {
        logger.error("getPermittedPrisonersForBooker Prisoner with id - ${prisoner.prisonerId} not found on offender search")
      }

      offenderSearchPrisoner?.let {
        validatePrisoner(prisoner.prisonerId, offenderSearchPrisoner)?.let {
          logger.error("getPermittedPrisonersForBooker " + MessageFormat.format(PRISONER_VALIDATION_ERROR_MSG, prisoner.prisonerId, it))
        } ?: run {
          getPermittedPrisonerInfo(offenderSearchPrisoner, prisoner)?.let {
            prisonerDetailsList.add(it)
          }
        }
      }
    }

    return prisonerDetailsList
  }

  fun getPermittedVisitorsForPermittedPrisonerAndBooker(bookerReference: String, prisonerNumber: String): List<VisitorInfoDto> {
    prisonVisitBookerRegistryClient.getPermittedVisitorsForPermittedPrisonerAndBooker(bookerReference)
      .firstOrNull { it.prisonerId == prisonerNumber }
      ?: throw NotFoundException("Prisoner with number - $prisonerNumber not found for booker reference - $bookerReference")

    // get the offender details from prisoner search and validate
    val offenderSearchPrisoner = prisonerSearchService.getPrisoner(prisonerNumber)

    validatePrisoner(prisonerNumber, offenderSearchPrisoner)?.let {
      val message = MessageFormat.format(PRISONER_VALIDATION_ERROR_MSG, prisonerNumber, it)
      logger.error(message)
      throw ValidationException(message)
    }

    val prisonCode = offenderSearchPrisoner.prisonId!!
    // get the prison and validate
    val prison = prisonService.getVSIPPrison(prisonCode)
    validatePrison(prison)?.let {
      val message = MessageFormat.format(PRISON_VALIDATION_ERROR_MSG, prisonCode, prisonerNumber, it)
      logger.error(message)
      throw ValidationException(message)
    }

    return getValidVisitors(bookerReference, prisonerNumber, prison)
  }

  private fun getPermittedPrisonerInfo(offenderSearchPrisoner: PrisonerDto, bookerPrisoner: PermittedPrisonerForBookerDto): PrisonerInfoDto? {
    val prisonCode = offenderSearchPrisoner.prisonId!!
    val prison: VisitSchedulerPrisonDto
    try {
      prison = prisonService.getVSIPPrison(prisonCode)
    } catch (ne: NotFoundException) {
      logger.error("Prison with code - $prisonCode, not found on visit-scheduler")
      return null
    }

    validatePrison(prison)?.let {
      logger.error(MessageFormat.format(PRISON_VALIDATION_ERROR_MSG, prisonCode, bookerPrisoner.prisonerId, it))
    } ?: run {
      return PrisonerInfoDto(bookerPrisoner.prisonerId, offenderSearchPrisoner)
    }

    return null
  }

  private fun getValidVisitors(bookerReference: String, prisonerNumber: String, prison: VisitSchedulerPrisonDto): List<VisitorInfoDto> {
    val visitorDetailsList = mutableListOf<VisitorInfoDto>()
    val associatedVisitors = prisonVisitBookerRegistryClient.getPermittedVisitorsForBookersAssociatedPrisoner(bookerReference, prisonerNumber)

    if (associatedVisitors.isNotEmpty()) {
      // get approved visitors for a prisoner with a DOB and not BANNED
      var allValidContacts = emptyList<PrisonerContactDto>()
      try {
        allValidContacts = getAllValidContacts(prison, prisonerNumber)
      } catch (nfe: NotFoundException) {
        logger.error("No valid contacts found for prisoner id - $prisonerNumber")
      }

      // filter them through the associated visitor list
      if (allValidContacts.isNotEmpty()) {
        associatedVisitors.forEach { associatedVisitor ->
          allValidContacts.firstOrNull { it.personId == associatedVisitor.visitorId }?.let { contact ->
            visitorDetailsList.add(VisitorInfoDto(contact))
          }
        }
      }
    }

    return visitorDetailsList.toList()
  }

  private fun validatePrisoner(prisonerNumber: String, offenderSearchPrisoner: PrisonerDto): String? {
    var errorMessage: String? = null

    if (offenderSearchPrisoner.prisonId == null) {
      // if the offender was found but without a prison code throw an exception
      errorMessage = "Prisoner - $prisonerNumber on prisoner search does not have a valid prison"
    }

    return errorMessage
  }

  private fun validatePrison(prison: VisitSchedulerPrisonDto): String? {
    var errorMessage: String? = null

    if (!prisonService.isActive(prison)) {
      errorMessage = "Prison with code - ${prison.code}, is not active on visit-scheduler"
    } else {
      if (!prisonService.isActive(prison, UserType.PUBLIC)) {
        errorMessage = "Prison with code - ${prison.code}, is not active for public users on visit-scheduler"
      }
    }

    return errorMessage
  }

  private fun getAllValidContacts(prison: VisitSchedulerPrisonDto, prisonerNumber: String): List<PrisonerContactDto> {
    val lastBookableDate = getLastBookableSessionDate(prison)

    return prisonerContactService.getPrisonersSocialContactsWithDOBAndNotBannedBeforeDate(prisonerNumber, lastBookableDate)
  }

  private fun getLastBookableSessionDate(prison: VisitSchedulerPrisonDto): LocalDate {
    return prisonService.getLastBookableSessionDate(prison, LocalDate.now())
  }
}
