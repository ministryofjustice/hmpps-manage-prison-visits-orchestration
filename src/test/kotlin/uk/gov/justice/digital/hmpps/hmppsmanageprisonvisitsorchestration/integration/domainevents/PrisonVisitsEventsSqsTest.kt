package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.domainevents

import org.assertj.core.api.Assertions
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilAsserted
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue

// @Disabled
class PrisonVisitsEventsSqsTest : PrisonVisitsEventsIntegrationTestBase() {

  @Test
  fun `test incentives-iep-review-inserted is processed`() {
    // Given
    val publishRequest = createDomainEventPublishRequest("incentives.iep-review.inserted")

    // When
    awsSnsClient.publish(publishRequest).get()

    // Then
    await untilCallTo { sqsPrisonVisitsEventsClient.countMessagesOnQueue(prisonVisitsEventsQueueUrl).get() } matches { it == 0 }
    await untilAsserted { verify(prisonerIncentivesInsertedNotifierSpy, times(1)).processEvent(any()) }
  }

  @Test
  fun `test incentives-iep-review-deleted is processed`() {
    // Given
    val publishRequest = createDomainEventPublishRequest("incentives.iep-review.deleted")

    // When
    awsSnsClient.publish(publishRequest).get()

    // Then
    await untilCallTo { sqsPrisonVisitsEventsClient.countMessagesOnQueue(prisonVisitsEventsQueueUrl).get() } matches { it == 0 }
    await untilAsserted { verify(prisonerIncentivesDeletedNotifierSpy, times(1)).processEvent(any()) }
  }

  @Test
  fun `test incentives-iep-review-updated is processed`() {
    // Given
    val publishRequest = createDomainEventPublishRequest("incentives.iep-review.updated")

    // When
    awsSnsClient.publish(publishRequest).get()

    // Then
    await untilCallTo { sqsPrisonVisitsEventsClient.countMessagesOnQueue(prisonVisitsEventsQueueUrl).get() } matches { it == 0 }
    await untilAsserted { verify(prisonerIncentivesUpdatedNotifierSpy, times(1)).processEvent(any()) }
  }

  @Test
  fun `test prisoner non association detail changed is processed`() {
    // Given
    val eventType = "prison-offender-events.prisoner.non-association-detail.changed"
    val domainEvent = createDomainEventJson(eventType, createAdditionalInformationJson("2023-09-01", "2023-12-03"))
    val publishRequest = createDomainEventPublishRequest("prison-offender-events.prisoner.non-association-detail.changed", domainEvent)

    visitSchedulerMockServer.stubPostNotificationNonAssociationChanged()

    // When
    awsSnsClient.publish(publishRequest).get()
    // Then
    await untilCallTo { sqsPrisonVisitsEventsClient.countMessagesOnQueue(prisonVisitsEventsQueueUrl).get() } matches { it == 0 }
    await untilAsserted { verify(nonAssociationChangedNotifier, times(1)).processEvent(any()) }
    await untilAsserted { verify(visitSchedulerClient, times(1)).processNonAssociations(any()) }
  }

  @Test
  fun `test prisoner non association detail changed is processed when no valid to date`() {
    // Given
    val eventType = "prison-offender-events.prisoner.non-association-detail.changed"
    val domainEvent = createDomainEventJson(eventType, createAdditionalInformationJson("2023-09-01"))
    val publishRequest = createDomainEventPublishRequest("prison-offender-events.prisoner.non-association-detail.changed", domainEvent)

    visitSchedulerMockServer.stubPostNotificationNonAssociationChanged()

    // When
    awsSnsClient.publish(publishRequest).get()
    // Then
    await untilCallTo { sqsPrisonVisitsEventsClient.countMessagesOnQueue(prisonVisitsEventsQueueUrl).get() } matches { it == 0 }
    await untilAsserted { verify(nonAssociationChangedNotifier, times(1)).processEvent(any()) }
    await untilAsserted { verify(visitSchedulerClient, times(1)).processNonAssociations(any()) }
  }

  @Test
  fun `test event switch set to false stops processing`() {
    // Given
    val publishRequest = createDomainEventPublishRequest("incentives.iep-review.test")

    // When
    awsSnsClient.publish(publishRequest).get()

    // Then
    await untilAsserted { verify(prisonerIncentivesUpdatedNotifierSpy, never()).processEvent(any()) }
    await untilAsserted { Assertions.assertThat(eventFeatureSwitch.isEnabled("incentives.iep-review.test")).isFalse }
  }
}
