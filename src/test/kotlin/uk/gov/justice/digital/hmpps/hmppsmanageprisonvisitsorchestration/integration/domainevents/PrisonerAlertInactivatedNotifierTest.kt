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
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VISIT_NOTIFICATION_PRISONER_ALERT_DELETED_OR_INACTIVATED_PATH
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.alerts.api.enums.PrisonerSupportedAlertCodeType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.PrisonerAlertNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.Identifier
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.PersonIdentifier
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.PersonReference
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.PRISONER_ALERT_INACTIVATED
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue

class PrisonerAlertInactivatedNotifierTest : PrisonVisitsEventsIntegrationTestBase() {

  @Test
  fun `when a supported alert code is inactivated then event is successfully processed`() {
    // Given
    val prisonerNumber = "AB123456C"
    val alertCode = PrisonerSupportedAlertCodeType.C1.name
    val alertDescription = "Prisoner alert inactivated"
    val alertUuid = "12345678-1234-1234-1234-123456789012"

    val sentRequestToVsip = PrisonerAlertNotificationDto(
      prisonerNumber = prisonerNumber,
      alertCode = alertCode,
      alertUuid = alertUuid,
      description = alertDescription,
    )

    val personReference = PersonReference(
      listOf(
        PersonIdentifier(Identifier.NOMS, prisonerNumber),
      ),
    )

    val domainEvent = createDomainEventJson(
      eventType = PRISONER_ALERT_INACTIVATED,
      description = alertDescription,
      additionalInformation = createAddAlertAdditionalInformationJson(
        alertCode = alertCode,
        alertUuid = alertUuid,
      ),
      personReference = objectMapper.writeValueAsString(personReference),
    )

    val publishRequest = createDomainEventPublishRequest(PRISONER_ALERT_INACTIVATED, domainEvent)

    visitSchedulerMockServer.stubPostNotification(VISIT_NOTIFICATION_PRISONER_ALERT_DELETED_OR_INACTIVATED_PATH)
    // When
    sendSqSMessage(publishRequest)

    // Then
    assertStandardCalls(sentRequestToVsip)
  }

  @Test
  fun `when a non supported alert code is inactivated then event is not processed`() {
    // Given
    val prisonerNumber = "AB123456C"
    // alert code not in list of supported codes
    val alertCode = "NOT_SUPPORTED"
    val alertDescription = "Prisoner alert inactivated"
    val alertUuid = "12345678-1234-1234-1234-123456789012"

    // Given
    val personReference = PersonReference(
      listOf(
        PersonIdentifier(Identifier.NOMS, prisonerNumber),
      ),
    )

    val domainEvent = createDomainEventJson(
      eventType = PRISONER_ALERT_INACTIVATED,
      description = alertDescription,
      additionalInformation = createAddAlertAdditionalInformationJson(
        alertCode = alertCode,
        alertUuid = alertUuid,
      ),
      personReference = objectMapper.writeValueAsString(personReference),
    )

    val publishRequest = createDomainEventPublishRequest(PRISONER_ALERT_INACTIVATED, domainEvent)

    visitSchedulerMockServer.stubPostNotification(VISIT_NOTIFICATION_PRISONER_ALERT_DELETED_OR_INACTIVATED_PATH)
    // When
    sendSqSMessage(publishRequest)

    // Then
    assertEventNotProcessed()
    verify(visitSchedulerClient, times(0)).processPrisonerAlertInactivatedOrDeleted(sendDto = any())
  }

  @Test
  fun `when prisoner number identifier is not NOMS then event is not processed`() {
    // Given
    val prisonerNumber = "AB123456C"
    val alertCode = PrisonerSupportedAlertCodeType.CPRC.name
    val alertDescription = "Prisoner alert inactivated"
    val alertUuid = "12345678-1234-1234-1234-123456789012"

    // Person reference identifier is not NOMS
    val personReference = PersonReference(
      listOf(
        PersonIdentifier(Identifier.DPS_CONTACT_ID, prisonerNumber),
      ),
    )

    val domainEvent = createDomainEventJson(
      eventType = PRISONER_ALERT_INACTIVATED,
      description = alertDescription,
      additionalInformation = createAddAlertAdditionalInformationJson(
        alertCode = alertCode,
        alertUuid = alertUuid,
      ),
      personReference = objectMapper.writeValueAsString(personReference),
    )

    val publishRequest = createDomainEventPublishRequest(PRISONER_ALERT_INACTIVATED, domainEvent)

    visitSchedulerMockServer.stubPostNotification(VISIT_NOTIFICATION_PRISONER_ALERT_DELETED_OR_INACTIVATED_PATH)
    // When
    sendSqSMessage(publishRequest)

    // Then
    assertEventNotProcessed()
    verify(visitSchedulerClient, times(0)).processPrisonerAlertInactivatedOrDeleted(sendDto = any())
  }

  private fun sendSqSMessage(publishRequest: PublishRequest?) {
    awsSnsClient.publish(publishRequest).get()
  }

  private fun assertStandardCalls(expectedRequestBody: Any? = null) {
    await untilCallTo { sqsPrisonVisitsEventsClient.countMessagesOnQueue(prisonVisitsEventsQueueUrl).get() } matches { it == 0 }
    await untilAsserted { verify(prisonerAlertInactivatedNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { visitSchedulerMockServer.verifyPost(VISIT_NOTIFICATION_PRISONER_ALERT_DELETED_OR_INACTIVATED_PATH, expectedRequestBody) }
  }

  private fun assertEventNotProcessed() {
    await untilCallTo { sqsPrisonVisitsEventsClient.countMessagesOnQueue(prisonVisitsEventsQueueUrl).get() } matches { it == 0 }
    verify(prisonerAlertInactivatedNotifierSpy, times(0)).processEvent(any())
  }
}
