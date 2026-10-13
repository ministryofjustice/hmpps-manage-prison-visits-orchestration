package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.GovUKHolidayClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.govuk.holidays.HolidayEventDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.DateRange
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.utils.CurrentDateUtils
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
    var bankHolidays = getBankHolidays()
    if (futureOnly && bankHolidays.isNotEmpty()) {
      bankHolidays = bankHolidays.filter { futureDatedHolidays.test(it) }
    }
    return bankHolidays.sortedBy { it.date }
  }

  fun getGovUKBankHolidays(dateRange: DateRange): List<HolidayEventDto> {
    LOG.debug("Entered getGovUKBankHolidays, dateRange: {}", dateRange)
    val bankHolidays = getBankHolidays().filter { it.date >= dateRange.fromDate && it.date <= dateRange.toDate }
    return bankHolidays.sortedBy { it.date }
  }

  private fun getBankHolidays(): List<HolidayEventDto> {
    LOG.debug("Entered getBankHolidays")

    // Holiday lookup failures must stop the request rather than shorten the booking notice period.
    val holidaysDto = govUKHolidayClient.getHolidays()
    if (holidaysDto == null) {
      throw IllegalStateException("No bank holiday data was returned by GOV.UK")
    }

    val bankHolidays = holidaysDto.englandAndWalesHolidays.events
    if (bankHolidays == null) {
      throw IllegalStateException("England and Wales bank holiday events were not returned by GOV.UK")
    }

    return bankHolidays
  }
}
