package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.util.function.Tuple4
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonerOffenderSearchClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.PrisonerProfileDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.RestPage
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.InmateDetailDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.PrisonerBookingSummaryDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.VisitBalancesDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.InvalidPrisonerProfileException
import java.time.Duration

@Service
class PrisonerProfileService(
  private val prisonApiClient: PrisonApiClient,
  private val prisonerOffenderSearchClient: PrisonerOffenderSearchClient,
  @Value("\${prisoner.profile.timeout:10s}") private val apiTimeout: Duration

) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

/*
  TODO - keep this till we do the performance tests
  fun getPrisonerProfile(offenderNo: String): PrisonerProfileDto? {
    val prisoner = prisonerOffenderSearchClient
      .getPrisonerById(offenderNo) ?: throw InvalidPrisonerProfileException("Unable to retrieve offender details from Prison Offender Search API")
    val inmateDetail = prisonApiClient
      .getInmateDetails(offenderNo) ?: throw InvalidPrisonerProfileException("Unable to retrieve inmate details from Prison API")
    val visitBalances = prisonApiClient
      .getVisitBalances(offenderNo) ?: throw InvalidPrisonerProfileException("Unable to retrieve visit balances from Prison API")

    val prisonerBookingSummaryList =
      prisonApiClient.getBookings(offenderNo, prisoner.prisonId)?.content
    val prisonerBookingSummary = prisonerBookingSummaryList?.firstOrNull()

    return PrisonerProfileDto(prisoner, inmateDetail, prisonerBookingSummary, visitBalances)
  }
*/

  fun getPrisonerProfile(prisonId: String, prisonerId: String): PrisonerProfileDto? {
    val prisoner = prisonerOffenderSearchClient.getPrisonerById(prisonerId)
    val inmateDetail = prisonApiClient.getInmateDetails(prisonerId)
    val visitBalances = prisonApiClient.getVisitBalances(prisonerId)
    val prisonerBookingSummaryList = prisonApiClient.getBookings(prisonId, prisonerId)

    val prisonerProfileTuple =
      Mono.zip(prisoner, inmateDetail, visitBalances, prisonerBookingSummaryList).block(apiTimeout)
    val prisonerProfile = getPrisonerProfileDto(prisonerProfileTuple)
    validatePrisonersPrisonId(prisonerProfile, prisonId)
    return prisonerProfile
  }

  fun getPrisonerProfileDto(
    prisonerProfileTuple: Tuple4<PrisonerDto, InmateDetailDto, VisitBalancesDto, RestPage<PrisonerBookingSummaryDto>>?
  ): PrisonerProfileDto {
    val prisoner = prisonerProfileTuple?.t1 ?: throw InvalidPrisonerProfileException("Unable to retrieve offender details from Prison Offender Search API")
    val inmateDetail = prisonerProfileTuple.t2 ?: throw InvalidPrisonerProfileException("Unable to retrieve inmate details from Prison API")
    val visitBalances = prisonerProfileTuple.t3 ?: throw InvalidPrisonerProfileException("Unable to retrieve visit balances from Prison API")
    val prisonerBookingSummaryList = prisonerProfileTuple.t4.content
    val prisonerBookingSummary = prisonerBookingSummaryList.firstOrNull()
    return PrisonerProfileDto(prisoner, inmateDetail, prisonerBookingSummary, visitBalances)
  }

  fun validatePrisonersPrisonId(prisonerProfile: PrisonerProfileDto, prisonId: String) {
    require(prisonerProfile.prisonId == prisonId) {
      throw InvalidPrisonerProfileException("Prisoner's prison ID - ${prisonerProfile.prisonId} does not match prisonId parameter - $prisonId")
    }
  }
}
