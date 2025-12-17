package uk.gov.justice.digital.hmpps.prison.visits.orchestration.utils

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.visit.allocation.PrisonerVOBalanceDto
import java.time.LocalDate

@Component
class VisitBalancesUtil(private val currentDateUtil: CurrentDateUtils) {
  fun calculateAvailableVoAndPvoCount(visitBalance: PrisonerVOBalanceDto?): Int {
    // total of available VOs and PVOs
    return ((visitBalance?.voBalance ?: 0) + (visitBalance?.pvoBalance ?: 0))
  }

  fun calculateRenewalDate(voBalanceDto: PrisonerVOBalanceDto?): LocalDate {
    val currentDate = currentDateUtil.getCurrentDate()

    return voBalanceDto?.nextVoAllocationDate ?: currentDate.plusDays(14)
  }
}
