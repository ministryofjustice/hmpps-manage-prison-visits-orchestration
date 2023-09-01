package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.DomainEvent

interface IEventNotifier {
  fun process(domainEvent: DomainEvent)
}

abstract class EventNotifier(
  private val objectMapper: ObjectMapper,
) : IEventNotifier {

  private companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  final override fun process(domainEvent: DomainEvent) {
    LOG.debug("Entered process for ${this::class.java.name} ")
    this.processEvent(domainEvent)
  }

  abstract fun processEvent(domainEvent: DomainEvent)
}
