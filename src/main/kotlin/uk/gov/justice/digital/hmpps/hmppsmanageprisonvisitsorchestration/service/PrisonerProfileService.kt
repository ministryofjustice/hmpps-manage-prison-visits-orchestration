package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import jakarta.validation.ValidationException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonerOffenderSearchClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitSchedulerClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.PrisonerProfileDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.InvalidPrisonerProfileException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.filter.VisitSearchRequestFilter
import java.time.Duration
import java.time.LocalDateTime
import java.time.Period
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

@Service
class PrisonerProfileService(
  private val prisonApiClient: PrisonApiClient,
  private val prisonerOffenderSearchClient: PrisonerOffenderSearchClient,
  private val visitSchedulerClient: VisitSchedulerClient,
  @Value("\${prisoner.profile.timeout:10s}") private val apiTimeout: Duration,
  @Value("\${prisoner.profile.past-visits.duration-in-months: P3M}") private val pastVisitsPeriod: Period,
  @Value("\${prisoner.profile.future-visits.duration-in-months: P2M}") private val futureVisitsPeriod: Period,
) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
    const val MAX_RECORDS: Int = 1000
    const val PAGE: Int = 0
  }

  fun getPrisonerProfile(prisonId: String, prisonerId: String): PrisonerProfileDto? {
    val prisonerPromise = prisonerOffenderSearchClient.getPrisonerById(prisonerId)
    val inmateDetailPromise = prisonApiClient.getInmateDetails(prisonerId)
    val visitBalancesPromise = prisonApiClient.getVisitBalances(prisonerId)
    val prisonerBookingSummaryPromise = prisonApiClient.getBookings(prisonId, prisonerId)
    val visitSchedulerPromise = visitSchedulerClient.getVisits(
      createVisitSearchFilterForProfile(prisonerId, getStartDate(LocalDateTime.now(), pastVisitsPeriod), getEndDate(LocalDateTime.now(), futureVisitsPeriod)),
    )

    val prisonerProfile =
      Mono.zip(prisonerPromise, inmateDetailPromise, visitBalancesPromise, prisonerBookingSummaryPromise, visitSchedulerPromise)
        .map {
          PrisonerProfileDto(
            it.t1 ?: throw InvalidPrisonerProfileException("Unable to retrieve offender details from Prison Offender Search API"),
            it.t2 ?: throw InvalidPrisonerProfileException("Unable to retrieve inmate details from Prison API"),
            if (it.t3.isEmpty) null else it.t3.get(),
            it.t4.content.firstOrNull(),
            it.t5.content,
          )
        }.block(apiTimeout)
    validatePrisonersPrisonId(prisonerProfile, prisonId)
    return prisonerProfile
  }

  private fun validatePrisonersPrisonId(prisonerProfile: PrisonerProfileDto?, prisonId: String) {
    prisonerProfile?.let {
      require(it.prisonId == prisonId) {
        throw ValidationException("Prisoner's prison ID - ${it.prisonId} does not match prisonId parameter - $prisonId")
      }
    }
  }

  private fun getStartDate(dateTime: LocalDateTime, period: Period): LocalDateTime {
    return dateTime.minus(period).with(TemporalAdjusters.firstDayOfMonth()).truncatedTo(ChronoUnit.DAYS)
  }

  private fun getEndDate(dateTime: LocalDateTime, period: Period): LocalDateTime {
    return dateTime.plus(period).with(TemporalAdjusters.lastDayOfMonth()).truncatedTo(ChronoUnit.DAYS)
  }

  private fun createVisitSearchFilterForProfile(
    prisonerId: String,
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
  ): VisitSearchRequestFilter {
    return VisitSearchRequestFilter(
      prisonerId = prisonerId,
      startDateTime = startDateTime,
      endDateTime = endDateTime,
      visitStatusList = listOf("BOOKED", "CANCELLED"),
      page = PAGE,
      size = MAX_RECORDS,
    )
  }
}
