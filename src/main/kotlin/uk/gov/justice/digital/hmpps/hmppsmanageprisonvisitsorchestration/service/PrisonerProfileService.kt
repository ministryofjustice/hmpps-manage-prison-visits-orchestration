package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import jakarta.validation.ValidationException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonerProfileClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.PrisonerProfileDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.OffenderRestrictionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.filter.VisitSearchRequestFilter
import java.time.LocalDate
import java.time.Period
import java.time.temporal.TemporalAdjusters

@Service
class PrisonerProfileService(
  private val prisonerProfileClient: PrisonerProfileClient,
  private val prisonApiClient: PrisonApiClient,
  @Value("\${prisoner.profile.past-visits.duration-in-months: P3M}") private val pastVisitsPeriod: Period,
  @Value("\${prisoner.profile.future-visits.duration-in-months: P2M}") private val futureVisitsPeriod: Period,
) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
    const val MAX_VISIT_RECORDS: Int = 1000
    const val PAGE_NUMBER: Int = 0
  }

  fun getPrisonerProfile(prisonId: String, prisonerId: String): PrisonerProfileDto? {
    LOG.debug("Entered getPrisonerProfile prisonId:$prisonId , prisonerId:$prisonerId")
    val prisonerProfile = prisonerProfileClient.getPrisonerProfile(
      prisonId,
      prisonerId,
      createVisitSearchFilterForProfile(
        prisonerId = prisonerId,
        startDate = getStartDate(LocalDate.now(), pastVisitsPeriod),
        endDate = getEndDate(LocalDate.now(), futureVisitsPeriod),
      ),
    )
    validatePrisonersPrisonId(prisonerProfile, prisonId)
    return prisonerProfile
  }

  fun getRestrictions(prisonerId: String): List<OffenderRestrictionDto> {
    val offenderRestrictionsDto = prisonApiClient.getOffenderRestrictions(prisonerId)
    return offenderRestrictionsDto?.offenderRestrictions ?: listOf()
  }

  private fun validatePrisonersPrisonId(prisonerProfile: PrisonerProfileDto?, prisonId: String) {
    prisonerProfile?.let {
      require(it.prisonId == prisonId) {
        throw ValidationException("Prisoner's prison ID - ${it.prisonId} does not match prisonId parameter - $prisonId")
      }
    }
  }

  private fun getStartDate(date: LocalDate, period: Period): LocalDate {
    return date.minus(period).with(TemporalAdjusters.firstDayOfMonth())
  }

  private fun getEndDate(date: LocalDate, period: Period): LocalDate {
    return date.plus(period).with(TemporalAdjusters.lastDayOfMonth())
  }

  private fun createVisitSearchFilterForProfile(
    prisonerId: String,
    startDate: LocalDate,
    endDate: LocalDate,
  ): VisitSearchRequestFilter {
    return VisitSearchRequestFilter(
      prisonerId = prisonerId,
      visitStartDate = startDate,
      visitEndDate = endDate,
      visitStatusList = listOf("BOOKED", "CANCELLED"),
      page = PAGE_NUMBER,
      size = MAX_VISIT_RECORDS,
    )
  }
}
