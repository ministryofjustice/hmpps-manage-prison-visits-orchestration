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
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.DELETE_INCENTIVES_EVENT_TYPE
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.EventNotifier
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.INSERTED_INCENTIVES_EVENT_TYPE
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.PERSON_RESTRICTION_CHANGED_TYPE
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.PRISONER_NON_ASSOCIATION_DETAIL_CHANGED_TYPE
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.PRISONER_RECEIVED_TYPE
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.PRISONER_RELEASED_TYPE
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.PRISONER_RESTRICTION_CHANGED_TYPE
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.UPDATED_INCENTIVES_EVENT_TYPE
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.VISITOR_RESTRICTION_CHANGED_TYPE
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue

private const val TEST_TYPE = "incentives.iep-review.test"

class PrisonVisitsEventsSqsTest : PrisonVisitsEventsIntegrationTestBase() {

  @Test
  fun `test incentives-iep-review-inserted is processed`() {
    // Given
    val publishRequest = createDomainEventPublishRequest(INSERTED_INCENTIVES_EVENT_TYPE)

    // When
    sendSqSMessage(publishRequest)

    // Then
    assertStandardCalls(prisonerIncentivesInsertedNotifierSpy)
  }

  @Test
  fun `test incentives-iep-review-deleted is processed`() {
    // Given
    val publishRequest = createDomainEventPublishRequest(DELETE_INCENTIVES_EVENT_TYPE)

    // When
    sendSqSMessage(publishRequest)

    // Then
    assertStandardCalls(prisonerIncentivesDeletedNotifierSpy)
  }

  @Test
  fun `test incentives-iep-review-updated is processed`() {
    // Given
    val publishRequest = createDomainEventPublishRequest(UPDATED_INCENTIVES_EVENT_TYPE)

    // When
    sendSqSMessage(publishRequest)

    // Then
    assertStandardCalls(prisonerIncentivesUpdatedNotifierSpy)
  }

  @Test
  fun `test person-restriction-changed is processed`() {
    // Given
    val domainEvent = createDomainEventJson(PERSON_RESTRICTION_CHANGED_TYPE, createAdditionalInformationJson(nomsNumber = "TEST"))
    val publishRequest = createDomainEventPublishRequest(PERSON_RESTRICTION_CHANGED_TYPE, domainEvent)

    // When
    sendSqSMessage(publishRequest)

    // Then
    assertStandardCalls(personRestrictionChangedNotifierSpy)
    await untilAsserted { verify(visitSchedulerService, times(1)).processPersonRestrictionChange(any()) }
  }

  @Test
  fun `test prisoner-received is processed`() {
    // Given
    val domainEvent = createDomainEventJson(PRISONER_RECEIVED_TYPE, createAdditionalInformationJson(nomsNumber = "TEST"))
    val publishRequest = createDomainEventPublishRequest(PRISONER_RECEIVED_TYPE, domainEvent)

    // When
    sendSqSMessage(publishRequest)

    // Then
    assertStandardCalls(prisonerReceivedNotifierSpy)
    await untilAsserted { verify(visitSchedulerService, times(1)).processPrisonerReceived(any()) }
  }

  @Test
  fun `test prisoner-released is processed`() {
    // Given
    val domainEvent = createDomainEventJson(PRISONER_RELEASED_TYPE, createAdditionalInformationJson(nomsNumber = "TEST"))
    val publishRequest = createDomainEventPublishRequest(PRISONER_RELEASED_TYPE, domainEvent)

    // When
    sendSqSMessage(publishRequest)

    // Then
    assertStandardCalls(prisonerReleasedNotifierSpy)
    await untilAsserted { verify(visitSchedulerService, times(1)).processPrisonerReleased(any()) }
  }

  @Test
  fun `test prisoner-restriction-changed is processed`() {
    // Given
    val domainEvent = createDomainEventJson(PRISONER_RESTRICTION_CHANGED_TYPE, createAdditionalInformationJson(nomsNumber = "TEST"))
    val publishRequest = createDomainEventPublishRequest(PRISONER_RESTRICTION_CHANGED_TYPE, domainEvent)

    // When
    sendSqSMessage(publishRequest)

    // Then
    assertStandardCalls(prisonerRestrictionChangedNotifierSpy)
    await untilAsserted { verify(visitSchedulerService, times(1)).processPrisonerRestrictionChange(any()) }
  }

  @Test
  fun `test visitor-restriction-changed is processed`() {
    // Given
    val domainEvent = createDomainEventJson(VISITOR_RESTRICTION_CHANGED_TYPE, createAdditionalInformationJson(personId = "TEST"))
    val publishRequest = createDomainEventPublishRequest(VISITOR_RESTRICTION_CHANGED_TYPE, domainEvent)

    // When
    sendSqSMessage(publishRequest)

    // Then
    assertStandardCalls(visitorRestrictionChangedNotifierSpy)
    await untilAsserted { verify(visitSchedulerService, times(1)).processVisitorRestrictionChange(any()) }
  }

  @Test
  fun `test prisoner non association detail changed is processed`() {
    // Given

    val domainEvent = createDomainEventJson(PRISONER_NON_ASSOCIATION_DETAIL_CHANGED_TYPE, createNonAssociationAdditionalInformationJson("2023-09-01", "2023-12-03"))
    val publishRequest = createDomainEventPublishRequest("prison-offender-events.prisoner.non-association-detail.changed", domainEvent)

    visitSchedulerMockServer.stubPostNotificationNonAssociationChanged()

    // When
    sendSqSMessage(publishRequest)

    // Then
    assertStandardCalls(nonAssociationChangedNotifier)
    await untilAsserted { verify(visitSchedulerClient, times(1)).processNonAssociations(any()) }
  }

  @Test
  fun `test prisoner non association detail changed is processed when no valid to date`() {
    // Given

    val domainEvent = createDomainEventJson(PRISONER_NON_ASSOCIATION_DETAIL_CHANGED_TYPE, createNonAssociationAdditionalInformationJson("2023-09-01"))
    val publishRequest = createDomainEventPublishRequest(PRISONER_NON_ASSOCIATION_DETAIL_CHANGED_TYPE, domainEvent)

    visitSchedulerMockServer.stubPostNotificationNonAssociationChanged()

    // When
    sendSqSMessage(publishRequest)

    // Then
    assertStandardCalls(nonAssociationChangedNotifier)
    await untilAsserted { verify(visitSchedulerClient, times(1)).processNonAssociations(any()) }
  }

  @Test
  fun `test event switch set to false stops processing`() {
    // Given
    val publishRequest = createDomainEventPublishRequest(TEST_TYPE)

    // When
    sendSqSMessage(publishRequest)

    // Then
    await untilAsserted { Assertions.assertThat(eventFeatureSwitch.isEnabled(TEST_TYPE)).isFalse }
    await untilAsserted { verify(domainEventListenerServiceSpy, never()).getNotifier(any()) }
  }

  private fun sendSqSMessage(publishRequest: PublishRequest?) {
    awsSnsClient.publish(publishRequest).get()
  }

  fun assertStandardCalls(eventNotifierSpy: EventNotifier) {
    await untilCallTo { sqsPrisonVisitsEventsClient.countMessagesOnQueue(prisonVisitsEventsQueueUrl).get() } matches { it == 0 }
    await untilAsserted { verify(eventNotifierSpy, times(1)).processEvent(any()) }
  }
}
