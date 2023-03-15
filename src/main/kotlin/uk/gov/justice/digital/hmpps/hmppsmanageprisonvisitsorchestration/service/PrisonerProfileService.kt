package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import jakarta.validation.ValidationException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonerProfileClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.PrisonerProfileDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.filter.VisitSearchRequestFilter
import java.time.LocalDateTime
import java.time.Period
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

@Service
class PrisonerProfileService(
  private val prisonerProfileClient: PrisonerProfileClient,
  @Value("\${prisoner.profile.past-visits.duration-in-months: P3M}") private val pastVisitsPeriod: Period,
  @Value("\${prisoner.profile.future-visits.duration-in-months: P2M}") private val futureVisitsPeriod: Period,
) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
    const val MAX_VISIT_RECORDS: Int = 1000
    const val PAGE_NUMBER: Int = 0
  }

  fun getPrisonerProfile(prisonId: String, prisonerId: String): PrisonerProfileDto? {
    val prisonerProfile = prisonerProfileClient.getPrisonerProfile(
      prisonId,
      prisonerId,
      createVisitSearchFilterForProfile(
        prisonerId = prisonerId,
        startDateTime = getStartDate(LocalDateTime.now(), pastVisitsPeriod),
        endDateTime = getEndDate(LocalDateTime.now(), futureVisitsPeriod),
      ),
    )
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
      page = PAGE_NUMBER,
      size = MAX_VISIT_RECORDS,
    )
  }
}
