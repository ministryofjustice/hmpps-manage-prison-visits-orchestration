package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.utils

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.VisitBalancesDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation.PrisonerVOBalanceDto
import java.time.LocalDate

@Component
class VisitBalancesUtil(private val currentDateUtil: CurrentDateUtils) {
  fun calculateAvailableVoAndPvoCount(visitBalance: PrisonerVOBalanceDto?): Int {
    // total of available VOs and PVOs
    return calculateAvailableVos(visitBalance) + calculateAvailablePvos(visitBalance)
  }

  private fun calculateAvailableVos(voBalancesDto: PrisonerVOBalanceDto?): Int {
    // (available + accumulated) - negative balance
    return (
      (((voBalancesDto?.availableVos) ?: 0) + ((voBalancesDto?.accumulatedVos) ?: 0)) - ((voBalancesDto?.negativeVos) ?: 0)
      )
  }

  private fun calculateAvailablePvos(voBalancesDto: PrisonerVOBalanceDto?): Int {
    // available - negative balance
    return (
      ((voBalancesDto?.availablePvos) ?: 0) - ((voBalancesDto?.negativePvos) ?: 0)
      )
  }

  fun calculateVoRenewalDate(visitBalance: PrisonerVOBalanceDto?): LocalDate {
    val currentDate = currentDateUtil.getCurrentDate()
    val latestVORenewalDate = calculateVORenewalDate(visitBalance, currentDate)
    val latestPVORenewalDate = calculatePVORenewalDate(visitBalance, currentDate)

    return if (latestPVORenewalDate == null) {
      latestVORenewalDate
    } else {
      minOf(latestVORenewalDate, latestPVORenewalDate)
    }
  }

  fun getVisitBalancesDto(prisonerVOBalanceDto: PrisonerVOBalanceDto): VisitBalancesDto = VisitBalancesDto(
    remainingVo = calculateAvailableVos(prisonerVOBalanceDto),
    remainingPvo = calculateAvailablePvos(prisonerVOBalanceDto),
    lastVoAllocationDate = prisonerVOBalanceDto.lastVoAllocatedDate,
    nextVoAllocationDate = calculateVORenewalDate(prisonerVOBalanceDto, currentDateUtil.getCurrentDate()),
    lastPvoAllocationDate = prisonerVOBalanceDto.lastPvoAllocatedDate,
    nextPvoAllocationDate = calculatePVORenewalDate(prisonerVOBalanceDto, currentDateUtil.getCurrentDate()),
  )

  private fun calculateVORenewalDate(visitBalance: PrisonerVOBalanceDto?, currentDate: LocalDate): LocalDate {
    // VO renewal date is last VO allocated date + 14 days
    // if visitBalance is null or lastVoAllocatedDate + 14 falls in the past, returning currentDate + 14
    return if (visitBalance == null || visitBalance.lastVoAllocatedDate.plusDays(14).isBefore(currentDate)) {
      return currentDate.plusDays(14)
    } else {
      visitBalance.lastVoAllocatedDate.plusDays(14)
    }
  }

  private fun calculatePVORenewalDate(visitBalance: PrisonerVOBalanceDto?, currentDate: LocalDate): LocalDate? {
    // PVO renewal date is last VO allocated date + 28 days
    // if visitBalance is null or lastVoAllocatedDate + 28 falls in the past, returning null
    return if (visitBalance == null ||
      visitBalance.lastPvoAllocatedDate == null ||
      visitBalance.lastPvoAllocatedDate.plusDays(28).isBefore(currentDate)
    ) {
      return null
    } else {
      visitBalance.lastPvoAllocatedDate.plusDays(28)
    }
  }
}
