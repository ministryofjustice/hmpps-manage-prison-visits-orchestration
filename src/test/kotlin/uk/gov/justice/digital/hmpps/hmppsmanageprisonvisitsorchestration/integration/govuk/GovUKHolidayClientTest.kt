package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.govuk

import com.github.tomakehurst.wiremock.client.WireMock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager
import org.springframework.http.HttpHeaders
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.GovUKHolidayClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.GovUKHolidayClient.Companion.HOLIDAYS_JSON
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config.CacheNames
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.govuk.holidays.HolidayEventByDivisionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.govuk.holidays.HolidaysDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase

class GovUKHolidayClientTest : IntegrationTestBase() {
  @Autowired
  lateinit var govUKHolidayClient: GovUKHolidayClient

  @Autowired
  lateinit var cacheManager: CacheManager

  @BeforeEach
  fun clearBankHolidaysCache() {
    cacheManager.getCache(CacheNames.BANK_HOLIDAYS)?.clear()
  }

  @Test
  fun `gov uk web client is unauthenticated`() {
    val holidaysDto = HolidaysDto(HolidayEventByDivisionDto(division = "england-and-wales", emptyList()))
    govUkMockServer.stubGetBankHolidays(holidaysDto)

    val response = govUKHolidayClient.getHolidays()

    assertThat(response).isEqualTo(holidaysDto)
    govUkMockServer.verify(
      WireMock.getRequestedFor(WireMock.urlEqualTo(HOLIDAYS_JSON))
        .withHeader(HttpHeaders.AUTHORIZATION, WireMock.absent()),
    )
  }
}
