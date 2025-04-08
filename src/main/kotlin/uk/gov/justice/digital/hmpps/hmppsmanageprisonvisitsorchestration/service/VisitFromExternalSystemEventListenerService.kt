package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitSchedulerClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.CancelVisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.ApplicationMethodType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.VisitFromExternalSystemEvent
import java.util.concurrent.CompletableFuture

const val PRISON_VISITS_WRITES_QUEUE_CONFIG_KEY = "prisonvisitswriteevents"

@Service
class VisitFromExternalSystemEventListenerService(
  private val objectMapper: ObjectMapper,
  private val visitSchedulerClient: VisitSchedulerClient,
) {
  private companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @SqsListener(PRISON_VISITS_WRITES_QUEUE_CONFIG_KEY, factory = "hmppsQueueContainerFactoryProxy")
  fun onEventReceived(
    rawMessage: String,
  ): CompletableFuture<Void> = asCompletableFuture {
    try {
      LOG.debug("Received visit write event: $rawMessage")
      val sqsMessage = objectMapper.readValue(rawMessage, VisitFromExternalSystemEvent::class.java)
      when (sqsMessage.eventType) {
        "VisitCreated" -> {
          val createVisitFromExternalSystemDto = sqsMessage.toCreateVisitFromExternalSystemDto()
          visitSchedulerClient.createVisitFromExternalSystem(createVisitFromExternalSystemDto)
        }
        "VisitUpdated" -> {
          val updateVisitFromExternalSystemDto = sqsMessage.toUpdateVisitFromExternalSystemDto()
          visitSchedulerClient.updateVisitFromExternalSystem(updateVisitFromExternalSystemDto)
        }
        "VisitCancelled" -> {
          val cancelVisitFromExternalSystemDto = sqsMessage.toCancelVisitFromExternalSystemDto()
          val cancelVisitDto = CancelVisitDto(
            cancelOutcome = cancelVisitFromExternalSystemDto.cancelOutcome,
            applicationMethodType = if (cancelVisitFromExternalSystemDto.userType == UserType.PRISONER) ApplicationMethodType.BY_PRISONER else ApplicationMethodType.NOT_KNOWN,
            actionedBy = cancelVisitFromExternalSystemDto.actionedBy,
            userType = cancelVisitFromExternalSystemDto.userType,
          )
          visitSchedulerClient.cancelVisit(cancelVisitFromExternalSystemDto.visitReference, cancelVisitDto)
        }
        else -> throw Exception("Cannot process event of type ${sqsMessage.eventType}")
      }
    } catch (e: Exception) {
      LOG.error("Error processing visit write event", e)
      throw e
    }
  }
}

private fun asCompletableFuture(
  process: suspend () -> Unit,
): CompletableFuture<Void> = CoroutineScope(Dispatchers.Default).future {
  process()
}.thenAccept { }
