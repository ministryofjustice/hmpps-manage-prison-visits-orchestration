package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.GovUKHolidayClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.govuk.holidays.HolidayEventDto
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
    var bankHolidays: List<HolidayEventDto>?
    try {
      bankHolidays = govUKHolidayClient.getHolidays()?.englandAndWalesHolidays?.events
    } catch (e: WebClientResponseException) {
      LOG.info("Failed to acquire bank holidays from gov uk api, attempting to acquire from local cache, exception: {}", e.message)

      // if there was an exception return an empty list
      bankHolidays = emptyList()
    }

    if (futureOnly && !bankHolidays.isNullOrEmpty()) {
      bankHolidays = bankHolidays.filter { futureDatedHolidays.test(it) }
    }

    return bankHolidays?.sortedBy { it.date } ?: emptyList()
  }
}
