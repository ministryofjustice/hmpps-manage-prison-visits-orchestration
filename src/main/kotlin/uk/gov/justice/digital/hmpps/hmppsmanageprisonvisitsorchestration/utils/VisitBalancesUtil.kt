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

    return if (latestIepAdjustDate.isBefore(latestPrivilegedIepAdjustDate)) {
      latestIepAdjustDate
    } else {
      latestPrivilegedIepAdjustDate
    }
  }

  private fun calculateIepAdjustDate(visitBalance: VisitBalancesDto?, dateFrom: LocalDate): LocalDate {
    // IEP Adjust date is latestIepAdjustDate or if latestIepAdjustDate not available then calculated as TODAY + 14 days
    return visitBalance?.latestIepAdjustDate ?: dateFrom.plusDays(14)
  }

  private fun calculatePrivilegedIepAdjustDate(visitBalance: VisitBalancesDto?, dateFrom: LocalDate): LocalDate {
    // Privileged IEP Adjust date is latestPrivIepAdjustDate or if latestPrivIepAdjustDate not available then calculated as 1st day of next month
    return visitBalance?.latestPrivIepAdjustDate ?: dateFrom.plusMonths(1).withDayOfMonth(1)
  }
}
