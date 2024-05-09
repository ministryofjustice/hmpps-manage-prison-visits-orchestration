package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import jakarta.validation.ValidationException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonRegisterClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.PrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.DateRange
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSchedulerPrisonDto
import java.time.LocalDate

@Service
class PrisonService(
  @Lazy
  @Autowired
  val visitSchedulerService: VisitSchedulerService,

  private val prisonRegisterClient: PrisonRegisterClient,
) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getPrison(prisonCode: String): PrisonDto {
    val visitSchedulerPrison = visitSchedulerService.getPrison(prisonCode)
    // TODO - throw NotFoundException
    val prisonRegisterPrison = prisonRegisterClient.getPrison(prisonCode) ?: throw ValidationException("Prison with code - $prisonCode not found on prison-register")
    return PrisonDto(visitSchedulerPrison, prisonRegisterPrison)
  }

  fun getToDaysBookableDateRange(
    prisonCode: String,
  ): DateRange {
    val prison = visitSchedulerService.getPrison(prisonCode)
    return getToDaysDateRange(prison)
  }

  fun getToDaysDateRange(
    prison: VisitSchedulerPrisonDto,
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
