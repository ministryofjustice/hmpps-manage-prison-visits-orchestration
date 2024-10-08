package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonRegisterClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitSchedulerClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.PrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.register.PrisonRegisterContactDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.DateRange
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSchedulerPrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.prisons.IsExcludeDateDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.prisons.PrisonExcludeDateDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.utils.DateUtils
import java.time.LocalDate
import java.util.function.Predicate

@Service
class PrisonService(
  val visitSchedulerClient: VisitSchedulerClient,
  private val prisonRegisterClient: PrisonRegisterClient,
  private val manageUsersService: ManageUsersService,
  private val dateUtils: DateUtils,
) {
  companion object {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
  }

  /**
   * Gets the prison details from VSIP and also the prison name from prison-register.
   * Only use if the name is also needed.
   */
  fun getPrisonWithName(prisonCode: String): PrisonDto {
    val visitSchedulerPrison = visitSchedulerClient.getPrison(prisonCode)
    val prisonRegisterPrison = prisonRegisterClient.getPrison(prisonCode)
    val prisonRegisterPrisonContractDetails = prisonRegisterClient.getPrisonContactDetails(prisonCode).orElse(PrisonRegisterContactDetailsDto())

    return PrisonDto(visitSchedulerPrison, prisonRegisterPrison, prisonRegisterPrisonContractDetails)
  }

  fun getVSIPPrison(prisonCode: String): VisitSchedulerPrisonDto {
    return visitSchedulerClient.getPrison(prisonCode)
  }

  fun isActive(prison: VisitSchedulerPrisonDto): Boolean {
    return prison.active
  }

  fun isActive(prison: VisitSchedulerPrisonDto, userType: UserType): Boolean {
    return prison.clients.firstOrNull { it.userType == userType }?.active ?: false
  }

  fun getLastBookableSessionDate(prison: VisitSchedulerPrisonDto, date: LocalDate): LocalDate {
    return date.plusDays(prison.policyNoticeDaysMax.toLong())
  }

  fun getSupportedPrisons(type: UserType): List<String> {
    return visitSchedulerClient.getSupportedPrisons(type)
  }

  fun getFutureExcludeDatesForPrison(prisonCode: String): List<PrisonExcludeDateDto> {
    val futureDatesPredicate: Predicate<PrisonExcludeDateDto> = Predicate { it.excludeDate >= dateUtils.getCurrentDate() }
    return getExcludeDatesForPrison(prisonCode, futureDatesPredicate).sortedBy { it.excludeDate }
  }

  fun getPastExcludeDatesForPrison(prisonCode: String): List<PrisonExcludeDateDto> {
    val pastDatesPredicate: Predicate<PrisonExcludeDateDto> = Predicate { it.excludeDate < dateUtils.getCurrentDate() }
    return getExcludeDatesForPrison(prisonCode, pastDatesPredicate).sortedByDescending { it.excludeDate }
  }

  private fun getExcludeDatesForPrison(prisonCode: String, excludeDatesFilter: Predicate<PrisonExcludeDateDto>): List<PrisonExcludeDateDto> {
    return getExcludeDatesForPrison(prisonCode).filter { excludeDatesFilter.test(it) }.also {
      setActionedByFullName(it)
    }
  }

  fun isDateExcludedForPrisonVisits(prisonCode: String, date: LocalDate): IsExcludeDateDto {
    logger.trace("isDateExcluded - prison - {}, date - {}", prisonCode, date)
    val isExcluded = getExcludeDatesForPrison(prisonCode).map { it.excludeDate }.contains(date)
    logger.trace("isDateExcluded - prison - {}, date - {}, isExcluded - {}", prisonCode, date, isExcluded)
    return IsExcludeDateDto(isExcluded)
  }

  fun addExcludeDateForPrison(prisonCode: String, prisonExcludeDate: PrisonExcludeDateDto): List<LocalDate> {
    return visitSchedulerClient.addPrisonExcludeDate(prisonCode, prisonExcludeDate)?.sortedByDescending { it } ?: emptyList()
  }

  fun removeExcludeDateForPrison(prisonCode: String, prisonExcludeDate: PrisonExcludeDateDto): List<LocalDate> {
    return visitSchedulerClient.removePrisonExcludeDate(prisonCode, prisonExcludeDate)?.sortedByDescending { it } ?: emptyList()
  }

  fun getToDaysBookableDateRange(
    prisonCode: String,
    fromDateOverride: Int? = null,
    toDateOverride: Int? = null,
  ): DateRange {
    val prison = visitSchedulerClient.getPrison(prisonCode)
    return dateUtils.getToDaysDateRange(prison = prison, minOverride = fromDateOverride, maxOverride = toDateOverride)
  }

  private fun getExcludeDatesForPrison(prisonCode: String): List<PrisonExcludeDateDto> {
    return visitSchedulerClient.getPrisonExcludeDates(prisonCode) ?: emptyList()
  }

  private fun setActionedByFullName(excludeDates: List<PrisonExcludeDateDto>): List<PrisonExcludeDateDto> {
    if (excludeDates.isNotEmpty()) {
      val userNameMap = getUserNamesMap(excludeDates.map { it.actionedBy }.toSet())

      for (excludeDate in excludeDates) {
        excludeDate.actionedBy = userNameMap[excludeDate.actionedBy] ?: excludeDate.actionedBy
      }
    }

    return excludeDates
  }

  /**
   * returns Map<String, String> where key = username, value = full name.
   */
  private fun getUserNamesMap(usernames: Set<String>): Map<String, String> {
    val userNameMap = HashMap<String, String>()
    for (username in usernames) {
      userNameMap[username] = manageUsersService.getUserFullName(username, userNameIfNotAvailable = username)
    }

    return userNameMap
  }
}
