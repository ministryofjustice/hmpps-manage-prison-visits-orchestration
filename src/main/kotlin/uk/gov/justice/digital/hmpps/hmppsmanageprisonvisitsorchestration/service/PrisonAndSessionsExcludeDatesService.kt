package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitSchedulerClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.prisons.ExcludeDateDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.prisons.PrisonAndSessionsExcludeDatesDto

@Service
class PrisonAndSessionsExcludeDatesService(
  private val prisonService: PrisonService,
  private val sessionsService: VisitSchedulerSessionsService,
  private val visitSchedulerClient: VisitSchedulerClient,
) {
  companion object {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getFuturePrisonAndSessionExcludeDates(prisonCode: String, includeSessions: Boolean): PrisonAndSessionsExcludeDatesDto {
    logger.info("Getting future exclude dates for prison $prisonCode and sessions")
    val prisonExcludeDates = prisonService.getFutureExcludeDatesForPrison(prisonCode)
    val sessionExclusionsMap = if (includeSessions) getCurrentAndFutureSessionExclusions(prisonCode) else emptyMap()

    return PrisonAndSessionsExcludeDatesDto(fullDateExclusions = prisonExcludeDates, sessionExclusions = sessionExclusionsMap)
  }

  private fun getCurrentAndFutureSessionExclusions(prisonCode: String): Map<String, List<ExcludeDateDto>> {
    // get all current or future sessions
    val currentOrFutureSessions = visitSchedulerClient.getCurrentOrFutureSessionTemplates(prisonCode)

    // get exclude dates for each session
    val sessionExclusionsMap = mutableMapOf<String, List<ExcludeDateDto>>()
    currentOrFutureSessions?.let {
      currentOrFutureSessions.map { it.reference }.distinct().forEach { sessionTemplateReference ->
        val excludedSessionsForSession = sessionsService.getFutureExcludeDatesForSessionTemplate(sessionTemplateReference)
        sessionExclusionsMap[sessionTemplateReference] = excludedSessionsForSession
      }
    }

    return sessionExclusionsMap
  }
}
