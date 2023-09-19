package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.VisitSchedulerService
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.DomainEvent
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.NonAssociationChangedInfo

const val PRISONER_NON_ASSOCIATION_DETAIL_CHANGED_TYPE = "prison-offender-events.prisoner.non-association-detail.changed"

@Component(value = PRISONER_NON_ASSOCIATION_DETAIL_CHANGED_TYPE)
class PrisonerNonAssociationChangedNotifier(
  private val objectMapper: ObjectMapper,
  private val visitSchedulerService: VisitSchedulerService,
) : EventNotifier(objectMapper) {
  override fun processEvent(domainEvent: DomainEvent) {
    val nonAssociationChangedInfo: NonAssociationChangedInfo = objectMapper.readValue(domainEvent.additionalInformation)
    LOG.debug("Enter NonAssociationChangedInfo Info:$nonAssociationChangedInfo")

    visitSchedulerService.processNonAssociations(nonAssociationChangedInfo)
  }
}
