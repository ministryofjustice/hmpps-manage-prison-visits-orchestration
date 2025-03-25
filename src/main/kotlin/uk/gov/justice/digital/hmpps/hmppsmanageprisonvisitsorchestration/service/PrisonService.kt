package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonRegisterClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitSchedulerClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.PrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.register.PrisonRegisterContactDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.register.PrisonRegisterPrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.DateRange
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.prisons.ExcludeDateDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.prisons.IsExcludeDateDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.utils.DateUtils
import java.time.LocalDate

@Service
class PrisonService(
  val visitSchedulerClient: VisitSchedulerClient,
  private val prisonRegisterClient: PrisonRegisterClient,
  private val dateUtils: DateUtils,
  private val excludeDatesService: ExcludeDatesService,
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
    val prisonRegisterPrisonContractDetails = prisonRegisterClient.getPrisonContactDetails(prisonCode).orElse(PrisonRegisterContactDetailsDto())

    return PrisonDto(visitSchedulerPrison, prisonRegisterPrison, prisonRegisterPrisonContractDetails)
  }

  fun getSupportedPrisons(type: UserType): List<String> = visitSchedulerClient.getSupportedPrisons(type)

  fun getSupportedPrisonsDetails(type: UserType): List<PrisonRegisterPrisonDto> {
    val supportedPrisonIds = visitSchedulerClient.getSupportedPrisons(type)
    return if (supportedPrisonIds.isNotEmpty()) {
      prisonRegisterClient.prisonsByIds(supportedPrisonIds) ?: emptyList()
    } else {
      emptyList()
    }
  }

  fun getFutureExcludeDatesForPrison(prisonCode: String): List<ExcludeDateDto> {
    val excludeDates = getExcludeDatesForPrison(prisonCode)
    return excludeDatesService.getFutureExcludeDates(excludeDates)
  }

  fun getPastExcludeDatesForPrison(prisonCode: String): List<ExcludeDateDto> {
    val excludeDates = getExcludeDatesForPrison(prisonCode)
    return excludeDatesService.getPastExcludeDates(excludeDates)
  }

  fun isDateExcludedForPrisonVisits(prisonCode: String, date: LocalDate): IsExcludeDateDto {
    logger.trace("isDateExcluded - prison - {}, date - {}", prisonCode, date)
    val excludeDates = getExcludeDatesForPrison(prisonCode)
    val isExcluded = excludeDatesService.isDateExcluded(excludeDates, date)
    logger.trace("isDateExcluded - prison - {}, date - {}, isExcluded - {}", prisonCode, date, isExcluded)
    return isExcluded
  }

  fun addExcludeDateForPrison(prisonCode: String, prisonExcludeDate: ExcludeDateDto): List<LocalDate> = visitSchedulerClient.addPrisonExcludeDate(prisonCode, prisonExcludeDate)?.sortedByDescending { it } ?: emptyList()

  fun removeExcludeDateForPrison(prisonCode: String, prisonExcludeDate: ExcludeDateDto): List<LocalDate> = visitSchedulerClient.removePrisonExcludeDate(prisonCode, prisonExcludeDate)?.sortedByDescending { it } ?: emptyList()

  fun getToDaysBookableDateRange(
    prisonCode: String,
    fromDateOverride: Int? = null,
    toDateOverride: Int? = null,
  ): DateRange {
    val prison = visitSchedulerClient.getPrison(prisonCode)
    return dateUtils.getToDaysDateRange(prison = prison, minOverride = fromDateOverride, maxOverride = toDateOverride)
  }

  private fun getExcludeDatesForPrison(prisonCode: String): List<ExcludeDateDto> = visitSchedulerClient.getPrisonExcludeDates(prisonCode) ?: emptyList()
}
