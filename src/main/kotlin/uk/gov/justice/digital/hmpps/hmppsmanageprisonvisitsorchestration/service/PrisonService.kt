package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.PrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType
import java.time.LocalDate

@Service
class PrisonService(
  private val visitSchedulerService: VisitSchedulerService,
) {
  companion object {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getPrison(prisonCode: String): PrisonDto? {
    var prison: PrisonDto? = null

    try {
      prison = visitSchedulerService.getPrison(prisonCode)
    } catch (e: WebClientResponseException) {
      logger.info("Failed to get details for prison - $prisonCode from visit-scheduler, error = ${e.message}")
      if (e.statusCode != HttpStatus.NOT_FOUND) {
        throw e
      }
    }

    return prison
  }

  fun isActive(prison: PrisonDto): Boolean {
    return prison.active
  }

  fun isActive(prison: PrisonDto, userType: UserType): Boolean {
    return prison.clients.firstOrNull { it.userType == userType }?.active ?: false
  }

  fun getLastBookableSessionDate(prison: PrisonDto, date: LocalDate): LocalDate {
    return date.plusDays(prison.policyNoticeDaysMax.toLong())
  }
}
