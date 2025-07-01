package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.GovUKHolidayClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.govuk.holidays.HolidayEventDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.govuk.holidays.HolidaysDto
import java.util.function.Predicate

@Service
class GovUkHolidayService(
  private val govUKHolidayClient: GovUKHolidayClient,
) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)

    val futureDatedHolidays: Predicate<HolidayEventDto> =
      Predicate {
        (it.date.isEqual(java.time.LocalDate.now()) || it.date.isAfter(java.time.LocalDate.now()))
      }
  }

  fun getGovUKBankHolidays(futureOnly: Boolean = false): List<HolidayEventDto> {
    LOG.debug("Entered getGovUKBankHolidays, futureOnly:$futureOnly")
    var holidaysDto: HolidaysDto?
    try {
      holidaysDto = govUKHolidayClient.getHolidays()
    } catch (e: WebClientResponseException) {
      LOG.info("Failed to acquire bank holidays from gov uk api, attempting to acquire from local cache, exception: {}", e.message)

      // TODO - check if this is needed?
      holidaysDto = govUKHolidayClient.getHolidaysFromLocalCache()
    }

    var bankHolidays = holidaysDto?.englandAndWalesHolidays?.events

    if (futureOnly) {
      bankHolidays = bankHolidays?.filter { futureDatedHolidays.test(it) }
    }

    return bankHolidays?.sortedBy { it.date } ?: emptyList()
  }
}
