package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.DomainEvent
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.EventFeatureSwitch
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.SQSMessage
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.IEventNotifier
import java.util.concurrent.CompletableFuture

const val PRISON_VISITS_QUEUE_CONFIG_KEY = "prisonvisitsevents"

@Service
class DomainEventListenerService(
  val context: ApplicationContext,
  val objectMapper: ObjectMapper,
  val eventFeatureSwitch: EventFeatureSwitch,
) {

  private companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @SqsListener(PRISON_VISITS_QUEUE_CONFIG_KEY, factory = "hmppsQueueContainerFactoryProxy")
  fun onDomainEvent(
    rawMessage: String,
  ): CompletableFuture<Void> {
    return asCompletableFuture {
      val sqsMessage: SQSMessage = objectMapper.readValue(rawMessage)
      if (sqsMessage.type == "Notification") {
        LOG.debug("Enter onDomainEvent")
        val domainEvent = objectMapper.readValue<DomainEvent>(sqsMessage.message)
        if (eventFeatureSwitch.isAllEventsEnabled()) {
          try {
            LOG.debug("Received message: type:${domainEvent.eventType} message:${domainEvent.additionalInformation}")
            val enabled = eventFeatureSwitch.isEnabled(domainEvent.eventType)
            if (enabled) {
              getNotifier(domainEvent)?.process(domainEvent)
            } else {
              LOG.info("Received a message I wasn't expecting Type: ${domainEvent.eventType}")
            }
          } catch (e: Exception) {
            LOG.error("Fail to process domain event", e)
          }
        } else {
          LOG.info("Enter onDomainEvent: disabled via property hmpps.sqs.enabled=false ${domainEvent.eventType}")
        }
      }
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
): CompletableFuture<Void> {
  return CoroutineScope(Dispatchers.Default).future {
    process()
  }.thenAccept { }
}
