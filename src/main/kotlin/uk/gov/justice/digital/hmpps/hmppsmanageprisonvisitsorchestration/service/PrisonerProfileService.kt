package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonerOffenderSearchClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.PrisonerProfileDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.InvalidPrisonerProfileException
import java.time.Duration

@Service
class PrisonerProfileService(
  private val prisonApiClient: PrisonApiClient,
  private val prisonerOffenderSearchClient: PrisonerOffenderSearchClient,
  @Value("\${prisoner.profile.timeout:10s}") private val apiTimeout: Duration,

) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getPrisonerProfile(prisonId: String, prisonerId: String): PrisonerProfileDto? {
    val prisonerPromise = prisonerOffenderSearchClient.getPrisonerById(prisonerId)
    val inmateDetailPromise = prisonApiClient.getInmateDetails(prisonerId)
    val visitBalancesPromise = prisonApiClient.getVisitBalances(prisonerId)
    val prisonerBookingSummaryPromise = prisonApiClient.getBookings(prisonId, prisonerId)

    val prisonerProfile =
      Mono.zip(prisonerPromise, inmateDetailPromise, visitBalancesPromise, prisonerBookingSummaryPromise)
        .map {
          PrisonerProfileDto(
            it.t1 ?: throw InvalidPrisonerProfileException("Unable to retrieve offender details from Prison Offender Search API"),
            it.t2 ?: throw InvalidPrisonerProfileException("Unable to retrieve inmate details from Prison API"),
            if (it.t3.isEmpty) null else it.t3.get(),
            it.t4.content.firstOrNull(),
          )
        }.block(apiTimeout)
    validatePrisonersPrisonId(prisonerProfile, prisonId)
    return prisonerProfile
  }

  fun validatePrisonersPrisonId(prisonerProfile: PrisonerProfileDto?, prisonId: String) {
    prisonerProfile?.let {
      require(it.prisonId == prisonId) {
        throw InvalidPrisonerProfileException("Prisoner's prison ID - ${it.prisonId} does not match prisonId parameter - $prisonId")
      }
    }
  }
}
