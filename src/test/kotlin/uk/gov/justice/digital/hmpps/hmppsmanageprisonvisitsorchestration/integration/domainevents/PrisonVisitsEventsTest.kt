package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.domainevents

import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test

import org.mockito.kotlin.any
import org.mockito.kotlin.verify

import org.mockito.kotlin.times
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.DomainEvent
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue

class PrisonVisitsEventsTest : PrisonVisitsEventsIntegrationTestBase() {

  @Test
  fun `Test incentives-iep-review-inserted is processed`() {

    // Given
    val publishRequest = createDomainEventPublishRequest("incentives.iep-review.inserted")

    // When
    awsSnsClient.publish(publishRequest).get()

    // Then
    await untilCallTo { sqsPrisonVisitsEventsClient.countMessagesOnQueue(prisonVisitsEventsQueueUrl).get() } matches { it == 0 }
    verify(prisonerIncentivesInsertedNotifierSpy, times(1)).processEvent(any())
  }

  @Test
  fun `Test incentives-iep-review-deleted is processed`() {

    // Given
    val publishRequest = createDomainEventPublishRequest("incentives.iep-review.deleted")

    // When
    awsSnsClient.publish(publishRequest).get()

    // Then
    await untilCallTo { sqsPrisonVisitsEventsClient.countMessagesOnQueue(prisonVisitsEventsQueueUrl).get() } matches { it == 0 }
    verify(prisonerIncentivesDeletedNotifierSpy, times(1)).processEvent(any())
  }

  @Test
  fun `Test incentives-iep-review-updated is processed`() {

    // Given
    val publishRequest = createDomainEventPublishRequest("incentives.iep-review.updated")

    // When
    awsSnsClient.publish(publishRequest).get()

    // Then
    await untilCallTo { sqsPrisonVisitsEventsClient.countMessagesOnQueue(prisonVisitsEventsQueueUrl).get() } matches { it == 0 }
    verify(prisonerIncentivesUpdatedNotifierSpy, times(1)).processEvent(any())
  }

  private fun createDomainEventPublishRequest(eventType: String): PublishRequest? {
    val domainEvent = DomainEvent(eventType = eventType)
    val domainEventJson = objectMapper.writeValueAsString(domainEvent)
    val messageAttributes = mapOf(
      "eventType" to MessageAttributeValue.builder().dataType("String").stringValue(domainEvent.eventType).build(),
    )

    return PublishRequest.builder()
      .topicArn(topicArn)
      .message(domainEventJson)
      .messageAttributes(messageAttributes).build()
  }
}
