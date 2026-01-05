package uk.gov.justice.digital.hmpps.prison.visits.orchestration.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.visit.scheduler.prisons.ExcludeDateDto
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.visit.scheduler.prisons.IsExcludeDateDto
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.utils.CurrentDateUtils
import java.time.LocalDate
import java.util.function.Predicate

@Service
class ExcludeDatesService(
  private val manageUsersService: ManageUsersService,
  private val currentDateUtils: CurrentDateUtils,
) {

  fun getFutureExcludeDates(excludeDates: List<ExcludeDateDto>): List<ExcludeDateDto> {
    val futureDatesPredicate: Predicate<ExcludeDateDto> = Predicate { it.excludeDate >= currentDateUtils.getCurrentDate() }
    return getExcludeDates(excludeDates, futureDatesPredicate).sortedBy { it.excludeDate }
  }

  fun getPastExcludeDates(excludeDates: List<ExcludeDateDto>): List<ExcludeDateDto> {
    val pastDatesPredicate: Predicate<ExcludeDateDto> = Predicate { it.excludeDate < currentDateUtils.getCurrentDate() }
    return getExcludeDates(excludeDates, pastDatesPredicate).sortedByDescending { it.excludeDate }
  }

  fun isDateExcluded(excludeDates: List<ExcludeDateDto>, date: LocalDate): IsExcludeDateDto {
    val isExcluded = excludeDates.map { it.excludeDate }.contains(date)
    return IsExcludeDateDto(isExcluded)
  }

  private fun getExcludeDates(excludeDates: List<ExcludeDateDto>, excludeDatesFilter: Predicate<ExcludeDateDto>): List<ExcludeDateDto> = excludeDates.filter { excludeDatesFilter.test(it) }.also {
    setActionedByFullName(it)
  }

  private fun setActionedByFullName(excludeDates: List<ExcludeDateDto>): List<ExcludeDateDto> {
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
