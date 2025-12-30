package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitAllocationApiClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation.VisitOrderHistoryDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation.VisitOrderHistoryDto
import java.time.LocalDate
import java.util.function.Predicate

@Service
class VisitAllocationService(
  private val visitAllocationApiClient: VisitAllocationApiClient,
  private val manageUsersService: ManageUsersService,
) {
  companion object {
    const val SYSTEM_USER_NAME = "SYSTEM"
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
  }
  private fun setVisitOrderHistoryUserNames(visitOrderHistoryList: List<VisitOrderHistoryDto>) {
    val userNames = visitOrderHistoryList.map { it.userName }.toSet()
    val userFullNames = getUserFullNames(userNames)

    visitOrderHistoryList.forEach { visitOrderHistory ->
      setUsername(visitOrderHistory, userFullNames = userFullNames)
    }
  }

  private fun setVisitOrderHistoryBalanceChange(visitOrderHistoryList: List<VisitOrderHistoryDto>) {
    var previousVisitOrderHistory: VisitOrderHistoryDto? = null

    visitOrderHistoryList.forEach { visitOrderHistory ->
      setBalanceChange(visitOrderHistory, previousVisitOrderHistory)
      previousVisitOrderHistory = visitOrderHistory
    }
  }

  fun getVisitOrderHistoryDetails(prisonerId: String, fromDate: LocalDate): VisitOrderHistoryDetailsDto? {
    logger.trace("Getting visit order history details for prisoner {}, starting from date {}", prisonerId, fromDate)
    return visitAllocationApiClient.getVisitOrderHistoryDetails(prisonerId, fromDate)?.also {
      if (it.visitOrderHistoryList.isNotEmpty()) {
        setVisitOrderHistoryUserNames(it.visitOrderHistoryList)
        setVisitOrderHistoryBalanceChange(it.visitOrderHistoryList)
      }
      it.visitOrderHistoryList = it.visitOrderHistoryList.filterNot { visitOrderHistoryWithoutBalanceChange.test(it) }
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

  private val visitOrderHistoryWithoutBalanceChange: Predicate<VisitOrderHistoryDto> = Predicate<VisitOrderHistoryDto> { visitOrderHistoryDto ->
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
}
