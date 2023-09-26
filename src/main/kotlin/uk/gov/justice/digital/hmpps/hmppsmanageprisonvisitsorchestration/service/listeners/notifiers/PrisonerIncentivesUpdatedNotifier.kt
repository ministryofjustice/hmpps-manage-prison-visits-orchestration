package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.DomainEvent

const val UPDATED_INCENTIVES_EVENT_TYPE = "incentives.iep-review.updated"

@Component(value = UPDATED_INCENTIVES_EVENT_TYPE)
class PrisonerIncentivesUpdatedNotifier(private val objectMapper: ObjectMapper) : EventNotifier(objectMapper) {

  private companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  override fun processEvent(domainEvent: DomainEvent) {
  }
}
