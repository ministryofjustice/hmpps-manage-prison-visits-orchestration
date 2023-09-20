package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.VisitSchedulerService
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.DomainEvent
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.VisitorRestrictionChangeInfo

const val VISITOR_RESTRICTION_CHANGED_TYPE = "prison-offender-events.visitor.restriction.changed"

@Component(value = VISITOR_RESTRICTION_CHANGED_TYPE)
class VisitorRestrictionChangedNotifier(
  private val objectMapper: ObjectMapper,
  private val visitSchedulerService: VisitSchedulerService,
) : EventNotifier(objectMapper) {
  override fun processEvent(domainEvent: DomainEvent) {
    val info: VisitorRestrictionChangeInfo = objectMapper.readValue(domainEvent.additionalInformation)
    LOG.debug("Enter VisitorRestrictionChangeInfo Info:$info")

    visitSchedulerService.processVisitorRestrictionChange(info)
  }
}
