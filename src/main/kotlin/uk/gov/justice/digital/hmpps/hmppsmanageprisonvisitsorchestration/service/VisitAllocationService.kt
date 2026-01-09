package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import jakarta.validation.ValidationException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.IncentivesApiClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitAllocationApiClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.incentives.IncentiveLevelDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation.PrisonerBalanceAdjustmentDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation.PrisonerBalanceDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation.VisitOrderHistoryAttributesDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation.VisitOrderHistoryDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation.VisitOrderHistoryDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation.enums.VisitOrderHistoryAttributeType
import java.time.LocalDate
import java.util.function.Predicate

@Service
class VisitAllocationService(
  private val manageUsersService: ManageUsersService,
  private val prisonerSearchService: PrisonerSearchService,
  private val visitAllocationApiClient: VisitAllocationApiClient,
  private val incentivesApiClient: IncentivesApiClient,
) {
  companion object {
    const val SYSTEM_USER_NAME = "SYSTEM"
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
  }

  private val hasAttributesWithIncentiveLevel: Predicate<VisitOrderHistoryDto> = Predicate {
    it.attributes.any { attribute -> hasIncentiveLevel.test(attribute) }
  }

  private val hasIncentiveLevel: Predicate<VisitOrderHistoryAttributesDto> = Predicate {
    it.attributeType == VisitOrderHistoryAttributeType.INCENTIVE_LEVEL
  }

  fun getPrisonerVisitOrderBalance(prisonerId: String, staffPrisonId: String): PrisonerBalanceDto {
    logger.info("Entered VisitAllocationService - getPrisonerVisitOrderBalance for prisonerId $prisonerId and staff prisonId $staffPrisonId")

    val prisoner = prisonerSearchService.getPrisoner(prisonerId)
    validatePrisonerLocationAgainstStaffCaseload(prisoner.prisonId!!, staffPrisonId)

    val visitOrderBalance = visitAllocationApiClient.getPrisonerVOBalance(prisonerId)

    return PrisonerBalanceDto(
      prisonerId = prisoner.prisonerNumber,
      voBalance = visitOrderBalance.voBalance,
      pvoBalance = visitOrderBalance.pvoBalance,
      firstName = prisoner.firstName,
      lastName = prisoner.lastName,
    )
  }

  fun adjustPrisonerVisitOrderBalance(prisonerId: String, staffPrisonId: String, prisonerBalanceAdjustmentDto: PrisonerBalanceAdjustmentDto) {
    logger.info("Entered VisitAllocationService - adjustPrisonerVisitOrderBalance, adjust prisonerId $prisonerId's balance with dto $prisonerBalanceAdjustmentDto")
    val prisoner = prisonerSearchService.getPrisoner(prisonerId)
    validatePrisonerLocationAgainstStaffCaseload(prisoner.prisonId!!, staffPrisonId)

    visitAllocationApiClient.adjustPrisonersVisitOrderBalanceAsMono(prisonerId, prisonerBalanceAdjustmentDto)
  }

  fun getVisitOrderHistoryDetails(prisonerId: String, fromDate: LocalDate, maxResults: Int?): VisitOrderHistoryDetailsDto? {
    logger.trace("Getting visit order history details for prisoner {}, starting from date {}", prisonerId, fromDate)
    return visitAllocationApiClient.getVisitOrderHistoryDetails(prisonerId, fromDate)?.also { visitOrderHistoryDetailsDto ->
      var filteredVisitOrderHistoryList = visitOrderHistoryDetailsDto.visitOrderHistory.also { visitOrderHistoryList ->
        // set balance change
        setVisitOrderHistoryBalanceChange(visitOrderHistoryList)
      }.filterNot { visitOrderHistoryDto ->
        // remove any entries that do not have a balance change
        visitOrderHistoryWithoutBalanceChange.test(visitOrderHistoryDto)
      }.sortedByDescending {
        it.createdTimeStamp
      }

      filteredVisitOrderHistoryList = getVisitOrderHistoryMaxElements(filteredVisitOrderHistoryList, maxResults).also { visitOrderHistoryList ->
        // set username and incentive levels
        setVisitOrderHistoryUserNames(visitOrderHistoryList)
        setIncentiveLevels(visitOrderHistoryList)
      }.also { visitOrderHistoryList ->
        // finally set balance change to zero if null
        setBalanceChangeToZeroIfNull(visitOrderHistoryList)
      }

      visitOrderHistoryDetailsDto.visitOrderHistory = filteredVisitOrderHistoryList
    }
  }

  private fun setVisitOrderHistoryUserNames(visitOrderHistoryList: List<VisitOrderHistoryDto>) {
    if (visitOrderHistoryList.isNotEmpty()) {
      val userNames = visitOrderHistoryList.map { it.userName }.toSet()
      val userFullNames = getUserFullNames(userNames)

      visitOrderHistoryList.forEach { visitOrderHistory ->
        setUsername(visitOrderHistory, userFullNames = userFullNames)
      }
    }
  }

  private fun setVisitOrderHistoryBalanceChange(visitOrderHistoryList: List<VisitOrderHistoryDto>) {
    var previousVisitOrderHistory: VisitOrderHistoryDto? = null

    visitOrderHistoryList.forEach { visitOrderHistory ->
      setBalanceChange(visitOrderHistory, previousVisitOrderHistory)
      previousVisitOrderHistory = visitOrderHistory
    }
  }

  private fun setBalanceChange(visitOrderHistory: VisitOrderHistoryDto, previousVisitOrderHistory: VisitOrderHistoryDto?) {
    visitOrderHistory.voBalanceChange = if (previousVisitOrderHistory == null) null else (visitOrderHistory.voBalance - previousVisitOrderHistory.voBalance)
    visitOrderHistory.pvoBalanceChange = if (previousVisitOrderHistory == null) null else (visitOrderHistory.pvoBalance - previousVisitOrderHistory.pvoBalance)
  }

  private fun setUsername(visitOrderHistory: VisitOrderHistoryDto, userFullNames: Map<String, String>) {
    when (visitOrderHistory.userName) {
      SYSTEM_USER_NAME -> { /*do nothing*/ }
      else -> {
        visitOrderHistory.userName = userFullNames.getOrDefault(visitOrderHistory.userName, visitOrderHistory.userName)
      }
    }
  }

  private val visitOrderHistoryWithoutBalanceChange: Predicate<VisitOrderHistoryDto> = Predicate { visitOrderHistoryDto ->
    (visitOrderHistoryDto.voBalanceChange == 0 && visitOrderHistoryDto.pvoBalanceChange == 0)
  }

  private fun getUserFullNames(userNames: Set<String>): Map<String, String> {
    // TODO - see if manage users can return multiple user names in a single API call, if they do replace this with the new API call as this will slow down the process
    val userNameMap = mutableMapOf<String, String>()
    userNames.forEach { userName ->
      when (userName) {
        SYSTEM_USER_NAME -> userNameMap[userName] = userName
        else -> {
          val userFullName = manageUsersService.getUserFullName(userName, userNameIfNotAvailable = userName)
          userNameMap[userName] = userFullName
        }
      }
    }

    return userNameMap
  }

  private fun setIncentiveLevels(visitOrderHistoryList: List<VisitOrderHistoryDto>) {
    if (visitOrderHistoryList.isNotEmpty()) {
      val visitOrderHistoryWithIncentiveLevel = visitOrderHistoryList.filter { hasAttributesWithIncentiveLevel.test(it) }
      if (visitOrderHistoryWithIncentiveLevel.isNotEmpty()) {
        // get all incentive levels only if there are entries with INCENTIVE LEVEL
        val incentiveLevels = getAllIncentiveLevelsOrEmptyListIfNull()
        if (incentiveLevels.isNotEmpty()) {
          visitOrderHistoryWithIncentiveLevel.forEach { visitOrderHistory ->
            setIncentiveLevel(visitOrderHistory, incentiveLevels)
          }
        }
      }
    }
  }

  private fun setIncentiveLevel(visitOrderHistory: VisitOrderHistoryDto, incentiveLevels: List<IncentiveLevelDto>) {
    visitOrderHistory.attributes.filter { attribute -> hasIncentiveLevel.test(attribute) }.forEach { attr ->
      incentiveLevels.firstOrNull { it.code == attr.attributeValue }?.let { incentiveLevel ->
        attr.attributeValue = incentiveLevel.name
      }
    }
  }

  private fun setBalanceChangeToZeroIfNull(visitOrderHistoryList: List<VisitOrderHistoryDto>) {
    visitOrderHistoryList.filter { it.voBalanceChange == null || it.pvoBalanceChange == null }.forEach { visitOrderHistoryDto ->
      if (visitOrderHistoryDto.voBalanceChange == null) {
        visitOrderHistoryDto.voBalanceChange = 0
      }
      if (visitOrderHistoryDto.pvoBalanceChange == null) {
        visitOrderHistoryDto.pvoBalanceChange = 0
      }
    }
  }

  private fun getVisitOrderHistoryMaxElements(visitOrderHistoryList: List<VisitOrderHistoryDto>, maxResults: Int?): List<VisitOrderHistoryDto> = if (maxResults != null && maxResults > 0) {
    visitOrderHistoryList.take(maxResults)
  } else {
    visitOrderHistoryList
  }

  private fun getAllIncentiveLevelsOrEmptyListIfNull(): List<IncentiveLevelDto> = try {
    incentivesApiClient.getAllIncentiveLevels() ?: emptyList()
  } catch (e: Exception) {
    logger.info("Error getting all incentive levels, returning empty list", e)
    emptyList()
  }

  private fun validatePrisonerLocationAgainstStaffCaseload(prisonerPrisonId: String, staffPrisonId: String) {
    if (prisonerPrisonId != staffPrisonId) {
      throw ValidationException("Prisoner's prison ID - $prisonerPrisonId does not match staff prisonId caseload - $staffPrisonId. Prisoner visit order balance cannot be retrieved for a prisoner from a different prison.")
    }
  }
}
