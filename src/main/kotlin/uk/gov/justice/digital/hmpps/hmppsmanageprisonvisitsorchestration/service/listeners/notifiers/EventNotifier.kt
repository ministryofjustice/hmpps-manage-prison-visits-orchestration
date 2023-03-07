package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.DomainEvent
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.SQSMessage

interface IEventNotifier {
  fun process(sqsMessage: SQSMessage)
}

abstract class EventNotifier(
  private val objectMapper: ObjectMapper
) : IEventNotifier {

  protected fun getDomainEvent(sqsMessage: SQSMessage) {
    val domainEvent: DomainEvent = objectMapper.readValue(sqsMessage.message)
  }

  private companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  final override fun process(sqsMessage: SQSMessage) {
    LOG.debug("Entered process for ${this::class.java.name} ")
    this.processEvent(sqsMessage)
  }

  abstract fun processEvent(sqsMessage: SQSMessage)
}
