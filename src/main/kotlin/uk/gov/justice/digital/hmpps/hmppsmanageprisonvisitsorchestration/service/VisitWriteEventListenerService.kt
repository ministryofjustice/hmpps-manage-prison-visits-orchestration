package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.VisitWriteEvent
import java.util.concurrent.CompletableFuture

const val PRISON_VISITS_WRITES_QUEUE_CONFIG_KEY = "prisonvisitswriteevents"

@Service
class VisitWriteEventListenerService(
  val objectMapper: ObjectMapper,
) {
  @SqsListener(PRISON_VISITS_WRITES_QUEUE_CONFIG_KEY, factory = "hmppsQueueContainerFactoryProxy")
  fun onEventReceived(
    rawMessage: String,
  ): CompletableFuture<Void> = asCompletableFuture {
    val sqsMessage = objectMapper.readValue(rawMessage, VisitWriteEvent::class.java)
    when(sqsMessage.eventType) {
      "VisitCreated" -> {}
      "VisitUpdated" -> {}
      "VisitCancelled" -> {}
      else -> throw Exception("Cannot process event of type ${sqsMessage.eventType}")
    }
  }
}

private fun asCompletableFuture(
  process: suspend () -> Unit,
): CompletableFuture<Void> = CoroutineScope(Dispatchers.Default).future {
  process()
}.thenAccept { }
