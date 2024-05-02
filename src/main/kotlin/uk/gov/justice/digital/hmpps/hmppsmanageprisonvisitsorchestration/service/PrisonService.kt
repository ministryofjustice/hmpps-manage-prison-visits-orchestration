package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitSchedulerClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.DateRange
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.PrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType
import java.time.LocalDate

@Service
class PrisonService(
  private val visitSchedulerClient: VisitSchedulerClient,
) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getPrison(prisonCode: String): PrisonDto {
    return visitSchedulerClient.getPrison(prisonCode)!!
  }

  fun getSupportedPrisons(type: UserType): List<String>? {
    return visitSchedulerClient.getSupportedPrisons(type)
  }

  fun getToDaysBookableDateRange(
    prisonCode: String,
  ): DateRange {
    return getToDaysDateRange(getPrison(prisonCode))
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
