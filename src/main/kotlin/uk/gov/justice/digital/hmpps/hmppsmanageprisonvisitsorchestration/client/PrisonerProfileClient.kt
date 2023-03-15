package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.PrisonerProfileDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.InvalidPrisonerProfileException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.filter.VisitSearchRequestFilter
import java.time.Duration

@Component
class PrisonerProfileClient(
  private val prisonApiClient: PrisonApiClient,
  private val prisonerOffenderSearchClient: PrisonerOffenderSearchClient,
  private val visitSchedulerClient: VisitSchedulerClient,
  @Value("\${prisoner.profile.timeout:10s}") private val apiTimeout: Duration,
) {
  fun getPrisonerProfile(
    prisonId: String,
    prisonerId: String,
    visitSearchRequestFilter: VisitSearchRequestFilter,
  ): PrisonerProfileDto? {
    val prisonerPromise = prisonerOffenderSearchClient.getPrisonerByIdAsMono(prisonerId)
    val inmateDetailPromise = prisonApiClient.getInmateDetailsAsMono(prisonerId)
    val visitBalancesPromise = prisonApiClient.getVisitBalancesAsMono(prisonerId)
    val prisonerBookingSummaryPromise = prisonApiClient.getBookingsAsMono(prisonId, prisonerId)
    val visitSchedulerPromise = visitSchedulerClient.getVisitsAsMono(visitSearchRequestFilter)

    return Mono.zip(prisonerPromise, inmateDetailPromise, visitBalancesPromise, prisonerBookingSummaryPromise, visitSchedulerPromise)
      .map {
        PrisonerProfileDto(
          it.t1 ?: throw InvalidPrisonerProfileException("Unable to retrieve offender details from Prison Offender Search API"),
          it.t2 ?: throw InvalidPrisonerProfileException("Unable to retrieve inmate details from Prison API"),
          if (it.t3.isEmpty) null else it.t3.get(),
          it.t4.content.firstOrNull(),
          it.t5.content,
        )
      }.block(apiTimeout)
  }
}
