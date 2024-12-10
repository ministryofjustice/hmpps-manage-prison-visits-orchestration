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
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VISIT_NOTIFICATION_NON_ASSOCIATION_CHANGE_PATH
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VISIT_NOTIFICATION_PERSON_RESTRICTION_UPSERTED_PATH
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VISIT_NOTIFICATION_PRISONER_ALERTS_UPDATED_PATH
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VISIT_NOTIFICATION_PRISONER_RECEIVED_CHANGE_PATH
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VISIT_NOTIFICATION_PRISONER_RELEASED_CHANGE_PATH
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VISIT_NOTIFICATION_VISITOR_APPROVED_PATH
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VISIT_NOTIFICATION_VISITOR_RESTRICTION_UPSERTED_PATH
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VISIT_NOTIFICATION_VISITOR_UNAPPROVED_PATH
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.alerts.api.AlertCodeSummaryDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.alerts.api.AlertResponseDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.PrisonerReceivedReasonType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.PrisonerReleaseReasonType.RELEASED
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.NonAssociationChangedNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.PersonRestrictionUpsertedNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.PrisonerAlertsAddedNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.PrisonerReceivedNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.PrisonerReleasedNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.VisitorApprovedUnapprovedNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.VisitorRestrictionUpsertedNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.PrisonerReceivedInfo
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.DELETE_INCENTIVES_EVENT_TYPE
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.EventNotifier
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.INSERTED_INCENTIVES_EVENT_TYPE
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.NonAssociationDomainEventType.NON_ASSOCIATION_CREATED
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.PERSON_RESTRICTION_UPSERTED_TYPE
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.PRISONER_ALERTS_UPDATED
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.PRISONER_NON_ASSOCIATION_DETAIL_CREATED_TYPE
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.PRISONER_RECEIVED_TYPE
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.PRISONER_RELEASED_TYPE
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.UPDATED_INCENTIVES_EVENT_TYPE
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.VISITOR_APPROVED_EVENT_TYPE
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.VISITOR_RESTRICTION_UPSERTED_TYPE
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.VISITOR_UNAPPROVED_EVENT_TYPE
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue
import java.time.LocalDate

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
  fun `test person-restriction-upserted is processed`() {
    // Given
    val sentRequestToVsip = PersonRestrictionUpsertedNotificationDto(
      prisonerNumber = "TEST",
      visitorId = "12345",
      validFromDate = LocalDate.parse("2023-09-20"),
      restrictionType = "BAN",
    )

    val domainEvent =
      createDomainEventJson(
        PERSON_RESTRICTION_UPSERTED_TYPE,
        createPersonRestrictionAdditionalInformationJson(
          nomsNumber = "TEST",
          visitorId = "12345",
          effectiveDate = "2023-09-20",
          restrictionType = "BAN",
        ),
      )

    val publishRequest = createDomainEventPublishRequest(PERSON_RESTRICTION_UPSERTED_TYPE, domainEvent)

    visitSchedulerMockServer.stubPostNotification(VISIT_NOTIFICATION_PERSON_RESTRICTION_UPSERTED_PATH)

    // When
    sendSqSMessage(publishRequest)

    // Then
    assertStandardCalls(personRestrictionUpsertedNotifierSpy, VISIT_NOTIFICATION_PERSON_RESTRICTION_UPSERTED_PATH, sentRequestToVsip)
    await untilAsserted { verify(visitSchedulerService, times(1)).processPersonRestrictionUpserted(any()) }
    await untilAsserted { verify(visitSchedulerClient, times(1)).processPersonRestrictionUpserted(any()) }
  }

  @Test
  fun `test prisoner-released is processed`() {
    // Given
    val sentRequestToVsip = PrisonerReleasedNotificationDto(
      prisonerNumber = "TEST",
      prisonCode = "BRI",
      reasonType = RELEASED,
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
    await untilAsserted { verify(visitSchedulerService, times(1)).processPrisonerReleased(any()) }
    await untilAsserted { verify(visitSchedulerClient, times(1)).processPrisonerReleased(any()) }
  }

  @Test
  fun `test prisoner-received is processed`() {
    // Given
    val prisonerReceivedAdditionalInfo = PrisonerReceivedInfo(
      prisonerNumber = "TEST",
      prisonCode = "MDI",
      reason = PrisonerReceivedReasonType.TRANSFERRED,
    )
    val sentRequestToVsip = PrisonerReceivedNotificationDto(prisonerReceivedAdditionalInfo)

    val domainEvent = createDomainEventJson(PRISONER_RECEIVED_TYPE, createPrisonerReceivedAdditionalInformationJson(prisonerReceivedAdditionalInfo))
    val publishRequest = createDomainEventPublishRequest(PRISONER_RECEIVED_TYPE, domainEvent)

    visitSchedulerMockServer.stubPostNotification(VISIT_NOTIFICATION_PRISONER_RECEIVED_CHANGE_PATH)

    // When
    sendSqSMessage(publishRequest)

    // Then
    assertStandardCalls(prisonerReceivedNotifierSpy, VISIT_NOTIFICATION_PRISONER_RECEIVED_CHANGE_PATH, sentRequestToVsip)
    await untilAsserted { verify(visitSchedulerService, times(1)).processPrisonerReceived(any()) }
    await untilAsserted { verify(visitSchedulerClient, times(1)).processPrisonerReceived(any()) }
  }

  @Test
  fun `test visitor-restriction-upserted is processed`() {
    // Given
    val sentRequestToVsip = VisitorRestrictionUpsertedNotificationDto(
      visitorId = "12345",
      validFromDate = LocalDate.parse("2023-09-20"),
      restrictionType = "BAN",
    )

    val domainEvent =
      createDomainEventJson(
        VISITOR_RESTRICTION_UPSERTED_TYPE,
        createPersonRestrictionAdditionalInformationJson(
          visitorId = "12345",
          effectiveDate = "2023-09-20",
          restrictionType = "BAN",
        ),
      )
    val publishRequest = createDomainEventPublishRequest(VISITOR_RESTRICTION_UPSERTED_TYPE, domainEvent)

    visitSchedulerMockServer.stubPostNotification(VISIT_NOTIFICATION_VISITOR_RESTRICTION_UPSERTED_PATH)

    // When
    sendSqSMessage(publishRequest)

    // Then
    assertStandardCalls(visitorRestrictionChangedNotifierSpy, VISIT_NOTIFICATION_VISITOR_RESTRICTION_UPSERTED_PATH, sentRequestToVsip)
    await untilAsserted { verify(visitSchedulerService, times(1)).processVisitorRestrictionUpserted(any()) }
    await untilAsserted { verify(visitSchedulerClient, times(1)).processVisitorRestrictionUpserted(any()) }
  }

  @Test
  fun `test prisoner non association detail changed is processed`() {
    // Given

    val sentRequestToVsip = NonAssociationChangedNotificationDto(
      prisonerNumber = "A8713DY",
      nonAssociationPrisonerNumber = "B2022DY",
      type = NON_ASSOCIATION_CREATED,
    )

    val domainEvent = createDomainEventJson(PRISONER_NON_ASSOCIATION_DETAIL_CREATED_TYPE, createNonAssociationAdditionalInformationJson())
    val publishRequest = createDomainEventPublishRequest(PRISONER_NON_ASSOCIATION_DETAIL_CREATED_TYPE, domainEvent)

    visitSchedulerMockServer.stubPostNotification(VISIT_NOTIFICATION_NON_ASSOCIATION_CHANGE_PATH)

    // When
    sendSqSMessage(publishRequest)

    // Then
    assertStandardCalls(prisonerNonAssociationCreatedNotifier, VISIT_NOTIFICATION_NON_ASSOCIATION_CHANGE_PATH, sentRequestToVsip)
    await untilAsserted { verify(visitSchedulerClient, times(1)).processNonAssociations(any()) }
  }

  @Test
  fun `test prisoner non association detail changed is processed when no valid to date`() {
    // Given

    val sentRequestToVsip = NonAssociationChangedNotificationDto(
      prisonerNumber = "A8713DY",
      nonAssociationPrisonerNumber = "B2022DY",
      type = NON_ASSOCIATION_CREATED,
    )

    val domainEvent = createDomainEventJson(PRISONER_NON_ASSOCIATION_DETAIL_CREATED_TYPE, createNonAssociationAdditionalInformationJson())
    val publishRequest = createDomainEventPublishRequest(PRISONER_NON_ASSOCIATION_DETAIL_CREATED_TYPE, domainEvent)

    visitSchedulerMockServer.stubPostNotification(VISIT_NOTIFICATION_NON_ASSOCIATION_CHANGE_PATH)

    // When
    sendSqSMessage(publishRequest)

    // Then
    assertStandardCalls(prisonerNonAssociationCreatedNotifier, VISIT_NOTIFICATION_NON_ASSOCIATION_CHANGE_PATH, sentRequestToVsip)
    await untilAsserted { verify(visitSchedulerClient, times(1)).processNonAssociations(any()) }
  }

  @Test
  fun `test prisoner add alerts event is processed when alerts are added but no description passed`() {
    // Given
    val prisonerNumber = "A8713DY"
    val bookingId = 100L
    val alertsAdded = listOf("C1", "C2")
    val alertsRemoved = emptyList<String>()
    val activeAlert = listOf(
      AlertResponseDto(
        AlertCodeSummaryDto(alertTypeCode = "T", alertTypeDescription = "Type Description", code = "SC", description = "Alert Code Desc"),
        createdAt = LocalDate.of(1995, 12, 3),
        activeTo = null,
        active = false,
        description = "Alert code comment",
      ),
    )

    val eventDescription = "${alertsAdded.size} alerts added"

    val sentRequestToVsip = PrisonerAlertsAddedNotificationDto(
      prisonerNumber,
      alertsAdded,
      alertsRemoved,
      emptyList(),
      eventDescription,
    )

    val domainEvent = createDomainEventJson(
      PRISONER_ALERTS_UPDATED,
      eventDescription,
      createAlertsUpdatedAdditionalInformationJson(
        prisonerNumber,
        bookingId,
        alertsAdded,
        alertsRemoved,
      ),
    )
    val publishRequest = createDomainEventPublishRequest(PRISONER_ALERTS_UPDATED, domainEvent)

    visitSchedulerMockServer.stubPostNotification(VISIT_NOTIFICATION_PRISONER_ALERTS_UPDATED_PATH)
    alertsApiMockServer.stubGetPrisonerAlertsMono(prisonerNumber, activeAlert)

    // When
    sendSqSMessage(publishRequest)

    // Then
    assertStandardCalls(prisonerAlertsUpdatedNotifier, VISIT_NOTIFICATION_PRISONER_ALERTS_UPDATED_PATH, sentRequestToVsip)
    await untilAsserted { verify(visitSchedulerClient, times(1)).processPrisonerAlertsUpdated(sendDto = sentRequestToVsip) }
  }

  @Test
  fun `test prisoner add alerts event is processed when alerts are added`() {
    // Given
    val prisonerNumber = "A8713DY"
    val bookingId = 100L
    val alertsAdded = listOf("C1", "C2")
    val alertsRemoved = emptyList<String>()
    val activeAlert = listOf(
      AlertResponseDto(
        AlertCodeSummaryDto(alertTypeCode = "T", alertTypeDescription = "Type Description", code = "SC", description = "Alert Code Desc"),
        createdAt = LocalDate.of(1995, 12, 3),
        activeTo = null,
        active = false,
        description = "Alert code comment",
      ),
    )

    val eventDescription = "2 alerts added"

    val sentRequestToVsip = PrisonerAlertsAddedNotificationDto(
      prisonerNumber,
      alertsAdded,
      alertsRemoved,
      emptyList(),
      eventDescription,
    )

    val domainEvent = createDomainEventJson(
      PRISONER_ALERTS_UPDATED,
      eventDescription,
      createAlertsUpdatedAdditionalInformationJson(
        prisonerNumber,
        bookingId,
        alertsAdded,
        alertsRemoved,
      ),
    )
    val publishRequest = createDomainEventPublishRequest(PRISONER_ALERTS_UPDATED, domainEvent)

    visitSchedulerMockServer.stubPostNotification(VISIT_NOTIFICATION_PRISONER_ALERTS_UPDATED_PATH)
    alertsApiMockServer.stubGetPrisonerAlertsMono(prisonerNumber, activeAlert)

    // When
    sendSqSMessage(publishRequest)

    // Then
    assertStandardCalls(prisonerAlertsUpdatedNotifier, VISIT_NOTIFICATION_PRISONER_ALERTS_UPDATED_PATH, sentRequestToVsip)
    await untilAsserted { verify(visitSchedulerClient, times(1)).processPrisonerAlertsUpdated(sendDto = sentRequestToVsip) }
  }

  @Test
  fun `test prisoner add alerts event is processed when no alerts are added and only removed`() {
    // Given
    val prisonerNumber = "A8713DY"
    val bookingId = 100L
    val alertsAdded = emptyList<String>()
    val alertsRemoved = listOf("C1", "C2")
    val activeAlert = listOf(
      AlertResponseDto(
        AlertCodeSummaryDto(alertTypeCode = "T", alertTypeDescription = "Type Description", code = "C1", description = "Alert Code Desc"),
        createdAt = LocalDate.of(1995, 12, 3),
        activeTo = null,
        active = true,
        description = "Alert code comment",
      ),
    )

    val eventDescription = "2 alerts removed"

    val sentRequestToVsip = PrisonerAlertsAddedNotificationDto(
      prisonerNumber,
      alertsAdded,
      alertsRemoved,
      activeAlert.map { it.alertCode.code },
      eventDescription,
    )

    val domainEvent = createDomainEventJson(
      PRISONER_ALERTS_UPDATED,
      eventDescription,
      createAlertsUpdatedAdditionalInformationJson(prisonerNumber, bookingId, alertsAdded, alertsRemoved),
    )
    val publishRequest = createDomainEventPublishRequest(PRISONER_ALERTS_UPDATED, domainEvent)

    visitSchedulerMockServer.stubPostNotification(VISIT_NOTIFICATION_PRISONER_ALERTS_UPDATED_PATH)
    alertsApiMockServer.stubGetPrisonerAlertsMono(prisonerNumber, activeAlert)

    // When
    sendSqSMessage(publishRequest)

    // Then
    verify(prisonerAlertsUpdatedNotifier, times(0)).processEvent(any())
    await untilAsserted { verify(visitSchedulerClient, times(1)).processPrisonerAlertsUpdated(sendDto = sentRequestToVsip) }
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

  @Test
  fun `test visitor unapproved is processed`() {
    // Given
    val visitorUnapprovedEventType = VISITOR_UNAPPROVED_EVENT_TYPE
    val sentRequestToVsip = VisitorApprovedUnapprovedNotificationDto(
      prisonerNumber = "TEST",
      visitorId = "12345",
    )

    val domainEvent =
      createDomainEventJson(
        visitorUnapprovedEventType,
        createAdditionalInformationJson(
          nomsNumber = "TEST",
          personId = "12345",
        ),
      )

    val publishRequest = createDomainEventPublishRequest(visitorUnapprovedEventType, domainEvent)

    visitSchedulerMockServer.stubPostNotification(VISIT_NOTIFICATION_VISITOR_UNAPPROVED_PATH)

    // When
    sendSqSMessage(publishRequest)

    // Then
    assertStandardCalls(visitorUnapprovedNotifier, VISIT_NOTIFICATION_VISITOR_UNAPPROVED_PATH, sentRequestToVsip)
    await untilAsserted { verify(visitSchedulerService, times(1)).processVisitorUnapproved(any()) }
    await untilAsserted { verify(visitSchedulerClient, times(1)).processVisitorUnapproved(any()) }
  }

  @Test
  fun `test visitor approved is processed`() {
    // Given
    val visitorApprovedEventType = VISITOR_APPROVED_EVENT_TYPE
    val sentRequestToVsip = VisitorApprovedUnapprovedNotificationDto(
      prisonerNumber = "TEST",
      visitorId = "12345",
    )

    val domainEvent =
      createDomainEventJson(
        visitorApprovedEventType,
        createAdditionalInformationJson(
          nomsNumber = "TEST",
          personId = "12345",
        ),
      )

    val publishRequest = createDomainEventPublishRequest(visitorApprovedEventType, domainEvent)

    visitSchedulerMockServer.stubPostNotification(VISIT_NOTIFICATION_VISITOR_APPROVED_PATH)

    // When
    sendSqSMessage(publishRequest)

    // Then
    assertStandardCalls(visitorApprovedNotifier, VISIT_NOTIFICATION_VISITOR_APPROVED_PATH, sentRequestToVsip)
    await untilAsserted { verify(visitSchedulerService, times(1)).processVisitorApproved(any()) }
    await untilAsserted { verify(visitSchedulerClient, times(1)).processVisitorApproved(any()) }
  }

  @Test
  fun `test prisoner add alerts event is processed then alerts are correctly filtered to only supported codes`() {
    // Given
    val prisonerNumber = "A8713DY"
    val bookingId = 100L
    val alertsAdded = listOf("C2", "BAD_CODE2")
    val alertsRemoved = listOf("C1", "BAD_CODE")
    val activeAlert = listOf(
      AlertResponseDto(
        AlertCodeSummaryDto(alertTypeCode = "T", alertTypeDescription = "Type Description", code = "SC", description = "Alert Code Desc"),
        createdAt = LocalDate.of(1995, 12, 3),
        activeTo = null,
        active = true,
        description = "Alert code comment",
      ),
      AlertResponseDto(
        AlertCodeSummaryDto(alertTypeCode = "T", alertTypeDescription = "Type Description", code = "BAD_CODE", description = "Alert Code Desc"),
        createdAt = LocalDate.of(1995, 12, 3),
        activeTo = null,
        active = true,
        description = "Alert code comment",
      ),
    )

    val eventDescription = "2 alerts added, 2 alerts removed"

    val sentRequestToVsip = PrisonerAlertsAddedNotificationDto(
      prisonerNumber,
      listOf("C2"),
      listOf("C1"),
      listOf("SC"),
      eventDescription,
    )

    val domainEvent = createDomainEventJson(
      PRISONER_ALERTS_UPDATED,
      eventDescription,
      createAlertsUpdatedAdditionalInformationJson(prisonerNumber, bookingId, alertsAdded, alertsRemoved),
    )
    val publishRequest = createDomainEventPublishRequest(PRISONER_ALERTS_UPDATED, domainEvent)

    visitSchedulerMockServer.stubPostNotification(VISIT_NOTIFICATION_PRISONER_ALERTS_UPDATED_PATH)
    alertsApiMockServer.stubGetPrisonerAlertsMono(prisonerNumber, activeAlert)

    // When
    sendSqSMessage(publishRequest)

    // Then
    verify(prisonerAlertsUpdatedNotifier, times(0)).processEvent(any())
    await untilAsserted { verify(visitSchedulerClient, times(1)).processPrisonerAlertsUpdated(sendDto = sentRequestToVsip) }
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
