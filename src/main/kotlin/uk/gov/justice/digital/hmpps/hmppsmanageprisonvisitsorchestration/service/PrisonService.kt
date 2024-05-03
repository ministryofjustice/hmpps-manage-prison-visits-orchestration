package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitSchedulerClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.DateRange
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.PrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.NotFoundException
import java.time.LocalDate

@Service
class PrisonService(
  private val visitSchedulerClient: VisitSchedulerClient,
) {
  companion object {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getPrison(prisonCode: String): PrisonDto? {
    var prison: PrisonDto? = null

    try {
      prison = visitSchedulerClient.getPrison(prisonCode)
    } catch (e: WebClientResponseException) {
      logger.info("Failed to get details for prison - $prisonCode from visit-scheduler, error = ${e.message}")
      if (e.statusCode != HttpStatus.NOT_FOUND) {
        throw e
      }
    }

    return prison
  }

  fun isActive(prison: PrisonDto): Boolean {
    return prison.active
  }

  fun isActive(prison: PrisonDto, userType: UserType): Boolean {
    return prison.clients.firstOrNull { it.userType == userType }?.active ?: false
  }

  fun getLastBookableSessionDate(prison: PrisonDto, date: LocalDate): LocalDate {
    return date.plusDays(prison.policyNoticeDaysMax.toLong())
  }

  fun getSupportedPrisons(type: UserType): List<String>? {
    return visitSchedulerClient.getSupportedPrisons(type)
  }

  fun getToDaysBookableDateRange(
    prisonCode: String,
  ): DateRange {
    val prison = getPrison(prisonCode)
      ?: throw NotFoundException("Prison with prison code - $prisonCode not found on visit-scheduler")
    return getToDaysDateRange(prison)
  }

  fun getToDaysDateRange(
    prison: PrisonDto,
    minOverride: Int? = null,
    maxOverride: Int? = null,
  ): DateRange {
    val today = LocalDate.now()

    val min = minOverride ?: prison.policyNoticeDaysMin
    val max = maxOverride ?: prison.policyNoticeDaysMax

    val bookableStartDate = today.plusDays(min.toLong())
    val bookableEndDate = today.plusDays(max.toLong())
    return DateRange(bookableStartDate, bookableEndDate)
  }
}
