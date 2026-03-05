package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import io.awspring.cloud.sqs.annotation.SqsListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.DomainEvent
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.EventFeatureSwitch
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.SQSMessage
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.IEventNotifier
import java.util.concurrent.CompletableFuture

const val PRISON_VISITS_QUEUE_CONFIG_KEY = "prisonvisitsevents"

@Service
class DomainEventListenerService(
  val context: ApplicationContext,
  val eventFeatureSwitch: EventFeatureSwitch,
  @param:Qualifier("objectMapper") val objectMapper: ObjectMapper,
) {

  private companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @SqsListener(PRISON_VISITS_QUEUE_CONFIG_KEY, factory = "hmppsQueueContainerFactoryProxy")
  fun onDomainEvent(
    rawMessage: String,
  ): CompletableFuture<Void> = asCompletableFuture {
    var dLQException: Exception? = null
    try {
      val sqsMessage: SQSMessage = objectMapper.readValue(rawMessage, SQSMessage::class.java)
      if (sqsMessage.messageAttributes.eventType == "Notification") {
        if (eventFeatureSwitch.isAllEventsEnabled()) {
          LOG.debug("Entered onDomainEvent")
          val domainEvent = objectMapper.readValue(sqsMessage.message, DomainEvent::class.java)
          LOG.debug("Received message: type:${domainEvent.eventType} message:${domainEvent.additionalInformation}")
          val enabled = eventFeatureSwitch.isEnabled(domainEvent.eventType)
          if (enabled) {
            try {
              getNotifier(domainEvent)?.process(domainEvent)
            } catch (e: Exception) {
              LOG.error("Failed to process know domain event type:${domainEvent.eventType}", e)
              dLQException = e
            }
          } else {
            LOG.info("Received a message I wasn't expecting Type: ${domainEvent.eventType}")
          }
        }
      }
    } catch (e: Exception) {
      LOG.error("Failed to process unknown domain event $rawMessage", e)
    }

    if (dLQException != null) {
      // Throw exception caught in processing known events to push message back on event queue
      throw dLQException
    }
  }

  fun getNotifier(domainEvent: DomainEvent): IEventNotifier? {
    if (context.containsBean(domainEvent.eventType)) {
      return context.getBean(domainEvent.eventType) as IEventNotifier
    }
    LOG.info("EventNotifier does not exist for Type:'${domainEvent.eventType}'")
    return null
  }
}

private fun asCompletableFuture(
  process: suspend () -> Unit,
): CompletableFuture<Void> = CoroutineScope(Dispatchers.Default).future {
  process()
}.thenAccept { }
