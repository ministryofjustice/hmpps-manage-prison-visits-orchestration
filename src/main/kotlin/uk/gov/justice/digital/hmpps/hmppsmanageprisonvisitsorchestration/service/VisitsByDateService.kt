package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitSchedulerClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitPreviewDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitRestriction
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitStatus
import java.time.LocalDate

@Service
class VisitsByDateService(
  private val visitSchedulerClient: VisitSchedulerClient,
  private val prisonerSearchClient: PrisonerSearchClient,
) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
    const val MAX_VISIT_RECORDS: Int = 1000
    const val PAGE_NUMBER: Int = 0
  }

  fun getVisitsForSessionTemplateAndDate(sessionTemplateReference: String, sessionDate: LocalDate, visitStatus: List<VisitStatus>, visitRestrictions: List<VisitRestriction>?): List<VisitPreviewDto> {
    LOG.debug(
      "Entered getVisitsBySessionTemplateForDate sessionTemplateReference:{} , sessionDate:{}, visitStatus: {}, visitRestrictions: {}",
      sessionTemplateReference,
      sessionDate,
      visitStatus,
      visitRestrictions,
    )
    return visitSchedulerClient.getVisitsForSessionTemplateAndDate(
      sessionTemplateReference,
      sessionDate,
      visitStatus,
      visitRestrictions,
      page = PAGE_NUMBER,
      size = MAX_VISIT_RECORDS,
    )?.toList()?.map { visit ->
      try {
        prisonerSearchClient.getPrisonerById(visit.prisonerId)?.let { prisoner ->
          VisitPreviewDto(visit.prisonerId, prisoner.firstName, prisoner.lastName, visit.reference)
        } ?: VisitPreviewDto(visit.prisonerId, visit.reference)
      } catch (e: Exception) {
        VisitPreviewDto(visit.prisonerId, visit.reference)
      }
    } ?: emptyList()
  }
}
