package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.VisitSchedulerService
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.DomainEvent

const val PRISONER_RELEASED_TYPE = "prison-offender-events.prisoner.released"

@Component(value = PRISONER_RELEASED_TYPE)
class PrisonerReleasedNotifier(
  private val objectMapper: ObjectMapper,
  private val visitSchedulerService: VisitSchedulerService,
) : EventNotifier(objectMapper) {
  override fun processEvent(domainEvent: DomainEvent) {
    // TODO
  }
}
