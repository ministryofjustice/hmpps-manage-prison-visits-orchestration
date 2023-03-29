package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.hmpps.auth.UserDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.VisitHistoryDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitDto
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

@Component
class VisitDetailsClient(
  private val visitSchedulerClient: VisitSchedulerClient,
  private val hmppsAuthClient: HmppsAuthClient,
  @Value("\${hmpps.auth.timeout:10s}") private val apiTimeout: Duration,
) {
  companion object {
    const val NOT_KNOWN = "NOT_KNOWN"
  }


  fun getVisitHistoryByReference(
    reference: String,
  ): VisitHistoryDetailsDto? {
    val visits = visitSchedulerClient.getVisitHistoryByReference(reference)
    if (visits.isNotEmpty()) {
      val lastVisit = visits.last()

      val firstVisit = visits.first()
      val userNames = getUserDetails(lastVisit)

      return VisitHistoryDetailsDto(
        createdBy = userNames[firstVisit.createdBy] ?: firstVisit.createdBy,
        cancelledBy = userNames[lastVisit.cancelledBy] ?: lastVisit.cancelledBy,
        updatedBy = userNames[lastVisit.updatedBy] ?: lastVisit.updatedBy,
        createdDateAndTime = firstVisit.createdTimestamp,
        updatedDateAndTime = getLastUpdatedVisitDateAndTime(visits),
        cancelledDateAndTime = getCanceledVisitDateAndTime(lastVisit),
        visit = lastVisit,
      )
    }
    return null
  }

  private fun getLastUpdatedVisitDateAndTime(visits: List<VisitDto>): LocalDateTime? {
    val lastVisitDto = visits.last()
    if (visits.size > 1) {
      return lastVisitDto.let { it.createdTimestamp }
    }
    return null
  }

  private fun getCanceledVisitDateAndTime(lastVisit: VisitDto): LocalDateTime? {
    return if (lastVisit.visitStatus == "CANCELED") {
      lastVisit.modifiedTimestamp
    } else {
      null
    }
  }

  private fun getUserDetails(visitDto: VisitDto): Map<String, String> {
    val monoCallsList = createUserMonoCalls(visitDto)
    return executeMonoCalls(monoCallsList)
  }

  private fun executeMonoCalls(monoCallsList: List<Mono<UserDetailsDto>>): Map<String, String> {
    var results: MutableMap<String, String> = mutableMapOf()
    if (monoCallsList.size == 1) {
      val userDetails = monoCallsList[0].block(apiTimeout)
      userDetails.fullName?.let { results[userDetails.username] = userDetails.fullName }
    }

    if (monoCallsList.size > 1) {
      var zipResults: List<UserDetailsDto> = mutableListOf()
      if (monoCallsList.size == 2) {
        val iterable = Mono.zip(monoCallsList[0], monoCallsList[1]).block(apiTimeout)
        zipResults = iterable.toList() as List<UserDetailsDto>
      }
      if (monoCallsList.size == 3) {
        val userDetailsTuples = Mono.zip(monoCallsList[0], monoCallsList[1], monoCallsList[2]).block(apiTimeout)
        zipResults = userDetailsTuples.toList() as List<UserDetailsDto>
      }
      zipResults?.forEach { userDetails -> userDetails.fullName?.let { results[userDetails.username] = userDetails.fullName } }
    }

    return results.toMap()
  }

  private fun createUserMonoCalls(
    visitDto: VisitDto,
  ): List<Mono<UserDetailsDto>> {
    val userNames = mutableSetOf(visitDto.createdBy, visitDto.updatedBy, visitDto.cancelledBy).filterNotNull()
    return userNames.map {
      getUserDetails(it!!)
    }
  }

  private fun getUserDetails(actionedBy: String): Mono<UserDetailsDto> {
    // for past visits or some exceptional circumstances actionedBy will be NOT_KNOWN
    return if (actionedBy == NOT_KNOWN) {
      Mono.just(UserDetailsDto(actionedBy, NOT_KNOWN))
    } else {
      hmppsAuthClient.getUserDetails(actionedBy)
    }
  }
  
}
