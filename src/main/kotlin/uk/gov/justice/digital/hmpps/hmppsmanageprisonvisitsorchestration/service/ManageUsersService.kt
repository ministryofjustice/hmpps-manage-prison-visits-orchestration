package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.ManageUsersApiClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.manage.users.UserDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.EventAuditDto
import java.time.Duration

@Service
class ManageUsersService(
  private val manageUsersApiClient: ManageUsersApiClient,
  @Value("\${hmpps.auth.timeout:10s}") private val apiTimeout: Duration,
) {
  companion object {
    const val NOT_KNOWN = "NOT_KNOWN"
  }

  fun getFullNamesFromVisitHistory(eventAuditList: List<EventAuditDto>): Map<String, String> {
    val userNames = eventAuditList.filter { it.actionedBy != null }.map { it.actionedBy!! }.toSet()
    val monoCallsList = createUserMonoCalls(userNames)
    return executeMonoCalls(monoCallsList)
  }

  private fun executeMonoCalls(monoCallsList: List<Mono<UserDetailsDto>>): Map<String, String> {
    val results: MutableMap<String, String> = mutableMapOf()
    if (monoCallsList.size == 1) {
      val userDetails = monoCallsList[0].block(apiTimeout)
      userDetails?.let {
        userDetails.fullName?.let { results[userDetails.username] = userDetails.fullName }
      }
    }

    if (monoCallsList.size > 1) {
      var zipResults: List<UserDetailsDto> = mutableListOf()
      if (monoCallsList.size == 2) {
        val iterable = Mono.zip(monoCallsList[0], monoCallsList[1]).block(apiTimeout)
        iterable?.let {
          zipResults = iterable.toList() as List<UserDetailsDto>
        }
      }
      if (monoCallsList.size == 3) {
        val userDetailsTuples = Mono.zip(monoCallsList[0], monoCallsList[1], monoCallsList[2]).block(apiTimeout)
        userDetailsTuples?.let {
          zipResults = userDetailsTuples.toList() as List<UserDetailsDto>
        }
      }
      zipResults.forEach { userDetails -> userDetails.fullName?.let { results[userDetails.username] = userDetails.fullName } }
    }

    return results.toMap()
  }

  private fun createUserMonoCalls(
    userNames: Set<String>,
  ): List<Mono<UserDetailsDto>> {
    return userNames.map {
      getFullNamesFromVisitHistory(it)
    }
  }

  private fun getFullNamesFromVisitHistory(actionedBy: String): Mono<UserDetailsDto> {
    // for past visits or some exceptional circumstances actionedBy will be NOT_KNOWN
    return if (actionedBy == NOT_KNOWN) {
      Mono.just(UserDetailsDto(actionedBy, NOT_KNOWN))
    } else {
      manageUsersApiClient.getUserDetails(actionedBy)
    }
  }

  @Cacheable(value = ["UserFullName"], key = "#userName")
  fun getUserFullName(userName: String): String {
    return if (userName == NOT_KNOWN) {
      userName
    } else {
      manageUsersApiClient.getUserDetails(userName).block(apiTimeout)?.fullName ?: NOT_KNOWN
    }
  }
}
