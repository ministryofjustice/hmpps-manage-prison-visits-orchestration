package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.BookerAuditHistoryClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.BookerPrisonerInfoClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonVisitBookerRegistryClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitSchedulerClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.management.SocialContactsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.AuthDetailDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.BookerHistoryAuditDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.BookerPrisonerInfoDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.BookerReference
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.PermittedVisitorsForPermittedPrisonerBookerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.RegisterPrisonerForBookerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.RegisterVisitorForBookerPrisonerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.VisitorInfoDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.admin.BookerDetailedInfoDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.admin.BookerPrisonerDetailedInfoDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.admin.BookerSearchResultsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.admin.SearchBookerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.enums.BookerPrisonerValidationErrorCodes.REGISTERED_PRISON_NOT_SUPPORTED
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.PrisonerContactDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.RestrictionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.VisitorRestrictionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.VisitorRestrictionType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType.PUBLIC
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.BookerAuthFailureException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.BookerPrisonerValidationException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.NotFoundException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.utils.CurrentDateUtils
import java.time.LocalDate

@Service
class PublicBookerService(
  private val prisonVisitBookerRegistryClient: PrisonVisitBookerRegistryClient,
  private val prisonerContactService: PrisonerContactService,
  private val currentDateUtils: CurrentDateUtils,
  private val bookerPrisonerInfoClient: BookerPrisonerInfoClient,
  private val visitSchedulerClient: VisitSchedulerClient,
  private val prisonerSearchClient: PrisonerSearchClient,
  private val bookerAuditHistoryClient: BookerAuditHistoryClient,
) {
  companion object {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun searchForBooker(searchBookerDto: SearchBookerDto): List<BookerSearchResultsDto> {
    logger.info("Entered PublicBookerService - searchForBooker")
    return prisonVisitBookerRegistryClient.searchForBooker(searchBookerDto)
  }

  fun getBookerDetails(bookerReference: String): BookerDetailedInfoDto {
    logger.info("PublicBookerService - getBookerDetails called for bookerReference : $bookerReference")

    // Get overarching booker info (Such as reference and email)
    val booker = prisonVisitBookerRegistryClient.getBookerByBookerReference(bookerReference)

    // For all the bookers "permitted prisoners", retrieve their entire social contacts list
    val prisonerVisitorsMap = prisonerContactService.getPrisonersContacts(booker.permittedPrisoners.map { it.prisonerId }.toSet())

    // Begin looping each of the bookers "permitted prisoner" and craft a BookerPrisonerDetailedInfoDto
    val prisonerDetailsList = mutableListOf<BookerPrisonerDetailedInfoDto>()
    booker.permittedPrisoners.forEach { prisoner ->
      val permittedPrisonerInfo = bookerPrisonerInfoClient.getPermittedPrisonerInfo(prisoner)
      if (permittedPrisonerInfo == null) {
        logger.error("No prisoner info found for prisoner id - ${prisoner.prisonerId}")
        throw NotFoundException("call to getPermittedPrisonerInfo failed for prisonerId - ${prisoner.prisonerId}, for booker $bookerReference")
      }

      // Filter the contacts in the map retrieved earlier on to only contain the "permitted visitors" for the "permitted prisoner".
      val permittedVisitorInfos = prisonerVisitorsMap.getValue(prisoner.prisonerId).filter { contact -> prisoner.permittedVisitors.map { it.visitorId }.contains(contact.personId) }

      val prisonerDetails = BookerPrisonerDetailedInfoDto(
        permittedPrisonerInfo,
        permittedVisitorInfos,
      )

      prisonerDetailsList.add(prisonerDetails)
    }

    return BookerDetailedInfoDto(
      reference = booker.reference,
      email = booker.email,
      permittedPrisoners = prisonerDetailsList,
    )
  }

  fun getSocialContacts(bookerReference: String, prisonerId: String): List<SocialContactsDto> {
    logger.info("PublicBookerService - getSocialContacts called for bookerReference : $bookerReference, prisonerId: $prisonerId")

    // Get booker and prisoner and visitor details
    val booker = prisonVisitBookerRegistryClient.getBookerByBookerReference(bookerReference)

    val permittedPrisoner = booker.permittedPrisoners.firstOrNull { it.prisonerId == prisonerId } ?: throw NotFoundException("Prisoner with number - $prisonerId not found for booker reference - $bookerReference")

    // Get all visitors for prisoner except the ones already on the permittedVisitors list
    val socialContactsNotRegistered = try {
      prisonerContactService.getPrisonerSocialContacts(prisonerId).filterNot { socialContact ->
        permittedPrisoner.permittedVisitors.map { it.visitorId }.contains(socialContact.personId)
      }.map { visitorNotRegistered -> SocialContactsDto(visitorNotRegistered) }
    } catch (_: NotFoundException) {
      logger.info("No approved visitors found for $prisonerId - $prisonerId, returning an empty list")
      emptyList()
    }

    // set the last approved dates for each visitor
    if (socialContactsNotRegistered.isNotEmpty()) {
      setLastApprovedDate(prisonerId, socialContactsNotRegistered)
    }
    return socialContactsNotRegistered
  }

  fun bookerAuthorisation(createBookerAuthDetail: AuthDetailDto): BookerReference = prisonVisitBookerRegistryClient.bookerAuthorisation(createBookerAuthDetail) ?: throw BookerAuthFailureException("Failed to authorise booker with details - $createBookerAuthDetail")

  fun getPermittedPrisonersForBooker(bookerReference: String): List<BookerPrisonerInfoDto> {
    val prisonerDetailsList = mutableListOf<BookerPrisonerInfoDto>()
    val prisoners = prisonVisitBookerRegistryClient.getPermittedPrisonersForBooker(bookerReference)
    logger.debug("getPermittedPrisonersForBooker ${prisoners.size} prisoners found for bookerReference : $bookerReference")

    prisoners.forEach { prisoner ->
      bookerPrisonerInfoClient.getPermittedPrisonerInfo(prisoner)?.let {
        prisonerDetailsList.add(it)
      }
    }

    return prisonerDetailsList
  }

  fun getPermittedVisitorsForPermittedPrisonerAndBooker(bookerReference: String, prisonerNumber: String): List<VisitorInfoDto> {
    prisonVisitBookerRegistryClient.getPermittedPrisonersForBooker(bookerReference)
      .firstOrNull { it.prisonerId == prisonerNumber }
      ?: throw NotFoundException("Prisoner with number - $prisonerNumber not found for booker reference - $bookerReference")

    return getValidVisitors(bookerReference, prisonerNumber).sortedWith(compareBy({ it.lastName.uppercase() }, { it.firstName.uppercase() }))
  }

  fun validatePrisoner(bookerReference: String, prisonerNumber: String) {
    logger.trace("validate prisoner called for $prisonerNumber with booker reference $bookerReference")
    // run the booker-registry checks
    prisonVisitBookerRegistryClient.validatePrisoner(bookerReference, prisonerNumber)

    // finally, check if the prisoner's prison is supported on Visits
    prisonerSearchClient.getPrisonerById(prisonerNumber).prisonId?.let { prisonId ->
      if (!isPrisonSupportedOnVisits(prisonId)) {
        throw BookerPrisonerValidationException(REGISTERED_PRISON_NOT_SUPPORTED)
      }
    }

    logger.trace("validate prisoner successful for $prisonerNumber with booker reference $bookerReference")
  }

  fun registerPrisoner(bookerReference: String, registerPrisonerForBookerDto: RegisterPrisonerForBookerDto) {
    logger.trace("register prisoner called for ${registerPrisonerForBookerDto.prisonerId} with booker reference $bookerReference")
    prisonVisitBookerRegistryClient.registerPrisoner(bookerReference, registerPrisonerForBookerDto)
  }

  fun registerVisitorForBookerPrisoner(bookerReference: String, prisonerId: String, registerVisitorForBookerPrisonerDto: RegisterVisitorForBookerPrisonerDto): PermittedVisitorsForPermittedPrisonerBookerDto {
    logger.info("register visitor ${registerVisitorForBookerPrisonerDto.visitorId} for booker prisoner $prisonerId with booker reference $bookerReference")
    return prisonVisitBookerRegistryClient.registerVisitorForBookerPrisoner(bookerReference, prisonerId, registerVisitorForBookerPrisonerDto)
  }

  fun unlinkBookerPrisonerVisitor(bookerReference: String, prisonerNumber: String, visitorId: String) {
    logger.trace("Entered PublicBookerService - unlinkBookerPrisonerVisitor - for booker $bookerReference, prisoner $prisonerNumber, visitor $visitorId")

    prisonVisitBookerRegistryClient.unlinkBookerPrisonerVisitor(bookerReference, prisonerNumber, visitorId)
  }

  private fun isPrisonSupportedOnVisits(prisonId: String): Boolean = visitSchedulerClient.getSupportedPrisons(PUBLIC).map { it.uppercase() }.contains(prisonId.uppercase())

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

  private fun getAllValidContacts(prisonerNumber: String): List<PrisonerContactDto> = prisonerContactService.getPrisonersApprovedSocialContactsWithDOB(prisonerNumber)

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
          hasRestrictionForDate(restriction, currentDateUtils.getCurrentDate())
      }

      if (restrictionsByType.isNotEmpty()) {
        relevantVisitorRestrictions.add(VisitorRestrictionDto(restrictionType, getMaxExpiryDate(restrictionsByType)))
      }
    }

    return relevantVisitorRestrictions.toSet()
  }

  private fun hasRestrictionForDate(restriction: RestrictionDto, date: LocalDate): Boolean = isRestrictionApplicableForDate(restriction.expiryDate, date)

  private fun isRestrictionType(visitorRestrictionType: VisitorRestrictionType, restriction: RestrictionDto): Boolean = restriction.restrictionType == visitorRestrictionType.toString()

  private fun isRestrictionApplicableForDate(restrictionEndDate: LocalDate?, date: LocalDate): Boolean = (restrictionEndDate == null || (date <= restrictionEndDate))

  fun getBookerAudit(bookerReference: String): List<BookerHistoryAuditDto> = bookerAuditHistoryClient.getBookerAuditHistory(bookerReference)

  private fun setLastApprovedDate(prisonerId: String, socialContacts: List<SocialContactsDto>) {
    val socialContactsNomisPersonIds = socialContacts.map { it.visitorId }.toSet().toList()
    val lastApprovedDates = visitSchedulerClient.findLastApprovedDateForVisitor(prisonerId, socialContactsNomisPersonIds)
    if (lastApprovedDates != null && lastApprovedDates.isNotEmpty()) {
      socialContacts.forEach { socialContact ->
        socialContact.lastApprovedForVisitDate = lastApprovedDates.firstOrNull { it.nomisPersonId == socialContact.visitorId }?.lastApprovedVisitDate
      }
    }
  }
}
