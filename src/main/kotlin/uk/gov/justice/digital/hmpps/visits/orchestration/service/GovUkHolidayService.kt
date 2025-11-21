package uk.gov.justice.digital.hmpps.visits.orchestration.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.visits.orchestration.client.GovUKHolidayClient
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.govuk.holidays.HolidayEventDto
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.govuk.holidays.HolidaysDto
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.DateRange
import uk.gov.justice.digital.hmpps.visits.orchestration.utils.CurrentDateUtils
import java.util.function.Predicate

@Service
class GovUkHolidayService(
  private val govUKHolidayClient: GovUKHolidayClient,
  private val currentDateUtils: CurrentDateUtils,
) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  val futureDatedHolidays: Predicate<HolidayEventDto> =
    Predicate {
      val today = currentDateUtils.getCurrentDate()
      (it.date >= today)
    }

  fun getGovUKBankHolidays(futureOnly: Boolean = false): List<HolidayEventDto> {
    LOG.debug("Entered getGovUKBankHolidays, futureOnly: {}", futureOnly)
    var bankHolidays = getBankHolidays()?.englandAndWalesHolidays?.events.orEmpty()
    if (futureOnly && bankHolidays.isNotEmpty()) {
      bankHolidays = bankHolidays.filter { futureDatedHolidays.test(it) }
    }
    return bankHolidays.sortedBy { it.date }
  }

  fun getGovUKBankHolidays(dateRange: DateRange): List<HolidayEventDto> {
    LOG.debug("Entered getGovUKBankHolidays, dateRange: {}", dateRange)
    val bankHolidays = getBankHolidays()?.englandAndWalesHolidays?.events.orEmpty().filter { it.date >= dateRange.fromDate && it.date <= dateRange.toDate }
    return bankHolidays.sortedBy { it.date }
  }

  private fun getBankHolidays(): HolidaysDto? {
    LOG.debug("Entered getBankHolidays")
    var holidaysDto: HolidaysDto? = null
    try {
      holidaysDto = govUKHolidayClient.getHolidays()
    } catch (e: WebClientResponseException) {
      // log and ignore the error
      LOG.info("Failed to acquire bank holidays from gov uk api, exception: {}", e.message)
    }

    return holidaysDto
  }
}
