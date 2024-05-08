package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitSchedulerClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.DateRange
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.PrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.NotFoundException
import java.time.LocalDate
import kotlin.jvm.optionals.getOrNull

@Service
class PrisonService(
  private val visitSchedulerClient: VisitSchedulerClient,
) {
  companion object {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getPrison(prisonCode: String): PrisonDto {
    return visitSchedulerClient.getPrison(prisonCode)?.getOrNull() ?: throw NotFoundException("Prison with prison code - $prisonCode not found on visit-scheduler")
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
