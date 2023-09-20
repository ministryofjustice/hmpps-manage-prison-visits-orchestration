package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.domainevents

import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

class PrisonVisitsEventsTest() : PrisonVisitsEventsIntegrationTestBase() {

  @Test
  fun `Test prisoner non association detail changed is processed correctly`() {
    // Given
    val eventType = "prison-offender-events.prisoner.non-association-detail.changed"
    val domainEvent = createDomainEventJson(eventType, createNonAssociationAdditionalInformationJson("2023-09-01", "2023-12-03"))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    visitSchedulerMockServer.stubPostNotificationNonAssociationChanged()

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    // Then
    await untilAsserted { verify(nonAssociationChangedNotifier, times(1)).processEvent(any()) }
    await untilAsserted { verify(visitSchedulerClient, times(1)).processNonAssociations(any()) }
  }
}
