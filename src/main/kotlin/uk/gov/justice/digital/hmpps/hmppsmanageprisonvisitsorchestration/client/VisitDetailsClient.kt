package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.hmpps.auth.UserDetails
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitDto
import java.time.Duration
import java.util.Optional

@Component
class VisitDetailsClient(
  private val visitSchedulerClient: VisitSchedulerClient,
  private val hmppsAuthClient: HmppsAuthClient,
  @Value("\${hmpps.auth.timeout:10s}") private val apiTimeout: Duration,
) {
  companion object {
    const val NOT_KNOWN = "NOT_KNOWN"
  }
  fun getFullVisitDetailsByReference(
    reference: String,
  ): VisitDto? {
    val visitDto = visitSchedulerClient.getVisitByReference(reference)
    visitDto?.also {
      val createdByMono = getUserDetails(visitDto.createdBy)
      val updatedByMono = getUserDetails(visitDto.updatedBy)
      val cancelledByMono = getUserDetails(visitDto.cancelledBy)

      val userDetails = Mono.zip(createdByMono, updatedByMono, cancelledByMono).block(apiTimeout)
      visitDto.createdByFullName = getFullName(userDetails?.t1)
      visitDto.updatedByFullName = getFullName(userDetails?.t2)
      visitDto.cancelledByFullName = getFullName(userDetails?.t3)
    }
    return visitDto
  }

  private fun getUserDetails(actionedBy: String?): Mono<Optional<UserDetails>> {
    actionedBy?.let {
      // for past visits or some exceptional circumstances actionedBy will be NOT_KNOWN
      if (actionedBy == NOT_KNOWN) {
        return Mono.just(Optional.of(UserDetails(actionedBy, NOT_KNOWN)))
      } else {
        return hmppsAuthClient.getUserDetails(actionedBy)
      }
    }

    return Mono.just(Optional.empty())
  }

  private fun getFullName(userDetails: Optional<UserDetails>?): String? {
    return if (userDetails?.isEmpty == true) null else userDetails?.get()?.name
  }
}
