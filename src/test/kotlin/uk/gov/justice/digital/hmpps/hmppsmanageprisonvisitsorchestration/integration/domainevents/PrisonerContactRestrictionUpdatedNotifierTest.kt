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
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VISIT_NOTIFICATION_PRISONER_CONTACT_RESTRICTION_UPSERTED_PATH
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.PrisonerContactRestrictionUpsertedNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.Identifier
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.PersonIdentifier
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.PersonReference
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.EventNotifier
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.PRISONER_CONTACT_RESTRICTION_UPDATED_TYPE
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue

class PrisonerContactRestrictionUpdatedNotifierTest : PrisonVisitsEventsIntegrationTestBase() {

  @Test
  fun `when valid contact restriction updated event received then event is successfully processed`() {
    // Given
    val sentRequestToVsip = PrisonerContactRestrictionUpsertedNotificationDto(
      prisonerNumber = "Test",
      contactId = 103L,
      prisonerContactId = 102L,
      restrictionId = 101L,
    )

    val personReference = PersonReference(
      listOf(
        PersonIdentifier(Identifier.NOMS, "Test"),
        PersonIdentifier(Identifier.DPS_CONTACT_ID, "103"),
      ),
    )

    val domainEvent = createDomainEventJson(
      PRISONER_CONTACT_RESTRICTION_UPDATED_TYPE,
      "Contact restriction updated",
      createPrisonerContactRestrictionAdditionalInformationJson(
        prisonerContactRestrictionId = 101L,
        prisonerContactId = 102L,
      ),
      objectMapper.writeValueAsString(personReference),
    )

    val publishRequest = createDomainEventPublishRequest(PRISONER_CONTACT_RESTRICTION_UPDATED_TYPE, domainEvent)

    visitSchedulerMockServer.stubPostNotification(VISIT_NOTIFICATION_PRISONER_CONTACT_RESTRICTION_UPSERTED_PATH)
    // When
    sendSqSMessage(publishRequest)

    // Then
    assertStandardCalls(prisonerContactRestrictionUpdatedNotifierSpy, VISIT_NOTIFICATION_PRISONER_CONTACT_RESTRICTION_UPSERTED_PATH, sentRequestToVsip)
  }

  @Test
  fun `when invalid contact restriction updated event received then event is not processed`() {
    // Given

    // invalid person reference - prisoner number and contact ID missing
    val personReference = PersonReference(emptyList())

    val domainEvent = createDomainEventJson(
      PRISONER_CONTACT_RESTRICTION_UPDATED_TYPE,
      "Contact restriction updated",
      createPrisonerContactRestrictionAdditionalInformationJson(
        prisonerContactRestrictionId = 101L,
        prisonerContactId = 102L,
      ),
      objectMapper.writeValueAsString(personReference),
    )

    val publishRequest = createDomainEventPublishRequest(PRISONER_CONTACT_RESTRICTION_UPDATED_TYPE, domainEvent)

    visitSchedulerMockServer.stubPostNotification(VISIT_NOTIFICATION_PRISONER_CONTACT_RESTRICTION_UPSERTED_PATH)
    // When
    sendSqSMessage(publishRequest)

    // Then
    awaitVisitsDlqHasOneMessage()
    verify(visitSchedulerClient, times(0)).processPrisonerContactRestrictionUpserted(any())
  }

  private fun sendSqSMessage(publishRequest: PublishRequest?) {
    awsSnsClient.publish(publishRequest).get()
  }

  fun assertStandardCalls(eventNotifierSpy: EventNotifier, notificationEndPoint: String? = null, any: Any? = null) {
    await untilCallTo { sqsPrisonVisitsEventsClient.countMessagesOnQueue(prisonVisitsEventsQueueUrl).get() } matches { it == 0 }
    await untilAsserted { verify(eventNotifierSpy, times(1)).processEvent(any()) }
    notificationEndPoint?.let {
      await untilAsserted { visitSchedulerMockServer.verifyPost(notificationEndPoint, any) }
    }
  }
}
