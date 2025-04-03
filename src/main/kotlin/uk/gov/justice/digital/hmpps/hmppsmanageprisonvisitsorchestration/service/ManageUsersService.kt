package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.util.function.Tuple2
import reactor.util.function.Tuple3
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.ManageUsersApiClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.manage.users.UserDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.EventAuditOrchestrationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.ActionedByDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.EventAuditDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType.PRISONER
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType.PUBLIC
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType.STAFF
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType.SYSTEM
import java.time.Duration
import java.util.function.BiPredicate

@Service
class ManageUsersService(
  private val manageUsersApiClient: ManageUsersApiClient,
  @Value("\${hmpps.auth.timeout:10s}") private val apiTimeout: Duration,
) {
  companion object {
    const val NOT_KNOWN = "NOT_KNOWN"

    // this predicate determines when a call needs to be made to the manage-users API
    val userFullNameFilterPredicate: BiPredicate<UserType, String?> = BiPredicate { userType, userName -> (userType == STAFF) && !userName.isNullOrBlank() }
  }

  fun getFullNamesFromVisitHistory(eventAuditList: List<EventAuditDto>): Map<String, String> {
    val userDetails = eventAuditList.map { Pair(it.actionedBy.userType, it.actionedBy.userName) }
    return getUserFullNames(userDetails)
  }

  fun getFullNamesFromEventAuditOrchestrationDetails(eventAuditList: List<EventAuditOrchestrationDto>): Map<String, String> {
    val userDetails = eventAuditList.map { Pair(it.userType, it.actionedByFullName) }
    return getUserFullNames(userDetails)
  }

  private fun getUserFullNames(userDetails: List<Pair<UserType, String?>>): Map<String, String> {
    // converting to userIds as only STAFF user-ids need to be fetched
    val userIds = userDetails.asSequence().filter { userFullNameFilterPredicate.test(it.first, it.second) }
      .map { it.second }.toSet().filterNotNull().toSet()
    return getUsersFullNamesFromUserIds(userIds)
  }

  private fun getUsersFullNamesFromUserIds(userIds: Set<String>): Map<String, String> {
    val monoCallsList = createUserMonoCalls(userIds)
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
        val iterable: Tuple2<UserDetailsDto, UserDetailsDto>? = Mono.zip(
          monoCallsList[0],
          monoCallsList[1],
        ).block(apiTimeout)

        iterable?.let {
          zipResults = listOf(it.t1, it.t2) // Correct way to extract values from Tuple2
        }
      }
      if (monoCallsList.size == 3) {
        val userDetailsTuples: Tuple3<UserDetailsDto, UserDetailsDto, UserDetailsDto>? =
          Mono.zip(monoCallsList[0], monoCallsList[1], monoCallsList[2]).block(apiTimeout)

        userDetailsTuples?.let {
          zipResults = listOf(it.t1, it.t2, it.t3) // Extract values safely
        }
      }

      zipResults.forEach { userDetails -> userDetails.fullName?.let { results[userDetails.username] = userDetails.fullName } }
    }

    return results.toMap()
  }

  private fun createUserMonoCalls(
    userNames: Set<String>,
  ): List<Mono<UserDetailsDto>> = userNames.map {
    getFullNamesFromVisitHistory(it)
  }

  private fun getFullNamesFromVisitHistory(actionedBy: String): Mono<UserDetailsDto> {
    // for past visits or some exceptional circumstances actionedBy will be NOT_KNOWN
    return if (actionedBy == NOT_KNOWN) {
      Mono.just(UserDetailsDto(actionedBy, NOT_KNOWN))
    } else {
      manageUsersApiClient.getUserDetails(actionedBy)
    }
  }

  fun getUserFullName(userName: String, userNameIfNotAvailable: String = NOT_KNOWN): String = if (userName == NOT_KNOWN) {
    userName
  } else {
    manageUsersApiClient.getUserDetails(userName).block(apiTimeout)?.fullName ?: userNameIfNotAvailable
  }

  fun getFullNameFromActionedBy(actionedByDto: ActionedByDto): String = when (actionedByDto.userType) {
    STAFF -> manageUsersApiClient.getUserDetails(actionedByDto.userName!!).block(apiTimeout)?.fullName ?: NOT_KNOWN
    PUBLIC -> "GOV.UK"
    SYSTEM -> NOT_KNOWN
    PRISONER -> NOT_KNOWN
  }
}
