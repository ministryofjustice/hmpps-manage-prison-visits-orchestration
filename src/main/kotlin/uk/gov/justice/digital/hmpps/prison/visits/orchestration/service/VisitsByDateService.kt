package uk.gov.justice.digital.hmpps.prison.visits.orchestration.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.client.VisitSchedulerClient
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.prisoner.search.AttributeSearchPrisonerDto
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.visit.scheduler.VisitPreviewDto
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.visit.scheduler.enums.VisitRestriction
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.visit.scheduler.enums.VisitStatus
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

  fun getVisitsForSessionTemplateAndDate(
    sessionTemplateReference: String?,
    sessionDate: LocalDate,
    visitStatus: List<VisitStatus>,
    visitRestrictions: List<VisitRestriction>?,
    prisonCode: String,
  ): List<VisitPreviewDto> {
    LOG.debug(
      "Entered getVisitsBySessionTemplateForDate sessionTemplateReference:{} , sessionDate:{}, visitStatus: {}, visitRestrictions: {}, prisonCode : {}",
      sessionTemplateReference,
      sessionDate,
      visitStatus,
      visitRestrictions,
      prisonCode,
    )

    val visits = visitSchedulerClient.getVisitsForSessionTemplateAndDate(
      sessionTemplateReference,
      sessionDate,
      visitStatus,
      visitRestrictions,
      prisonCode,
      page = PAGE_NUMBER,
      size = MAX_VISIT_RECORDS,
    )?.toList() ?: emptyList()

    if (visits.isEmpty()) {
      return visits
    }

    var prisonerDetailsList = emptyList<AttributeSearchPrisonerDto>()
    var prisonerIds = emptyList<String>()
    try {
      prisonerIds = visits.map { it.prisonerId }.distinct().toList()
      prisonerDetailsList = prisonerSearchClient.getPrisonersByPrisonerIdsAttributeSearch(prisonerIds)?.toList() ?: emptyList()
    } catch (e: Exception) {
      LOG.error("Unable to load prisoner details for prisoners - $prisonerIds, exception - $e")
    }

    visits.forEach { visit ->
      val prisonerDetails = prisonerDetailsList.firstOrNull { visit.prisonerId == it.prisonerNumber }

      if (prisonerDetails != null) {
        visit.firstName = prisonerDetails.firstName
        visit.lastName = prisonerDetails.lastName
      }
    }

    return visits
  }
}
