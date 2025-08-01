package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.utils

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.VisitBalancesDto
import java.time.LocalDate

class VisitBalancesUtilTest {
  private val currentDateUtil: CurrentDateUtils = mock()

  @Test
  fun `test available VOs is a total of VO and PVO`() {
    // Given
    val visitBalance = VisitBalancesDto(remainingVo = 3, remainingPvo = 4, latestIepAdjustDate = null, latestPrivIepAdjustDate = null)

    // When
    whenever(currentDateUtil.getCurrentDate()).thenReturn(LocalDate.now())
    val availableVos = VisitBalancesUtil(currentDateUtil).calculateAvailableVos(visitBalance)

    // Then
    Assertions.assertThat(availableVos).isEqualTo(7)
  }

  @Test
  fun `test VO Renewal date is latestIepAdjustDate when earlier of the 2 dates`() {
    // Given
    val latestIepAdjustDate = LocalDate.now().plusDays(3)
    val latestPrivIepAdjustDate = LocalDate.now().plusDays(7)
    val visitBalance = VisitBalancesDto(remainingVo = 3, remainingPvo = 4, latestIepAdjustDate = latestIepAdjustDate, latestPrivIepAdjustDate = latestPrivIepAdjustDate)

    // When
    whenever(currentDateUtil.getCurrentDate()).thenReturn(LocalDate.now())
    val renewalDate = VisitBalancesUtil(currentDateUtil).calculateVoRenewalDate(visitBalance)

    // Then
    Assertions.assertThat(renewalDate).isEqualTo(latestIepAdjustDate)
  }

  @Test
  fun `test VO Renewal date is latestPrivIepAdjustDate when earlier of the 2 dates`() {
    // Given
    val latestIepAdjustDate = LocalDate.now().plusDays(14)
    val latestPrivIepAdjustDate = LocalDate.now().plusDays(8)
    val visitBalance = VisitBalancesDto(remainingVo = 3, remainingPvo = 4, latestIepAdjustDate = latestIepAdjustDate, latestPrivIepAdjustDate = latestPrivIepAdjustDate)

    // When
    whenever(currentDateUtil.getCurrentDate()).thenReturn(LocalDate.now())
    val renewalDate = VisitBalancesUtil(currentDateUtil).calculateVoRenewalDate(visitBalance)

    // Then
    Assertions.assertThat(renewalDate).isEqualTo(latestPrivIepAdjustDate)
  }

  @Test
  fun `test VO Renewal date is passed date when both are same`() {
    // Given
    val date = LocalDate.now().plusDays(14)
    val visitBalance = VisitBalancesDto(remainingVo = 3, remainingPvo = 4, latestIepAdjustDate = date, latestPrivIepAdjustDate = date)

    // When
    whenever(currentDateUtil.getCurrentDate()).thenReturn(LocalDate.now())
    val renewalDate = VisitBalancesUtil(currentDateUtil).calculateVoRenewalDate(visitBalance)

    // Then
    Assertions.assertThat(renewalDate).isEqualTo(date)
  }

  @Test
  fun `test VO Renewal date is 1st of next month when IEP adjust date is after that`() {
    // Given
    val latestIepAdjustDate = LocalDate.now().plusMonths(1).withDayOfMonth(2)
    val latestPrivIepAdjustDate = null
    val visitBalance = VisitBalancesDto(remainingVo = 3, remainingPvo = 4, latestIepAdjustDate = latestIepAdjustDate, latestPrivIepAdjustDate = latestPrivIepAdjustDate)

    // When
    whenever(currentDateUtil.getCurrentDate()).thenReturn(LocalDate.now())
    val renewalDate = VisitBalancesUtil(currentDateUtil).calculateVoRenewalDate(visitBalance)

    // Then
    Assertions.assertThat(renewalDate).isEqualTo(LocalDate.now().plusMonths(1).withDayOfMonth(1))
  }

  @Test
  fun `test VO Renewal date is today + 14 days if privileged IEP adjust date falls after that`() {
    // Given
    val latestIepAdjustDate = null
    val latestPrivIepAdjustDate = LocalDate.now().plusDays(15)
    val visitBalance = VisitBalancesDto(remainingVo = 3, remainingPvo = 4, latestIepAdjustDate = latestIepAdjustDate, latestPrivIepAdjustDate = latestPrivIepAdjustDate)

    // When
    whenever(currentDateUtil.getCurrentDate()).thenReturn(LocalDate.now())
    val renewalDate = VisitBalancesUtil(currentDateUtil).calculateVoRenewalDate(visitBalance)
    // Then
    Assertions.assertThat(renewalDate).isEqualTo(LocalDate.now().plusDays(14))
  }

  @Test
  fun `test VO Renewal date is first day of month after when both are null and current date is less than 14 days away from end of month`() {
    // Given
    val visitBalance = VisitBalancesDto(remainingVo = 3, remainingPvo = 4, latestIepAdjustDate = null, latestPrivIepAdjustDate = null)

    // When
    // calculating from 21st June 2024 - renewal date will be 01st Jul 2024
    val dateFrom = LocalDate.of(2024, 6, 21)
    whenever(currentDateUtil.getCurrentDate()).thenReturn(dateFrom)

    val expectedRenewalDate = LocalDate.of(2024, 7, 1)
    val renewalDate = VisitBalancesUtil(currentDateUtil).calculateVoRenewalDate(visitBalance)

    // Then
    Assertions.assertThat(renewalDate).isEqualTo(expectedRenewalDate)
  }

