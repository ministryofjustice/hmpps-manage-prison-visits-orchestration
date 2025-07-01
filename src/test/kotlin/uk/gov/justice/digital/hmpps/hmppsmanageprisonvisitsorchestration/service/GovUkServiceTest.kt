package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.GovUKHolidayClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.govuk.holidays.HolidayEventByDivisionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.govuk.holidays.HolidayEventDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.govuk.holidays.HolidaysDto
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class GovUkServiceTest {
  private val govUKHolidayClient = mock<GovUKHolidayClient>()
  private val govUkHolidayService = GovUkHolidayService(govUKHolidayClient)

  @Test
  fun `when future holidays are being retrieved with future flag as true only current and future dated ones are returned`() {
    val events = listOf(
      HolidayEventDto("tomorrow-is-a-holiday", LocalDate.now().plusDays(1)),
      HolidayEventDto("yesterday-was-a-holiday", LocalDate.now().minusDays(1)),
      HolidayEventDto("today-is-a-holiday", LocalDate.now()),
    )
    val holidaysDto = HolidaysDto(
      englandAndWalesHolidays = HolidayEventByDivisionDto("england-and-wales", events),
    )

    whenever(
      govUKHolidayClient.getHolidays(),
    ).thenReturn(holidaysDto)

    // When
    val futureHolidays = govUkHolidayService.getGovUKBankHolidays(futureOnly = true)

    // Then
    assertThat(futureHolidays.size).isEqualTo(2)
    assertThat(futureHolidays[0].date).isEqualTo(LocalDate.now())
    assertThat(futureHolidays[1].date).isEqualTo(LocalDate.now().plusDays(1))
  }

  @Test
  fun `when future holidays are being retrieved with future flag as false all results are returned`() {
    val events = listOf(
      HolidayEventDto("tomorrow-is-a-holiday", LocalDate.now().plusDays(1)),
      HolidayEventDto("yesterday-was-a-holiday", LocalDate.now().minusDays(1)),
      HolidayEventDto("today-is-a-holiday", LocalDate.now()),
    )
    val holidaysDto = HolidaysDto(
      englandAndWalesHolidays = HolidayEventByDivisionDto("england-and-wales", events),
    )

    whenever(
      govUKHolidayClient.getHolidays(),
    ).thenReturn(holidaysDto)

    // When
    val futureHolidays = govUkHolidayService.getGovUKBankHolidays(futureOnly = false)

    // Then
    assertThat(futureHolidays.size).isEqualTo(3)
    assertThat(futureHolidays[0].date).isEqualTo(LocalDate.now().minusDays(1))
    assertThat(futureHolidays[1].date).isEqualTo(LocalDate.now())
    assertThat(futureHolidays[2].date).isEqualTo(LocalDate.now().plusDays(1))
  }

  @Test
  fun `when future holidays are being retrieved and a NOT_FOUND error occurs then an empty list is returned`() {
    whenever(
      govUKHolidayClient.getHolidays(),
    ).thenThrow(
      WebClientResponseException.create(HttpStatus.NOT_FOUND.value(), "", HttpHeaders.EMPTY, byteArrayOf(), null),
    )

    // When
    val futureHolidays = govUkHolidayService.getGovUKBankHolidays(futureOnly = true)

    // Then
    assertThat(futureHolidays).isEmpty()
  }

  @Test
  fun `when all holidays are being retrieved and an INTERNAL_SERVER_ERROR error occurs then an empty list is returned`() {
    whenever(
      govUKHolidayClient.getHolidays(),
    ).thenThrow(
      WebClientResponseException.create(HttpStatus.INTERNAL_SERVER_ERROR.value(), "", HttpHeaders.EMPTY, byteArrayOf(), null),
    )

    // When
    val futureHolidays = govUkHolidayService.getGovUKBankHolidays()

    // Then
    assertThat(futureHolidays).isEmpty()
  }
}
