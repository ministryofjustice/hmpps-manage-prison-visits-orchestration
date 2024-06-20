package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.utils

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.VisitBalancesDto
import java.time.LocalDate

@Component
class VisitBalancesUtil {
  fun calculateAvailableVos(visitBalance: VisitBalancesDto?): Int {
    return (visitBalance?.remainingVo ?: 0) + (visitBalance?.remainingPvo ?: 0)
  }

  fun calculateVoRenewalDate(visitBalance: VisitBalancesDto?, dateFrom: LocalDate = LocalDate.now()): LocalDate {
    val latestIepAdjustDate = calculateIepAdjustDate(visitBalance, dateFrom)
    val latestPrivilegedIepAdjustDate = calculatePrivilegedIepAdjustDate(visitBalance, dateFrom)

    return minOf(latestIepAdjustDate, latestPrivilegedIepAdjustDate)
  }

  private fun calculateIepAdjustDate(visitBalance: VisitBalancesDto?, dateFrom: LocalDate): LocalDate {
    // IEP Adjust date is latestIepAdjustDate or if latestIepAdjustDate not available or today or in the past then calculated as TODAY + 14 days
    val latestIepAdjustDate = getLatestAdjustDate(visitBalance?.latestIepAdjustDate, dateFrom)
    return latestIepAdjustDate ?: dateFrom.plusDays(14)
  }

  private fun calculatePrivilegedIepAdjustDate(visitBalance: VisitBalancesDto?, dateFrom: LocalDate): LocalDate {
    // Privileged IEP Adjust date is latestPrivIepAdjustDate or if latestPrivIepAdjustDate not available or today or in the past then calculated as 1st day of next month
    getLatestAdjustDate(visitBalance?.latestIepAdjustDate, dateFrom)
    val latestPrivIepAdjustDate = getLatestAdjustDate(visitBalance?.latestPrivIepAdjustDate, dateFrom)
    return latestPrivIepAdjustDate ?: dateFrom.plusMonths(1).withDayOfMonth(1)
  }

  private fun getLatestAdjustDate(latestAdjustDate: LocalDate?, dateFrom: LocalDate): LocalDate? {
    // ignore any dates that are before or equal to current date - dateFrom will always be current date
    return latestAdjustDate?.let {
      if (latestAdjustDate <= dateFrom) null else latestAdjustDate
    }
  }
}
