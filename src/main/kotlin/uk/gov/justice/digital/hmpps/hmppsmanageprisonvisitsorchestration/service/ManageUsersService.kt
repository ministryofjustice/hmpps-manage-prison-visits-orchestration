package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.ManageUsersApiClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.manage.users.UserExtendedDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.EventAuditOrchestrationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.ActionedByDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.EventAuditDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType.PRISONER
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType.PUBLIC
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType.STAFF
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType.SYSTEM
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.VisitAllocationService.Companion.SYSTEM_USER_NAME
import java.time.Duration
import java.util.function.BiPredicate

@Service
class ManageUsersService(
  private val manageUsersApiClient: ManageUsersApiClient,
  @param:Value("\${hmpps.auth.timeout:10s}") private val apiTimeout: Duration,
) {
  companion object {
    const val NOT_KNOWN = "NOT_KNOWN"

    // this predicate determines when a call needs to be made to the manage-users API
    val userFullNameFilterPredicate: BiPredicate<UserType, String?> = BiPredicate { userType, userName -> (userType == STAFF) && !userName.isNullOrBlank() }
  }

  fun getFullNamesFromVisitHistory(eventAuditList: List<EventAuditDto>): Map<String, String> {
    val userDetails = eventAuditList.map { Pair(it.actionedBy.userType, it.actionedBy.userName) }
    return getStaffUserFullNames(userDetails)
  }

  fun getFullNamesFromEventAuditOrchestrationDetails(eventAuditList: List<EventAuditOrchestrationDto>): Map<String, String> {
    val userDetails = eventAuditList.map { Pair(it.userType, it.actionedByFullName) }
    return getStaffUserFullNames(userDetails)
  }

  fun getFullNamesFromActionedByDetails(actionedByList: List<ActionedByDto>): Map<String, String> {
    val userDetails = actionedByList.map { Pair(it.userType, it.userName) }
    return getStaffUserFullNames(userDetails)
  }

  fun getFullNameFromActionedBy(actionedByDto: ActionedByDto, usernames: Map<String, String>): String = when (actionedByDto.userType) {
    STAFF -> usernames[actionedByDto.userName] ?: NOT_KNOWN
    PUBLIC -> "GOV.UK"
    SYSTEM -> NOT_KNOWN
    PRISONER -> NOT_KNOWN
  }

  fun getFullNamesForUserIds(userIds: Set<String>): Map<String, String> {
    val nonSystemUserIds = userIds.filter { it != SYSTEM_USER_NAME }.toSet()
    val userDetails = mutableMapOf<String, String>()

    if (nonSystemUserIds.isNotEmpty()) {
      val usersByUserNames = manageUsersApiClient.getUsersByUsernames(nonSystemUserIds)

      nonSystemUserIds.forEach { userId ->
        val fullName = getFullName(usersByUserNames[userId], userId)
        userDetails.put(userId, fullName)
      }
    }

    return userDetails.toMap()
  }

  private fun getStaffUserFullNames(userDetails: List<Pair<UserType, String?>>): Map<String, String> {
    val staffUserNames = getStaffUserNames(userDetails)
    return getFullNamesForUserIds(staffUserNames.toSet())
  }

  private fun getFullName(userDetails: UserExtendedDetailsDto?, userId: String): String = userDetails?.let { it.firstName + " " + it.lastName } ?: userId

  private fun getStaffUserNames(userTypesAndNames: List<Pair<UserType, String?>>) = userTypesAndNames.asSequence().filter {
    userFullNameFilterPredicate.test(it.first, it.second)
  }.map { it.second }.toSet().filterNotNull()
}
