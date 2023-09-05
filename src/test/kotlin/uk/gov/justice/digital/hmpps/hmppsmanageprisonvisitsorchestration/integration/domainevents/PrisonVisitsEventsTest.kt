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
    val additionalInformation = "{\"nomsNumber\":\"G7747GD\",\"bookingId\":\"1171243\",\"nonAssociationNomsNumber\":\"A8713DY\",\"nonAssociationBookingId\":\"1202261\",\"effectiveDate\":\"2023-09-01\"}"
    val eventType = "prison-offender-events.prisoner.non-association-detail.changed"
    val domainEvent = "{\"eventType\":\"$eventType\",\"additionalInformation\":$additionalInformation}"

    val jsonSqsMessage = createSQSMessage(domainEvent)

    visitSchedulerMockServer.stubPostNotificationNonAssociationChanged()

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    // Then
    await untilAsserted { verify(nonAssociationChangedNotifier, times(1)).processEvent(any()) }
    await untilAsserted { verify(visitSchedulerClient, times(1)).processNonAssociations(any()) }
  }
}
