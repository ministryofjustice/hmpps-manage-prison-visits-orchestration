package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.EventAuditOrchestrationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.EventAuditDto

@Service
class EventAuditDetailsService(
  private val manageUsersService: ManageUsersService,
) {
  companion object {
  }

  fun getEventAuditDetailsWithActionedByUserNames(eventAuditList: List<EventAuditDto>): List<EventAuditOrchestrationDto> {
    val names = manageUsersService.getFullNamesFromVisitHistory(eventAuditList)
    return eventAuditList.map {
      EventAuditOrchestrationDto(
        eventAuditDto = it,
        actionedByFullName = names[it.actionedBy.userName] ?: it.actionedBy.userName,
      )
    }
  }
}
