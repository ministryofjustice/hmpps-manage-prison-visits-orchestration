package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.utils

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation.PrisonerVOBalanceDto
import java.time.LocalDate

@Component
class VisitBalancesUtil(private val currentDateUtil: CurrentDateUtils) {
  fun calculateAvailableVoAndPvoCount(visitBalance: PrisonerVOBalanceDto?): Int {
    // total of available VOs and PVOs
    return ((visitBalance?.voBalance ?: 0) + (visitBalance?.pvoBalance ?: 0))
  }

  fun calculateRenewalDate(visitBalance: PrisonerVOBalanceDto?): LocalDate {
    val currentDate = currentDateUtil.getCurrentDate()

    if (visitBalance == null) {
      return currentDate.plusDays(14)
    }
    val nextVORenewalDate = visitBalance.nextVoAllocationDate
    val nextPVORenewalDate = visitBalance.nextPvoAllocationDate

    return if (nextPVORenewalDate == null) {
      nextVORenewalDate
    } else {
      minOf(nextVORenewalDate, nextPVORenewalDate)
    }
  }
}
