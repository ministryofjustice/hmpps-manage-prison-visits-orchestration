package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.VisitSchedulerService
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.DomainEvent
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.PrisonerReceivedInfo

const val PRISONER_RECEIVED_TYPE = "prison-offender-events.prisoner.received"

@Component(value = PRISONER_RECEIVED_TYPE)
class PrisonerReceivedNotifier(
  private val objectMapper: ObjectMapper,
  private val visitSchedulerService: VisitSchedulerService,
) : EventNotifier(objectMapper) {
  override fun processEvent(domainEvent: DomainEvent) {
    val prisonerReceivedInfo: PrisonerReceivedInfo = objectMapper.readValue(domainEvent.additionalInformation)
    LOG.debug("Enter PrisonerReceivedInfo Info:$prisonerReceivedInfo")

    visitSchedulerService.processPrisonerReceived(prisonerReceivedInfo)
  }
}