  @Test
  fun `test VO Renewal date is dateFrom add 14 days when both are null and current date is more than 14 days away from end of month`() {
    // Given
    val visitBalance = VisitBalancesDto(remainingVo = 3, remainingPvo = 4, latestIepAdjustDate = null, latestPrivIepAdjustDate = null)

    // When
    // calculating from 16th June 2024 - renewal date will be 30th Jun 2024
    val dateFrom = LocalDate.of(2024, 6, 16)
    whenever(currentDateUtil.getCurrentDate()).thenReturn(dateFrom)

    val expectedRenewalDate = LocalDate.of(2024, 6, 30)
    val renewalDate = VisitBalancesUtil(currentDateUtil).calculateVoRenewalDate(visitBalance)

    // Then
    Assertions.assertThat(renewalDate).isEqualTo(expectedRenewalDate)
  }

  @Test
  fun `test VO Renewal date is dateFrom add 14 days when both are null and current date is 13 days away from end of month`() {
    // Given
    val visitBalance = VisitBalancesDto(remainingVo = 3, remainingPvo = 4, latestIepAdjustDate = null, latestPrivIepAdjustDate = null)

    // When
    // calculating from 17th June 2024 - renewal date will be 01st Jul 2024 as both are on the 1st
    val dateFrom = LocalDate.of(2024, 6, 17)
    whenever(currentDateUtil.getCurrentDate()).thenReturn(dateFrom)

    val expectedRenewalDate = LocalDate.of(2024, 7, 1)
    val renewalDate = VisitBalancesUtil(currentDateUtil).calculateVoRenewalDate(visitBalance)

    // Then
    Assertions.assertThat(renewalDate).isEqualTo(expectedRenewalDate)
  }

  @Test
  fun `test VO Renewal date is latestPrivIepAdjustDate when latestIepAdjustDate is in the past`() {
    // Given
    // although latestIepAdjustDate is less than latestPrivIepAdjustDate this date needs to be ignored as it is in the past
    val latestIepAdjustDate = LocalDate.now().minusDays(1)
    val latestPrivIepAdjustDate = LocalDate.now().plusDays(1)
    val visitBalance = VisitBalancesDto(remainingVo = 3, remainingPvo = 4, latestIepAdjustDate = latestIepAdjustDate, latestPrivIepAdjustDate = latestPrivIepAdjustDate)

    // When
    whenever(currentDateUtil.getCurrentDate()).thenReturn(LocalDate.now())
    val renewalDate = VisitBalancesUtil(currentDateUtil).calculateVoRenewalDate(visitBalance)

    // Then
    Assertions.assertThat(renewalDate).isEqualTo(latestPrivIepAdjustDate)
  }

  @Test
  fun `test VO Renewal date is latestIepAdjustDate when latestPrivIepAdjustDate is in the past`() {
    // Given
    val latestIepAdjustDate = LocalDate.now().plusDays(1)
    // although latestPrivIepAdjustDate is less than latestIepAdjustDate this date needs to be ignored as it is in the past
    val latestPrivIepAdjustDate = LocalDate.now().minusDays(1)
    val visitBalance = VisitBalancesDto(remainingVo = 3, remainingPvo = 4, latestIepAdjustDate = latestIepAdjustDate, latestPrivIepAdjustDate = latestPrivIepAdjustDate)

    // When
    whenever(currentDateUtil.getCurrentDate()).thenReturn(LocalDate.now())
    val renewalDate = VisitBalancesUtil(currentDateUtil).calculateVoRenewalDate(visitBalance)

    // Then
    Assertions.assertThat(renewalDate).isEqualTo(latestIepAdjustDate)
  }

  @Test
  fun `test VO Renewal date is latestPrivIepAdjustDate when latestIepAdjustDate is same as today`() {
    // Given
    // although latestIepAdjustDate is less than latestPrivIepAdjustDate this date needs to be ignored as it is same as today
    val latestIepAdjustDate = LocalDate.now()
    val latestPrivIepAdjustDate = LocalDate.now().plusDays(1)
    val visitBalance = VisitBalancesDto(remainingVo = 3, remainingPvo = 4, latestIepAdjustDate = latestIepAdjustDate, latestPrivIepAdjustDate = latestPrivIepAdjustDate)

    // When
    whenever(currentDateUtil.getCurrentDate()).thenReturn(LocalDate.now())
    val renewalDate = VisitBalancesUtil(currentDateUtil).calculateVoRenewalDate(visitBalance)

    // Then
    Assertions.assertThat(renewalDate).isEqualTo(latestPrivIepAdjustDate)
  }

  @Test
  fun `test VO Renewal date is latestIepAdjustDate when latestPrivIepAdjustDate  is same as today`() {
    // Given
    val latestIepAdjustDate = LocalDate.now().plusDays(1)

    // although latestPrivIepAdjustDate is less than latestIepAdjustDate this date needs to be ignored as it is same as today
    val latestPrivIepAdjustDate = LocalDate.now()
    val visitBalance = VisitBalancesDto(remainingVo = 3, remainingPvo = 4, latestIepAdjustDate = latestIepAdjustDate, latestPrivIepAdjustDate = latestPrivIepAdjustDate)

    // When
    whenever(currentDateUtil.getCurrentDate()).thenReturn(LocalDate.now())
    val renewalDate = VisitBalancesUtil(currentDateUtil).calculateVoRenewalDate(visitBalance)

    // Then
    Assertions.assertThat(renewalDate).isEqualTo(latestIepAdjustDate)
  }
}
