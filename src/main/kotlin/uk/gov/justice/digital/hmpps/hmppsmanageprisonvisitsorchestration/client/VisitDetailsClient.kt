package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.hmpps.auth.UserDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.VisitHistoryDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.EventAuditDto
import java.time.Duration

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
    val visit = visitSchedulerClient.getVisitByReference(reference)

    visit?.let {
      val eventAuditList = visitSchedulerClient.getVisitHistoryByReference(reference)
      eventAuditList?.let {
        if (eventAuditList.isNotEmpty()) {
          eventAuditList.filter { it.actionedBy != null }

          val userNames = eventAuditList.filter { it.actionedBy != null }.map { it.actionedBy!! }.toSet()
          val names = getUserDetails(userNames)

          val eventAuditListWithNames = eventAuditList.map {
            EventAuditDto(
              type = it.type,
              applicationMethodType = it.applicationMethodType,
              actionedBy = names[it.actionedBy] ?: it.actionedBy,
              sessionTemplateReference = it.sessionTemplateReference,
              createTimestamp = it.createTimestamp,
            )
          }
          return VisitHistoryDetailsDto(
            eventsAudit = eventAuditListWithNames,
            visit = visit,
          )
        }
        return VisitHistoryDetailsDto(
          visit = visit,
        )
      }
    }
    return null
  }

  private fun getUserDetails(userNames: Set<String>): Map<String, String> {
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
      getUserDetails(it)
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
