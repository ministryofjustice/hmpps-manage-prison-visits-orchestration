package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.utils

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation.PrisonerVOBalanceDetailedDto
import java.time.LocalDate

class VisitBalancesUtilTest {
  private val currentDateUtil: CurrentDateUtils = mock()

  private fun createPrisonerVOBalanceDto(
    prisonerId: String,
    availableVos: Int,
    accumulatedVos: Int,
    negativeVos: Int,
    availablePvos: Int,
    negativePvos: Int,
    lastVoAllocatedDate: LocalDate,
    lastPvoAllocatedDate: LocalDate?,
  ) = PrisonerVOBalanceDetailedDto(
    prisonerId = prisonerId,
    voBalance = (availableVos + accumulatedVos) - negativeVos,
    availableVos = availableVos,
    accumulatedVos = accumulatedVos,
    negativeVos = negativeVos,
    pvoBalance = availablePvos - negativePvos,
    availablePvos = availablePvos,
    negativePvos = negativePvos,
    lastVoAllocatedDate = lastVoAllocatedDate,
    nextVoAllocationDate = lastVoAllocatedDate.plusDays(14),
    lastPvoAllocatedDate = lastPvoAllocatedDate,
    nextPvoAllocationDate = lastPvoAllocatedDate?.plusDays(28),
  )

  @Test
  fun `test available VOs is a total of VO and PVO`() {
    // Given
    val visitBalance = createPrisonerVOBalanceDto(prisonerId = "test", availableVos = 3, accumulatedVos = 5, negativeVos = 0, availablePvos = 1, negativePvos = 2, lastVoAllocatedDate = LocalDate.now(), lastPvoAllocatedDate = null)

    // When
    whenever(currentDateUtil.getCurrentDate()).thenReturn(LocalDate.now())
    val availableVos = VisitBalancesUtil(currentDateUtil).calculateAvailableVoAndPvoCount(visitBalance)

    // Then
    Assertions.assertThat(availableVos).isEqualTo(7)
  }

  @Test
  fun `test VO Renewal date is next VO allocation date when VO date is earlier of the 2 dates`() {
    // Given
    val lastVOAllocationDate = LocalDate.now().minusDays(3)
    val lastPVOAllocationDate = LocalDate.now().minusDays(7)
    val visitBalance = createPrisonerVOBalanceDto(prisonerId = "test", availableVos = 3, accumulatedVos = 4, negativeVos = 0, availablePvos = 1, negativePvos = 2, lastVoAllocatedDate = lastVOAllocationDate, lastPvoAllocatedDate = lastPVOAllocationDate)

    // When
    whenever(currentDateUtil.getCurrentDate()).thenReturn(LocalDate.now())
    val renewalDate = VisitBalancesUtil(currentDateUtil).calculateRenewalDate(visitBalance)

    // Then
    Assertions.assertThat(renewalDate).isEqualTo(lastVOAllocationDate.plusDays(14))
  }

  @Test
  fun `test VO Renewal date is next VO allocation date when VO date is later of the 2 dates`() {
    // Given
    val lastVOAllocationDate = LocalDate.now().minusDays(13)
    val lastPVOAllocationDate = LocalDate.now().minusDays(28)
    val visitBalance = createPrisonerVOBalanceDto(prisonerId = "test", availableVos = 3, accumulatedVos = 4, negativeVos = 0, availablePvos = 1, negativePvos = 2, lastVoAllocatedDate = lastVOAllocationDate, lastPvoAllocatedDate = lastPVOAllocationDate)

    // When
    whenever(currentDateUtil.getCurrentDate()).thenReturn(LocalDate.now())
    val renewalDate = VisitBalancesUtil(currentDateUtil).calculateRenewalDate(visitBalance)

    // Then
    Assertions.assertThat(renewalDate).isEqualTo(lastVOAllocationDate.plusDays(14))
  }

  @Test
  fun `test VO Renewal date is next voAllocationDate when pvoAllocationDate and voAllocationDate are the same date`() {
    // Given
    val lastVoAllocatedDate = LocalDate.now()
    val lastPvoAllocatedDate = LocalDate.now()
    val visitBalance = createPrisonerVOBalanceDto(prisonerId = "test", availableVos = 3, accumulatedVos = 4, negativeVos = 0, availablePvos = 1, negativePvos = 2, lastVoAllocatedDate = lastVoAllocatedDate, lastPvoAllocatedDate = lastPvoAllocatedDate)

    // When
    whenever(currentDateUtil.getCurrentDate()).thenReturn(LocalDate.now())
    val renewalDate = VisitBalancesUtil(currentDateUtil).calculateRenewalDate(visitBalance)

    // Then
    Assertions.assertThat(renewalDate).isEqualTo(lastVoAllocatedDate.plusDays(14))
  }

  @Test
  fun `test VO Renewal date is next voAllocationDate when pvoAllocationDate is null`() {
    // Given
    val lastVoAllocatedDate = LocalDate.now()
    val lastPvoAllocatedDate = null
    val visitBalance = createPrisonerVOBalanceDto(prisonerId = "test", availableVos = 3, accumulatedVos = 4, negativeVos = 0, availablePvos = 1, negativePvos = 2, lastVoAllocatedDate = lastVoAllocatedDate, lastPvoAllocatedDate = lastPvoAllocatedDate)

    // When
    whenever(currentDateUtil.getCurrentDate()).thenReturn(LocalDate.now())
    val renewalDate = VisitBalancesUtil(currentDateUtil).calculateRenewalDate(visitBalance)

    // Then
    Assertions.assertThat(renewalDate).isEqualTo(lastVoAllocatedDate.plusDays(14))
  }

  @Test
  fun `test VO Renewal date is currentDate + 14 when visitBalance is null`() {
    // Given
    val visitBalance = null

    // When
    whenever(currentDateUtil.getCurrentDate()).thenReturn(LocalDate.now())
    val renewalDate = VisitBalancesUtil(currentDateUtil).calculateRenewalDate(visitBalance)

    // Then
    Assertions.assertThat(renewalDate).isEqualTo(LocalDate.now().plusDays(14))
  }
}
