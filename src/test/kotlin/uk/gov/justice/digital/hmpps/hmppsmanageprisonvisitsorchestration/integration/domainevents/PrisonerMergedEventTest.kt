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
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VISIT_NOTIFICATION_PRISONER_MERGED_PATH
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.PrisonerMergedNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.PrisonerMergedInfo
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.EventNotifier
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.PRISONER_MERGED_TYPE
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue

class PrisonerMergedEventTest : PrisonVisitsEventsIntegrationTestBase() {

  @Test
  fun `when prisoner merge event is received then event is successfully processed`() {
    // Given
    val oldPrisonerNumber = "AA11CCC"
    val newPrisonerNumber = "BB22CCC"

    val prisonerMergedInfo = PrisonerMergedInfo(
      newPrisonerNumber = newPrisonerNumber,
      oldPrisonerNumber = oldPrisonerNumber,
    )
    val expectedRequestSentToVsip = PrisonerMergedNotificationDto(
      oldPrisonerNumber = oldPrisonerNumber,
      newPrisonerNumber = newPrisonerNumber,
    )

    val domainEvent = createDomainEventJson(
      PRISONER_MERGED_TYPE,
      createPrisonerMergedAdditionalInformationJson(prisonerMergedInfo),
    )
    val publishRequest = createDomainEventPublishRequest(PRISONER_MERGED_TYPE, domainEvent)

    visitSchedulerMockServer.stubPostNotification(VISIT_NOTIFICATION_PRISONER_MERGED_PATH)
    // When
    sendSqSMessage(publishRequest)

    // Then
    assertStandardCalls(prisonerMergedNotifierSpy, VISIT_NOTIFICATION_PRISONER_MERGED_PATH, expectedRequestSentToVsip)
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
