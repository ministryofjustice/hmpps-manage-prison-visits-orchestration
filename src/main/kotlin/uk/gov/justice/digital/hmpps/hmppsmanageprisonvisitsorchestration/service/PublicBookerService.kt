package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import jakarta.validation.ValidationException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.BookerPrisonerInfoClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonVisitBookerRegistryClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.BookerPrisonerInfoDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.AuthDetailDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.BookerReference
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.PrisonerContactDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.RestrictionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.VisitorInfoDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.VisitorRestrictionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.VisitorRestrictionType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.BookerAuthFailureException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.NotFoundException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.utils.DateUtils
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.utils.PublicBookerValidationUtil
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.utils.PublicBookerValidationUtil.Companion.PRISONER_VALIDATION_ERROR_MSG
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.utils.PublicBookerValidationUtil.Companion.PRISON_VALIDATION_ERROR_MSG
import java.text.MessageFormat
import java.time.LocalDate

@Service
class PublicBookerService(
  private val prisonVisitBookerRegistryClient: PrisonVisitBookerRegistryClient,
  private val prisonerSearchService: PrisonerSearchService,
  private val prisonerContactService: PrisonerContactService,
  private val prisonService: PrisonService,
  private val dateUtils: DateUtils,
  private val bookerPrisonerInfoClient: BookerPrisonerInfoClient,
  private val publicBookerValidationUtil: PublicBookerValidationUtil,
) {
  companion object {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun bookerAuthorisation(createBookerAuthDetail: AuthDetailDto): BookerReference {
    return prisonVisitBookerRegistryClient.bookerAuthorisation(createBookerAuthDetail) ?: throw BookerAuthFailureException("Failed to authorise booker with details - $createBookerAuthDetail")
  }

  fun getPermittedPrisonersForBooker(bookerReference: String): List<BookerPrisonerInfoDto> {
    val prisonerDetailsList = mutableListOf<BookerPrisonerInfoDto>()
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
        publicBookerValidationUtil.validatePrisoner(prisoner.prisonerId, offenderSearchPrisoner)?.let {
          logger.error("getPermittedPrisonersForBooker " + MessageFormat.format(PRISONER_VALIDATION_ERROR_MSG, prisoner.prisonerId, it))
        } ?: run {
          bookerPrisonerInfoClient.getPermittedPrisonerInfo(offenderSearchPrisoner, prisoner)?.let {
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

    publicBookerValidationUtil.validatePrisoner(prisonerNumber, offenderSearchPrisoner)?.let {
      val message = MessageFormat.format(PRISONER_VALIDATION_ERROR_MSG, prisonerNumber, it)
      logger.error(message)
      throw ValidationException(message)
    }

    val prisonCode = offenderSearchPrisoner.prisonId!!
    // get the prison and validate
    val prison = prisonService.getVSIPPrison(prisonCode)
    publicBookerValidationUtil.validatePrison(prison)?.let {
      val message = MessageFormat.format(PRISON_VALIDATION_ERROR_MSG, prisonCode, prisonerNumber, it)
      logger.error(message)
      throw ValidationException(message)
    }

    return getValidVisitors(bookerReference, prisonerNumber)
  }

  fun validatePrisoner(bookerReference: String, prisonerNumber: String) {
    logger.trace("validate prisoner called for $prisonerNumber with booker reference $bookerReference")
    prisonVisitBookerRegistryClient.validatePrisoner(bookerReference, prisonerNumber)
    logger.trace("validate prisoner successful for $prisonerNumber with booker reference $bookerReference")
  }

  private fun getValidVisitors(bookerReference: String, prisonerNumber: String): List<VisitorInfoDto> {
    val visitorDetailsList = mutableListOf<VisitorInfoDto>()
    val associatedVisitors = prisonVisitBookerRegistryClient.getPermittedVisitorsForBookersAssociatedPrisoner(bookerReference, prisonerNumber)

    if (associatedVisitors.isNotEmpty()) {
      // get approved visitors for a prisoner with a DOB and not BANNED
      var allValidContacts = emptyList<PrisonerContactDto>()
      try {
        allValidContacts = getAllValidContacts(prisonerNumber)
      } catch (nfe: NotFoundException) {
        logger.error("No valid contacts found for prisoner id - $prisonerNumber")
      }

      // filter them through the associated visitor list
      if (allValidContacts.isNotEmpty()) {
        associatedVisitors.forEach { associatedVisitor ->
          allValidContacts.firstOrNull { it.personId == associatedVisitor.visitorId }?.let { contact ->
            val visitorRestrictions = getRestrictions(contact.restrictions)
            visitorDetailsList.add(VisitorInfoDto(contact, visitorRestrictions))
          }
        }
      }
    }

    return visitorDetailsList.toList()
  }

  private fun getAllValidContacts(prisonerNumber: String): List<PrisonerContactDto> {
    return prisonerContactService.getPrisonersApprovedSocialContactsWithDOB(prisonerNumber)
  }

  private fun getMaxExpiryDate(restrictions: List<RestrictionDto>): LocalDate? {
    val expiryDates = restrictions.map { it.expiryDate }
    return if (expiryDates.contains(null)) {
      null
    } else {
      expiryDates.maxWith(Comparator.naturalOrder())
    }
  }

  private fun getRestrictions(restrictions: List<RestrictionDto>): Set<VisitorRestrictionDto> {
    val relevantVisitorRestrictions: MutableSet<VisitorRestrictionDto> = mutableSetOf()

    VisitorRestrictionType.entries.forEach { restrictionType ->
      val restrictionsByType = restrictions.filter { restriction ->
        isRestrictionType(restrictionType, restriction) &&
          hasRestrictionForDate(restriction, dateUtils.getCurrentDate())
      }

      if (restrictionsByType.isNotEmpty()) {
        relevantVisitorRestrictions.add(VisitorRestrictionDto(restrictionType, getMaxExpiryDate(restrictionsByType)))
      }
    }

    return relevantVisitorRestrictions.toSet()
  }

  private fun hasRestrictionForDate(restriction: RestrictionDto, date: LocalDate): Boolean {
    return isRestrictionApplicableForDate(restriction.expiryDate, date)
  }

  private fun isRestrictionType(visitorRestrictionType: VisitorRestrictionType, restriction: RestrictionDto): Boolean {
    return restriction.restrictionType == visitorRestrictionType.toString()
  }

  private fun isRestrictionApplicableForDate(restrictionEndDate: LocalDate?, date: LocalDate): Boolean {
    return (restrictionEndDate == null || (date <= restrictionEndDate))
  }
}
