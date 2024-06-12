package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.utils

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.VisitBalancesDto
import java.time.LocalDate

class VisitBalancesUtilTest {

  @Test
  fun `test available VOs is a total of VO and PVO`() {
    // Given
    val visitBalance = VisitBalancesDto(3, 4, null, null)

    // When
    val availableVos = VisitBalancesUtil().calculateAvailableVos(visitBalance)

    // Then
    Assertions.assertThat(availableVos).isEqualTo(7)
  }

  @Test
  fun `test VO Renewal date is latestIepAdjustDate when earlier of the 2 dates`() {
    // Given
    val latestIepAdjustDate = LocalDate.now().plusDays(3)
    val latestPrivIepAdjustDate = LocalDate.now().plusDays(7)
    val visitBalance = VisitBalancesDto(3, 4, latestIepAdjustDate, latestPrivIepAdjustDate)
    val renewalDate = VisitBalancesUtil().calculateVoRenewalDate(visitBalance)

    // Then
    Assertions.assertThat(renewalDate).isEqualTo(latestIepAdjustDate)
  }

  @Test
  fun `test VO Renewal date is latestPrivIepAdjustDate when earlier of the 2 dates`() {
    // Given
    val latestIepAdjustDate = LocalDate.now().plusDays(14)
    val latestPrivIepAdjustDate = LocalDate.now().plusDays(8)
    val visitBalance = VisitBalancesDto(3, 4, latestIepAdjustDate, latestPrivIepAdjustDate)
    val renewalDate = VisitBalancesUtil().calculateVoRenewalDate(visitBalance)

    // Then
    Assertions.assertThat(renewalDate).isEqualTo(latestPrivIepAdjustDate)
  }

  @Test
  fun `test VO Renewal date is passed date when both are same`() {
    // Given
    val date = LocalDate.now().plusDays(14)
    val visitBalance = VisitBalancesDto(3, 4, date, date)
    val renewalDate = VisitBalancesUtil().calculateVoRenewalDate(visitBalance)

    // Then
    Assertions.assertThat(renewalDate).isEqualTo(date)
  }

  @Test
  fun `test VO Renewal date is 1st of next month when IEP adjust date is after that`() {
    // Given
    val latestIepAdjustDate = LocalDate.now().plusMonths(1).withDayOfMonth(2)
    val latestPrivIepAdjustDate = null
    val visitBalance = VisitBalancesDto(3, 4, latestIepAdjustDate, latestPrivIepAdjustDate)
    val renewalDate = VisitBalancesUtil().calculateVoRenewalDate(visitBalance)
    // Then
    Assertions.assertThat(renewalDate).isEqualTo(LocalDate.now().plusMonths(1).withDayOfMonth(1))
  }

  @Test
  fun `test VO Renewal date is today + 14 days if privileged IEP adjust date falls after that`() {
    // Given
    val latestIepAdjustDate = null
    val latestPrivIepAdjustDate = LocalDate.now().plusDays(15)
    val visitBalance = VisitBalancesDto(3, 4, latestIepAdjustDate, latestPrivIepAdjustDate)
    val renewalDate = VisitBalancesUtil().calculateVoRenewalDate(visitBalance)

    // Then
    Assertions.assertThat(renewalDate).isEqualTo(LocalDate.now().plusDays(14))
  }

  @Test
  fun `test VO Renewal date is first day of month after when both are null and current date is less than 14 days away from end of month`() {
    // Given
    val visitBalance = VisitBalancesDto(3, 4, null, null)

    // calculating from 21st June 2024 - renewal date will be 01st Jul 2024
    val dateFrom = LocalDate.of(2024, 6, 21)

    val expectedRenewalDate = LocalDate.of(2024, 7, 1)
    val renewalDate = VisitBalancesUtil().calculateVoRenewalDate(visitBalance, dateFrom)

    // Then
    Assertions.assertThat(renewalDate).isEqualTo(expectedRenewalDate)
  }

  @Test
  fun `test VO Renewal date is dateFrom add 14 days when both are null and current date is more than 14 days away from end of month`() {
    // Given
    val visitBalance = VisitBalancesDto(3, 4, null, null)

    // calculating from 16th June 2024 - renewal date will be 30th Jun 2024
    val dateFrom = LocalDate.of(2024, 6, 16)

    val expectedRenewalDate = LocalDate.of(2024, 6, 30)
    val renewalDate = VisitBalancesUtil().calculateVoRenewalDate(visitBalance, dateFrom)

    // Then
    Assertions.assertThat(renewalDate).isEqualTo(expectedRenewalDate)
  }

  @Test
  fun `test VO Renewal date is dateFrom add 14 days when both are null and current date is 13 days away from end of month`() {
    // Given
    val visitBalance = VisitBalancesDto(3, 4, null, null)

    // calculating from 17th June 2024 - renewal date will be 01st Jul 2024 as both are on the 1st
    val dateFrom = LocalDate.of(2024, 6, 17)

    val expectedRenewalDate = LocalDate.of(2024, 7, 1)
    val renewalDate = VisitBalancesUtil().calculateVoRenewalDate(visitBalance, dateFrom)

    // Then
    Assertions.assertThat(renewalDate).isEqualTo(expectedRenewalDate)
  }
}
