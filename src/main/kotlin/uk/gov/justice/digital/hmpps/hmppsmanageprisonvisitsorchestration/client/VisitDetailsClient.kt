package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitDto

@Component
class VisitDetailsClient(
  private val visitSchedulerClient: VisitSchedulerClient,
  private val hmppsAuthClient: HmppsAuthClient,
) {
  companion object {
    const val NOT_KNOWN = "NOT_KNOWN"
  }
  fun getExtendedVisitDetailsByReference(
    reference: String,
  ): VisitDto? {
    val visitDto = visitSchedulerClient.getVisitByReference(reference)
    visitDto?.also {
      // TODO - move this into Mono.zip call so that the calls are made in parallel to improve performance
      it.createdByFullName = getFullName(it.createdBy)
      it.updatedByFullName = getFullName(it.updatedBy)
      it.cancelledByFullName = getFullName(it.cancelledBy)
    }
    return visitDto
  }

  private fun getFullName(actionedBy: String?): String? {
    return actionedBy?.let {
      // for past visits or some exceptional circumstances actionedBy will be NOT_KNOWN
      return if (actionedBy == NOT_KNOWN) {
        actionedBy
      } else {
        try {
          hmppsAuthClient.getUserDetails(actionedBy)?.name
        } catch (e: Exception) {
          if (e is WebClientResponseException && e.statusCode == HttpStatus.NOT_FOUND) null else throw e
        }
      }
    }
  }
}
