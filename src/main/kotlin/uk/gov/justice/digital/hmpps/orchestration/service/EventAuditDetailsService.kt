package uk.gov.justice.digital.hmpps.orchestration.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.orchestration.dto.orchestration.EventAuditOrchestrationDto
import uk.gov.justice.digital.hmpps.orchestration.dto.visit.scheduler.EventAuditDto

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
