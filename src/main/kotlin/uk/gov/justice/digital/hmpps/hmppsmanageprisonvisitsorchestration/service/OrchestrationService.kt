package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.BookingOrchestrationRequestDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.CancelVisitOrchestrationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.IgnoreVisitNotificationsOrchestrationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.VisitDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.filter.VisitSearchRequestFilter
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.VisitSchedulerService.Companion.LOG

@Service
class OrchestrationService(
  private val visitSchedulerService: VisitSchedulerService,
) {
  fun getVisitByReference(reference: String): VisitDetailsDto? {
    return visitSchedulerService.getVisitByReference(reference)?.let {
      VisitDetailsDto(it)
    }
  }

  fun visitsSearch(visitSearchRequestFilter: VisitSearchRequestFilter): Page<VisitDetailsDto>? {
    try {
      return visitSchedulerService.visitsSearch(visitSearchRequestFilter)?.map {
        VisitDetailsDto(it)
      }
    } catch (e: WebClientResponseException) {
      if (e.statusCode != HttpStatus.NOT_FOUND) {
        LOG.error("Exception thrown on visit-scheduler call - /visits/search - with parameters - $visitSearchRequestFilter", e)
        throw e
      }
    }

    return Page.empty()
  }

  fun bookVisit(applicationReference: String, requestDto: BookingOrchestrationRequestDto): VisitDetailsDto? {
    return visitSchedulerService.bookVisit(applicationReference, requestDto)?.let {
      VisitDetailsDto(it)
    }
  }

  fun cancelVisit(reference: String, cancelVisitDto: CancelVisitOrchestrationDto): VisitDetailsDto? {
    return visitSchedulerService.cancelVisit(reference, cancelVisitDto)?.let {
      VisitDetailsDto(it)
    }
  }

  fun ignoreVisitNotifications(reference: String, ignoreVisitNotifications: IgnoreVisitNotificationsOrchestrationDto): VisitDetailsDto? {
    return visitSchedulerService.ignoreVisitNotifications(reference, ignoreVisitNotifications)?.let {
      VisitDetailsDto(it)
    }
  }

  fun findFutureVisitsForPrisoner(prisonerId: String): List<VisitDetailsDto> {
    return visitSchedulerService.findFutureVisitsForPrisoner(prisonerId).map { VisitDetailsDto(it) }
  }
}
