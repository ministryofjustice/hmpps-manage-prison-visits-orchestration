package uk.gov.justice.digital.hmpps.orchestration.service.listeners.notifiers

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.orchestration.service.VisitSchedulerService
import uk.gov.justice.digital.hmpps.orchestration.service.listeners.events.DomainEvent

interface IEventNotifier {
  fun process(domainEvent: DomainEvent)
}

abstract class EventNotifier : IEventNotifier {

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  @Autowired
  private lateinit var visitSchedulerService: VisitSchedulerService

  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  final override fun process(domainEvent: DomainEvent) {
    LOG.debug("Entered process for ${this::class.java.name} type: ${domainEvent.eventType}")
    if (this.isProcessableEvent(domainEvent)) {
      this.processEvent(domainEvent)
    }
  }

  fun <T> getAdditionalInfo(domainEvent: DomainEvent, target: Class<T>): T = objectMapper.readValue(domainEvent.additionalInformation, target)

  fun getVisitSchedulerService() = visitSchedulerService

  abstract fun processEvent(domainEvent: DomainEvent)

  abstract fun isProcessableEvent(domainEvent: DomainEvent): Boolean
}
