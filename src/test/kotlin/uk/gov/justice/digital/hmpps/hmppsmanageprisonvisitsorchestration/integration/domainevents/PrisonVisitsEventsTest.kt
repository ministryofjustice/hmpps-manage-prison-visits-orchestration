package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.domainevents

import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.NonAssociationChangedNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.NonAssociationChangedInfo

class PrisonVisitsEventsTest() : PrisonVisitsEventsIntegrationTestBase() {

  @Test
  fun `Test prisoner non association detail changed is processed correctly`() {
    // Given
    val nonAssociationChangedInfo = NonAssociationChangedInfo("1", "2", validFromDate = "2023-10-03", "2023-12-03")
    val nonAssociationChangedInfoDto = NonAssociationChangedNotificationDto(nonAssociationChangedInfo)
    val domainEvent = createDomainEvent("prisoner.non-association-detail.changed", objectMapper.writeValueAsString(nonAssociationChangedInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    visitSchedulerMockServer.stubPostNotificationNonAssociationChanged()

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    // Then
    await untilAsserted { verify(nonAssociationChangedNotifier, times(1)).processEvent(any()) }
    await untilAsserted { verify(visitSchedulerClient, times(1)).processNonAssociations(eq(nonAssociationChangedInfoDto)) }
  }
}
