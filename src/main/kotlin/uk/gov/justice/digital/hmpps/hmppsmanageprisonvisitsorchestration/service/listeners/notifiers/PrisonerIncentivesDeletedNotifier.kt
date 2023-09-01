package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.DomainEvent

const val DELETE_INCENTIVES_EVENT_TYPE = "incentives.iep-review.deleted"

@Component(value = DELETE_INCENTIVES_EVENT_TYPE)
class PrisonerIncentivesDeletedNotifier(private val objectMapper: ObjectMapper) : EventNotifier(objectMapper) {

  override fun processEvent(domainEvent: DomainEvent) {
  }
}
