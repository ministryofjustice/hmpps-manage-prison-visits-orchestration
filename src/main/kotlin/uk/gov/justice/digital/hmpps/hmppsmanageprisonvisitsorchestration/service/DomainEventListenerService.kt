package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.DomainEvent
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.EventFeatureSwitch
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.SQSMessage
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.IEventNotifier
import java.util.concurrent.CompletableFuture

const val PRISON_VISITS_QUEUE_CONFIG_KEY = "prisonvisitsevents"

@Service
@ConditionalOnExpression("\${hmpps.sqs.enabled:true}")
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
    LOG.debug("Enter onDomainEvent")
    val sqsMessage: SQSMessage = objectMapper.readValue(rawMessage)
    LOG.debug("Received message: type:${sqsMessage.type} message:${sqsMessage.message}")
    return asCompletableFuture {
      when (sqsMessage.type) {
        "Notification" -> {
          val domainEvent = objectMapper.readValue<DomainEvent>(sqsMessage.message)
          val enabled = eventFeatureSwitch.isEnabled(domainEvent.eventType)
          LOG.debug("Type: ${domainEvent.eventType} Enabled:$enabled")
          if (enabled) {
            if (context.containsBean(domainEvent.eventType)) {
              val eventNotifier = context.getBean(domainEvent.eventType) as IEventNotifier
              eventNotifier.process(domainEvent)
            } else {
              LOG.info("EventNotifier dose not exist for Type: ${domainEvent.eventType}")
            }
          }
        }
        else -> LOG.info("Received a message I wasn't expecting Type: ${sqsMessage.type}")
      }
    }
  }
}

private fun asCompletableFuture(
  process: suspend () -> Unit,
): CompletableFuture<Void> {
  return CoroutineScope(Dispatchers.Default).future {
    process()
  }.thenAccept { }
}
