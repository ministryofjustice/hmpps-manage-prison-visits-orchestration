package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.utils

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.VisitBalancesDto
import java.time.LocalDate

@Component
class VisitBalancesUtil(private val dateUtil: DateUtils) {
  fun calculateAvailableVos(visitBalance: VisitBalancesDto?): Int = (visitBalance?.remainingVo ?: 0) + (visitBalance?.remainingPvo ?: 0)

  fun calculateVoRenewalDate(visitBalance: VisitBalancesDto?): LocalDate {
    val currentDate = dateUtil.getCurrentDate()
    val latestVORenewalDate = calculateVORenewalDate(visitBalance, currentDate)
    val latestPVORenewalDate = calculatePVORenewalDate(visitBalance, currentDate)

    return minOf(latestVORenewalDate, latestPVORenewalDate)
  }

  private fun calculateVORenewalDate(visitBalance: VisitBalancesDto?, currentDate: LocalDate): LocalDate {
    // IEP Adjust date is latestIepAdjustDate or if latestIepAdjustDate not available or today or in the past then calculated as TODAY + 14 days
    val latestVORenewalDate = getLatestRenewalDate(visitBalance?.latestIepAdjustDate, currentDate)
    return latestVORenewalDate ?: currentDate.plusDays(14)
  }

  private fun calculatePVORenewalDate(visitBalance: VisitBalancesDto?, currentDate: LocalDate): LocalDate {
    // Privileged IEP Adjust date is latestPrivIepAdjustDate or if latestPrivIepAdjustDate not available or today or in the past then calculated as 1st day of next month
    val latestPVORenewalDate = getLatestRenewalDate(visitBalance?.latestPrivIepAdjustDate, currentDate)
    return latestPVORenewalDate ?: currentDate.plusMonths(1).withDayOfMonth(1)
  }

  private fun getLatestRenewalDate(latestRenewalDate: LocalDate?, currentDate: LocalDate): LocalDate? {
    // ignore any dates that are before or equal to current date - dateFrom will always be current date
    return latestRenewalDate?.let {
      if (latestRenewalDate <= currentDate) null else latestRenewalDate
    }
  }
}
