package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonRegisterClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitSchedulerClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.PrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.DateRange
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSchedulerPrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType
import java.time.LocalDate

@Service
class PrisonService(
  val visitSchedulerClient: VisitSchedulerClient,
  private val prisonRegisterClient: PrisonRegisterClient,
) {
  companion object {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
  }


  /**
   * Gets the prison details from VSIP and also the prison name from prison-register.
   * Only use if the name is also needed.
   */
  fun getPrisonWithName(prisonCode: String): PrisonDto {
    val visitSchedulerPrison = visitSchedulerClient.getPrison(prisonCode)
    val prisonRegisterPrison = prisonRegisterClient.getPrison(prisonCode)
    return PrisonDto(visitSchedulerPrison, prisonRegisterPrison)
  }

  fun getVSIPPrison(prisonCode: String): VisitSchedulerPrisonDto {
    return visitSchedulerClient.getPrison(prisonCode)
  }

  fun isActive(prison: VisitSchedulerPrisonDto): Boolean {
    return prison.active
  }

  fun isActive(prison: VisitSchedulerPrisonDto, userType: UserType): Boolean {
    return prison.clients.firstOrNull { it.userType == userType }?.active ?: false
  }

  fun getLastBookableSessionDate(prison: VisitSchedulerPrisonDto, date: LocalDate): LocalDate {
    return date.plusDays(prison.policyNoticeDaysMax.toLong())
  }

  fun getSupportedPrisons(type: UserType): List<String> {
    return visitSchedulerClient.getSupportedPrisons(type)
  }

  fun getToDaysBookableDateRange(
    prisonCode: String,
  ): DateRange {
    val prison = visitSchedulerClient.getPrison(prisonCode)
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
