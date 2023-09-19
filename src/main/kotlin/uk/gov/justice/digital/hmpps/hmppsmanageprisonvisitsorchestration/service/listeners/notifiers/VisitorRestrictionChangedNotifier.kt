package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.VisitSchedulerService
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.DomainEvent

const val VISITOR_RESTRICTION_CHANGED_TYPE = "prison-offender-events.visitor.restriction.changed"

@Component(value = VISITOR_RESTRICTION_CHANGED_TYPE)
class VisitorRestrictionChangedNotifier(
  private val objectMapper: ObjectMapper,
  private val visitSchedulerService: VisitSchedulerService,
) : EventNotifier(objectMapper) {
  override fun processEvent(domainEvent: DomainEvent) {
    // TODO
  }
}
