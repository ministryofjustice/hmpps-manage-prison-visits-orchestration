package uk.gov.justice.digital.hmpps.visits.orchestration.integration.domainevents

import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilAsserted
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.visits.orchestration.client.VISIT_NOTIFICATION_PRISONER_RELEASED_CHANGE_PATH
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.enums.PrisonerReleaseReasonType
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.enums.UserType
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.visitnotification.PrisonerReleasedNotificationDto
import uk.gov.justice.digital.hmpps.visits.orchestration.service.listeners.notifiers.EventNotifier
import uk.gov.justice.digital.hmpps.visits.orchestration.service.listeners.notifiers.PRISONER_RELEASED_TYPE
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue

class PrisonerReleasedEventTest : PrisonVisitsEventsIntegrationTestBase() {

  @Test
  fun `test prisoner-released is processed`() {
    // Given
    val sentRequestToVsip = PrisonerReleasedNotificationDto(
      prisonerNumber = "TEST",
      prisonCode = "BRI",
      reasonType = PrisonerReleaseReasonType.RELEASED,
    )

    val domainEvent = createDomainEventJson(
      PRISONER_RELEASED_TYPE,
      createAdditionalInformationJson(
        nomsNumber = "TEST",
        prisonCode = "BRI",
        reason = sentRequestToVsip.reasonType.toString(),
      ),
    )
    val publishRequest = createDomainEventPublishRequest(PRISONER_RELEASED_TYPE, domainEvent)

    visitSchedulerMockServer.stubPostNotification(VISIT_NOTIFICATION_PRISONER_RELEASED_CHANGE_PATH)
    visitSchedulerMockServer.stubGetSupportedPrisons(UserType.STAFF, listOf("BRI", "HEI", "ABC"))
    // When
    sendSqSMessage(publishRequest)

    // Then
    assertStandardCalls(prisonerReleasedNotifierSpy, VISIT_NOTIFICATION_PRISONER_RELEASED_CHANGE_PATH, sentRequestToVsip)
    await untilAsserted { verify(visitSchedulerClient, times(1)).getSupportedPrisons(UserType.STAFF) }
  }

  @Test
  fun `test prisoner-released is not processed when prison not supported on VSIP`() {
    // Given

    val domainEvent = createDomainEventJson(
      PRISONER_RELEASED_TYPE,
      createAdditionalInformationJson(
        nomsNumber = "TEST",
        prisonCode = "BRI",
        reason = "reason",
      ),
    )
    val publishRequest = createDomainEventPublishRequest(PRISONER_RELEASED_TYPE, domainEvent)

    visitSchedulerMockServer.stubPostNotification(VISIT_NOTIFICATION_PRISONER_RELEASED_CHANGE_PATH)

    // BRI - not supported on VSIP
    visitSchedulerMockServer.stubGetSupportedPrisons(UserType.STAFF, listOf("HEI", "ABC"))
    // When
    sendSqSMessage(publishRequest)

    // Then

    await untilCallTo { sqsPrisonVisitsEventsClient.countMessagesOnQueue(prisonVisitsEventsQueueUrl).get() } matches { it == 0 }
    verify(visitSchedulerClient, times(1)).getSupportedPrisons(UserType.STAFF)
    verify(visitSchedulerClient, times(0)).processPrisonerReleased(any())
  }

  @Test
  fun `test prisoner-released is not processed when getSupportedPrisons returns 404`() {
    // Given
    val domainEvent = createDomainEventJson(
      PRISONER_RELEASED_TYPE,
      createAdditionalInformationJson(
        nomsNumber = "TEST",
        prisonCode = "BRI",
        reason = "reason",
      ),
    )
    val publishRequest = createDomainEventPublishRequest(PRISONER_RELEASED_TYPE, domainEvent)

    visitSchedulerMockServer.stubPostNotification(VISIT_NOTIFICATION_PRISONER_RELEASED_CHANGE_PATH)

    // 404 returned from visit-scheduler
    visitSchedulerMockServer.stubGetSupportedPrisons(UserType.STAFF, null)
    // When
    sendSqSMessage(publishRequest)

    // Then

    await untilCallTo { sqsPrisonVisitsEventsClient.countMessagesOnQueue(prisonVisitsEventsQueueUrl).get() } matches { it == 0 }
    verify(visitSchedulerClient, times(1)).getSupportedPrisons(UserType.STAFF)
    verify(visitSchedulerClient, times(0)).processPrisonerReleased(any())
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
