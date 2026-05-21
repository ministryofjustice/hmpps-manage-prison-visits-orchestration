package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitPassesClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitSchedulerClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitStatus
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.passes.VisitPassDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.passes.VisitPassRequestDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.filter.VisitSearchRequestFilter
import java.time.LocalDate

@Service
class VisitPassesService(
  private val visitPassesClient: VisitPassesClient,
  private val visitSchedulerClient: VisitSchedulerClient,
  private val telemetryClientService: TelemetryClientService,
) {
  companion object {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    // max visits per day per prison is around 150, setting an upper limit of 250 for now.
    private const val VISITS_PAGE_SIZE = 250
  }

  fun getVisitPasses(prisonId: String, visitPassRequest: VisitPassRequestDto): List<VisitPassDto> {
    logger.info("Getting visit passes for prison - $prisonId, visit date - ${visitPassRequest.date}")
    val visitDate = visitPassRequest.date
    val visitSearchRequestFilter = getVisitRequestSearchFilter(prisonId, visitDate)
    val visitsPagedResults = visitSchedulerClient.getVisits(visitSearchRequestFilter)

    val visits = if (visitsPagedResults == null) {
      emptyList()
    } else {
      if (visitsPagedResults.totalElements > VISITS_PAGE_SIZE) {
        val errorMessage = "More than $VISITS_PAGE_SIZE visits found for prison - $prisonId, visit date - $visitDate, total visits - ${visitsPagedResults.totalElements}"
        logger.error(errorMessage)
        throw IllegalStateException(errorMessage)
      } else {
        visitsPagedResults.toList()
      }
    }

    val visitPasses = if (visits.isNotEmpty()) {
      visitPassesClient.getVisitPasses(visits)
    } else {
      emptyList()
    }

    // write to app insights
    telemetryClientService.trackVisitPassesEvent(prisonCode = prisonId, visitDate = visitDate, actionedBy = visitPassRequest.actionedBy, totalVisits = visits.size)
    return visitPasses
  }

  private fun getVisitRequestSearchFilter(prisonId: String, visitDate: LocalDate) = VisitSearchRequestFilter(
    prisonCode = prisonId,
    visitStartDate = visitDate,
    visitEndDate = visitDate,
    visitStatusList = listOf(VisitStatus.BOOKED.name),
    page = 0,
    size = VISITS_PAGE_SIZE,
    prisonerId = null,
  )
}
