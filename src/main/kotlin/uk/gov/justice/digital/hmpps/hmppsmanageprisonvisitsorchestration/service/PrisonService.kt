package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.PrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.PrisonNotFoundException
import java.time.LocalDate

@Service
class PrisonService(
  private val visitSchedulerService: VisitSchedulerService,
) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  private fun getActivePublicPrison(prisonCode: String): PrisonDto {
    val prison = getPrison(prisonCode)
    return if (isPrisonActive(prison, UserType.PUBLIC)) {
      prison
    } else {
      throw PrisonNotFoundException("Prison with code - $prisonCode, is not active for usertype - ${UserType.PUBLIC}")
    }
  }

  fun getPrison(prisonCode: String): PrisonDto {
    return visitSchedulerService.getPrison(prisonCode)?: throw PrisonNotFoundException("Prison with code - $prisonCode, not found on visit-scheduler")
  }

  private fun isPrisonActive(prison: PrisonDto, userType: UserType): Boolean {
    val isPrisonActive = prison.active
    val isPrisonActiveForUserType = prison.clients.firstOrNull { it.userType == userType}?.active ?: false
    return isPrisonActive && isPrisonActiveForUserType
  }

  fun getLastBookableSessionDate(prison: PrisonDto, date: LocalDate): LocalDate {
    return date.plusDays(prison.policyNoticeDaysMax.toLong())
  }
}
