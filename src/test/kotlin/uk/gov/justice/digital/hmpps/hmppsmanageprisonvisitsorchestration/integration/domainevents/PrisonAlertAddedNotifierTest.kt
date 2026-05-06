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
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VISIT_NOTIFICATION_PRISONER_ALERT_ADDED_PATH
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.alerts.api.enums.PrisonerSupportedAlertCodeType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.PrisonerAlertAddedNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.Identifier
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.PersonIdentifier
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.PersonReference
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.EventNotifier
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.PRISONER_ALERT_ADDED
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue

class PrisonAlertAddedNotifierTest : PrisonVisitsEventsIntegrationTestBase() {

  @Test
  fun `when a supported alert code is added then event is successfully processed`() {
    // Given
    val prisonerNumber = "AB123456C"
    val alertCode = PrisonerSupportedAlertCodeType.C1.name
    val alertDescription = "Prisoner alert added"
    val alertUUID = "12345678-1234-1234-1234-123456789012"

    val sentRequestToVsip = PrisonerAlertAddedNotificationDto(
      prisonerNumber = prisonerNumber,
      alertCode = alertCode,
      alertUUID = alertUUID,
      description = alertDescription,
    )

    val personReference = PersonReference(
      listOf(
        PersonIdentifier(Identifier.NOMS, prisonerNumber),
      ),
    )

    val domainEvent = createDomainEventJson(
      eventType = PRISONER_ALERT_ADDED,
      description = alertDescription,
      additionalInformation = createAddAlertAdditionalInformationJson(
        alertCode = alertCode,
        alertUUID = alertUUID,
      ),
      personReference = objectMapper.writeValueAsString(personReference),
    )

    val publishRequest = createDomainEventPublishRequest(PRISONER_ALERT_ADDED, domainEvent)

    visitSchedulerMockServer.stubPostNotification(VISIT_NOTIFICATION_PRISONER_ALERT_ADDED_PATH)
    // When
    sendSqSMessage(publishRequest)

    // Then
    assertStandardCalls(prisonerAlertAddedNotifierSpy, VISIT_NOTIFICATION_PRISONER_ALERT_ADDED_PATH, sentRequestToVsip)
  }

  @Test
  fun `when a non supported alert code is added then event is not processed`() {
    // Given
    val prisonerNumber = "AB123456C"
    // alert code not in list of supported codes
    val alertCode = "NOT_SUPPORTED"
    val alertDescription = "Prisoner alert added"
    val alertUUID = "12345678-1234-1234-1234-123456789012"

    // Given
    val personReference = PersonReference(
      listOf(
        PersonIdentifier(Identifier.NOMS, prisonerNumber),
      ),
    )

    val domainEvent = createDomainEventJson(
      eventType = PRISONER_ALERT_ADDED,
      description = alertDescription,
      additionalInformation = createAddAlertAdditionalInformationJson(
        alertCode = alertCode,
        alertUUID = alertUUID,
      ),
      personReference = objectMapper.writeValueAsString(personReference),
    )

    val publishRequest = createDomainEventPublishRequest(PRISONER_ALERT_ADDED, domainEvent)

    visitSchedulerMockServer.stubPostNotification(VISIT_NOTIFICATION_PRISONER_ALERT_ADDED_PATH)
    // When
    sendSqSMessage(publishRequest)

    // Then
    verify(prisonerAlertAddedNotifierSpy, times(0)).processEvent(any())
  }

  @Test
  fun `when prisoner number identifier is not NOMS then event is not processed`() {
    // Given
    val prisonerNumber = "AB123456C"
    val alertCode = PrisonerSupportedAlertCodeType.SSHO.name
    val alertDescription = "Prisoner alert added"
    val alertUUID = "12345678-1234-1234-1234-123456789012"

    // Person reference identifier is not NOMS
    val personReference = PersonReference(
      listOf(
        PersonIdentifier(Identifier.DPS_CONTACT_ID, prisonerNumber),
      ),
    )

    val domainEvent = createDomainEventJson(
      eventType = PRISONER_ALERT_ADDED,
      description = alertDescription,
      additionalInformation = createAddAlertAdditionalInformationJson(
        alertCode = alertCode,
        alertUUID = alertUUID,
      ),
      personReference = objectMapper.writeValueAsString(personReference),
    )

    val publishRequest = createDomainEventPublishRequest(PRISONER_ALERT_ADDED, domainEvent)

    visitSchedulerMockServer.stubPostNotification(VISIT_NOTIFICATION_PRISONER_ALERT_ADDED_PATH)
    // When
    sendSqSMessage(publishRequest)

    // Then
    verify(prisonerAlertAddedNotifierSpy, times(0)).processEvent(any())
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
