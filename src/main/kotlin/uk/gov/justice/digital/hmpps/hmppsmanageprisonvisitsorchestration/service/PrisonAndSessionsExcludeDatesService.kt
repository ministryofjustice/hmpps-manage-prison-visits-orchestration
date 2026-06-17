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
  private val manageUsersService: ManageUsersService,
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
        // get all exclude dates for the session without setting usernames
        val excludedSessionsForSession = sessionsService.getFutureExcludeDatesForSessionTemplate(sessionTemplateReference = sessionTemplateReference, withUsernames = false)
        sessionExclusionsMap[sessionTemplateReference] = excludedSessionsForSession
      }
    }

    setActionedByFullName(sessionExclusionsMap)

    return sessionExclusionsMap
  }

  private fun setActionedByFullName(sessionExclusionsMap: Map<String, List<ExcludeDateDto>>) {
    val userIds = sessionExclusionsMap.values.flatten().map { it.actionedBy }.toSet()

    val userNames = manageUsersService.getFullNamesForUserIds(userIds)
    sessionExclusionsMap.forEach { (_, excludeDateDtos) ->
      excludeDateDtos.forEach { excludeDateDto -> excludeDateDto.actionedBy = userNames[excludeDateDto.actionedBy] ?: excludeDateDto.actionedBy }
    }
  }
}
