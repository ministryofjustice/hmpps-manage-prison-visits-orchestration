package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.VisitSchedulerService
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.DomainEvent
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.PrisonerReleasedInfo

const val PRISONER_RELEASED_TYPE = "prison-offender-events.prisoner.released"

@Component(value = PRISONER_RELEASED_TYPE)
class PrisonerReleasedNotifier(
  private val objectMapper: ObjectMapper,
  private val visitSchedulerService: VisitSchedulerService,
) : EventNotifier(objectMapper) {
  override fun processEvent(domainEvent: DomainEvent) {
    val prisonerReleasedInfo: PrisonerReleasedInfo = objectMapper.readValue(domainEvent.additionalInformation)
    LOG.debug("Enter PrisonerReleasedNotificationDto Info:$prisonerReleasedInfo")

    visitSchedulerService.processPrisonerReleased(prisonerReleasedInfo)
  }
}
