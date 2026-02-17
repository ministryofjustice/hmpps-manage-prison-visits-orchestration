package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.prisons.ExcludeDateDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.prisons.IsExcludeDateDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.utils.CurrentDateUtils
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
      val userNames = excludeDates.map { it.actionedBy }.toSet()
      val userNameMap = manageUsersService.getFullNamesForUserIds(userNames)

      for (excludeDate in excludeDates) {
        excludeDate.actionedBy = userNameMap[excludeDate.actionedBy] ?: excludeDate.actionedBy
      }
    }

    return excludeDates
  }
}
