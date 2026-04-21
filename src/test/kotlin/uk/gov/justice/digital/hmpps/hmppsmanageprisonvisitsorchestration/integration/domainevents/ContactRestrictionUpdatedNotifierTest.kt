package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.domainevents

import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilAsserted
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VISIT_NOTIFICATION_CONTACT_RESTRICTION_UPSERTED_PATH
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.ContactRestrictionUpsertedNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.Identifier
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.PersonIdentifier
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.PersonReference
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.CONTACT_RESTRICTION_UPDATED_TYPE
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.EventNotifier
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue

class ContactRestrictionUpdatedNotifierTest : PrisonVisitsEventsIntegrationTestBase() {

  @Test
  fun `when valid contact global restriction updated event received then event is successfully processed`() {
    // Given
    val sentRequestToVsip = ContactRestrictionUpsertedNotificationDto(
      contactId = 103L,
      restrictionId = 101L,
    )

    val personReference = PersonReference(
      listOf(
        PersonIdentifier(Identifier.DPS_CONTACT_ID, "103"),
      ),
    )

    val domainEvent = createDomainEventJson(
      CONTACT_RESTRICTION_UPDATED_TYPE,
      "Contact restriction updated",
      createContactRestrictionAdditionalInformationJson(
        contactRestrictionId = 101L,
      ),
      objectMapper.writeValueAsString(personReference),
    )

    val publishRequest = createDomainEventPublishRequest(CONTACT_RESTRICTION_UPDATED_TYPE, domainEvent)

    visitSchedulerMockServer.stubPostNotification(VISIT_NOTIFICATION_CONTACT_RESTRICTION_UPSERTED_PATH)
    // When
    sendSqSMessage(publishRequest)

    // Then
    assertStandardCalls(contactRestrictionUpdatedNotifierSpy, VISIT_NOTIFICATION_CONTACT_RESTRICTION_UPSERTED_PATH, sentRequestToVsip)
  }

  @Test
  fun `when invalid contact global restriction updated event received then event is not processed`() {
    // Given

    // invalid person reference - contact ID missing
    val personReference = PersonReference(emptyList())

    val domainEvent = createDomainEventJson(
      CONTACT_RESTRICTION_UPDATED_TYPE,
      "Contact restriction updated",
      createContactRestrictionAdditionalInformationJson(
        contactRestrictionId = 101L,
      ),
      objectMapper.writeValueAsString(personReference),
    )

    val publishRequest = createDomainEventPublishRequest(CONTACT_RESTRICTION_UPDATED_TYPE, domainEvent)

    visitSchedulerMockServer.stubPostNotification(VISIT_NOTIFICATION_CONTACT_RESTRICTION_UPSERTED_PATH)
    // When
    sendSqSMessage(publishRequest)

    // Then
    awaitVisitsDlqHasOneMessage()
    verify(visitSchedulerClient, times(0)).processContactRestrictionUpserted(any())
  }

  private fun sendSqSMessage(publishRequest: PublishRequest?) {
    awsSnsClient.publish(publishRequest).get()
  }

  private fun assertStandardCalls(eventNotifierSpy: EventNotifier, notificationEndPoint: String? = null, expectedRequestBody: Any? = null) {
    await untilCallTo { sqsPrisonVisitsEventsClient.countMessagesOnQueue(prisonVisitsEventsQueueUrl).get() } matches { it == 0 }
    await untilAsserted { verify(eventNotifierSpy, times(1)).processEvent(any()) }
    notificationEndPoint?.let {
      await untilAsserted { visitSchedulerMockServer.verifyPost(notificationEndPoint, expectedRequestBody) }
    }
  }
}
