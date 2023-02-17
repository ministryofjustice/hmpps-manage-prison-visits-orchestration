package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonerOffenderSearchClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.PrisonerProfileDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.InvalidPrisonerProfileException

@Service
class PrisonerProfileService(
  private val prisonApiClient: PrisonApiClient,
  private val prisonerOffenderSearchClient: PrisonerOffenderSearchClient,
) {
  fun getPrisonerProfile(offenderNo: String): PrisonerProfileDto? {
    val prisoner = prisonerOffenderSearchClient.getPrisonerById(offenderNo)?:
      throw InvalidPrisonerProfileException("Unable to retrieve offender details from Prison Offender Search API")
    val inmateDetail = prisonApiClient.getInmateDetails(offenderNo)?:
      throw InvalidPrisonerProfileException("Unable to retrieve inmate details from Prison API")
    val visitBalances = prisonApiClient.getVisitBalances(offenderNo)

    val prisonerBookingSummaryList =
      prisonApiClient.getBookings(offenderNo, prisoner.prisonId)?.content
    val prisonerBookingSummary = prisonerBookingSummaryList?.firstOrNull()

    return PrisonerProfileDto(prisoner, inmateDetail, prisonerBookingSummary, visitBalances)
  }
}
