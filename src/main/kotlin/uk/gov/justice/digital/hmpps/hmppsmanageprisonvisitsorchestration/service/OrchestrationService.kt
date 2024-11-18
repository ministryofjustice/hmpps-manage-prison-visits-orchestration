package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.BookingOrchestrationRequestDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.CancelVisitOrchestrationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.IgnoreVisitNotificationsOrchestrationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSummaryDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.filter.VisitSearchRequestFilter

@Service
class OrchestrationService(
  private val visitSchedulerService: VisitSchedulerService,
) {
  companion object {
    private val LOG = LoggerFactory.getLogger(OrchestrationService::class.java)
  }
  fun getVisitByReference(reference: String): VisitSummaryDto? {
    return visitSchedulerService.getVisitByReference(reference)?.let {
      VisitSummaryDto(it)
    }
  }

  fun visitsSearch(visitSearchRequestFilter: VisitSearchRequestFilter): Page<VisitSummaryDto>? {
    try {
      return visitSchedulerService.visitsSearch(visitSearchRequestFilter)?.map {
        VisitSummaryDto(it)
      }
    } catch (e: WebClientResponseException) {
      if (e.statusCode != HttpStatus.NOT_FOUND) {
        LOG.error("Exception thrown on visit-scheduler call - /visits/search - with parameters - $visitSearchRequestFilter", e)
        throw e
      }
    }

    return Page.empty()
  }

  fun bookVisit(applicationReference: String, requestDto: BookingOrchestrationRequestDto): VisitSummaryDto? {
    return visitSchedulerService.bookVisit(applicationReference, requestDto)?.let {
      VisitSummaryDto(it)
    }
  }

  fun cancelVisit(reference: String, cancelVisitDto: CancelVisitOrchestrationDto): VisitSummaryDto? {
    return visitSchedulerService.cancelVisit(reference, cancelVisitDto)?.let {
      VisitSummaryDto(it)
    }
  }

  fun ignoreVisitNotifications(reference: String, ignoreVisitNotifications: IgnoreVisitNotificationsOrchestrationDto): VisitSummaryDto? {
    return visitSchedulerService.ignoreVisitNotifications(reference, ignoreVisitNotifications)?.let {
      VisitSummaryDto(it)
    }
  }

  fun findFutureVisitsForPrisoner(prisonerId: String): List<VisitSummaryDto> {
    return visitSchedulerService.findFutureVisitsForPrisoner(prisonerId).map { VisitSummaryDto(it) }
  }
}
