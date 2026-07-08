package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitSchedulerClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.prisons.PrisonAndSessionsExcludeDatesDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.prisons.SessionExcludeDateDto

@Service
class PrisonAndSessionsExcludeDatesService(
  private val prisonService: PrisonService,
  private val visitSchedulerClient: VisitSchedulerClient,
  private val manageUsersService: ManageUsersService,
) {
  companion object {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    private val sessionExclusionSortOrder: Comparator<SessionExcludeDateDto> = compareBy(
      { it.excludeDate.excludeDate },
      { it.sessionTimeSlot.startTime },
      { it.sessionTimeSlot.endTime },
    )
  }

  fun getFuturePrisonAndSessionExcludeDates(prisonCode: String, includeSessions: Boolean): PrisonAndSessionsExcludeDatesDto {
    logger.info("Getting future exclude dates for prison $prisonCode and sessions")
    val prisonExcludeDates = prisonService.getFutureExcludeDatesForPrison(prisonCode)
    val sessionExclusions = if (includeSessions) getCurrentAndFutureSessionExclusions(prisonCode) else emptyList()

    return PrisonAndSessionsExcludeDatesDto(fullDateExclusions = prisonExcludeDates, sessionExclusions = sessionExclusions)
  }

  private fun getCurrentAndFutureSessionExclusions(prisonCode: String): List<SessionExcludeDateDto> {
    // get all current or future sessions that have date exclusions
    val currentOrFutureSessions = visitSchedulerClient.getFutureSessionTemplateExclusions(prisonCode)
    val sessionExclusions = mutableListOf<SessionExcludeDateDto>()

    currentOrFutureSessions?.forEach { session ->
      session.excludeDates.forEach {
        sessionExclusions.add(SessionExcludeDateDto(sessionSchedule = session.sessionSchedule, excludeDate = it))
      }
    }

    // set actual names for actionedBy
    setActionedByFullName(sessionExclusions)

    return sessionExclusions.sortedWith(sessionExclusionSortOrder)
  }

  private fun setActionedByFullName(sessionExclusions: List<SessionExcludeDateDto>) {
    val userIds = sessionExclusions.map { it.excludeDate.actionedBy }.toSet()

    val userNames = manageUsersService.getFullNamesForUserIds(userIds)
    sessionExclusions.forEach { sessionExclusion ->
      val actionedBy = sessionExclusion.excludeDate.actionedBy
      sessionExclusion.excludeDate.actionedBy = userNames[actionedBy] ?: actionedBy
    }
  }
}
