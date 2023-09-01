package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.VisitSchedulerService
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.DomainEvent
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.NonAssociationChangedInfo

const val PRISONER_NON_ASSOCIATION_DETAIL_CHANGED = "prisoner.non-association-detail.changed"

@Component(value = PRISONER_NON_ASSOCIATION_DETAIL_CHANGED)
class PrisonerNonAssociationChangedNotifier(
  private val objectMapper: ObjectMapper,
  private val visitSchedulerService: VisitSchedulerService,
) : EventNotifier(objectMapper) {

  private companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  override fun processEvent(domainEvent: DomainEvent) {
    val nonAssociationChangedInfo: NonAssociationChangedInfo = objectMapper.readValue(domainEvent.additionalInformation)
    LOG.debug("NonAssociationChangedInfo Info:$nonAssociationChangedInfo")

    visitSchedulerService.processNonAssociations(nonAssociationChangedInfo)
  }
}
