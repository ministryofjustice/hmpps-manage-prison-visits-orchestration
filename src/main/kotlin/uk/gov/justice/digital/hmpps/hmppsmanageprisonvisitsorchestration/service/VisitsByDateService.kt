package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitSchedulerClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionTimeSlotDto
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
    return visitSchedulerClient.getVisitsForSessionTemplateAndDate(
      sessionTemplateReference,
      sessionDate,
      visitStatus,
      visitRestrictions,
      prisonCode,
      page = PAGE_NUMBER,
      size = MAX_VISIT_RECORDS,
    )?.toList()?.map { visit ->
      val visitorCount = visit.visitors?.size ?: 0
      val visitTimeSlot = SessionTimeSlotDto(visit.startTimestamp.toLocalTime(), visit.endTimestamp.toLocalTime())
      try {
        val prisoner = prisonerSearchClient.getPrisonerById(visit.prisonerId)
        VisitPreviewDto(visit.prisonerId, prisoner.firstName, prisoner.lastName, visit.reference, visitorCount, visitTimeSlot)
      } catch (e: Exception) {
        VisitPreviewDto(visit.prisonerId, visit.reference, visitorCount, visitTimeSlot)
      }
    } ?: emptyList()
  }
}
