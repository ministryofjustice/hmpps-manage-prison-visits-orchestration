package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.visit

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller.VISIT_NOTIFICATION_EVENTS
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.NotificationEventAttributeType.VISITOR_ID
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.NotificationEventAttributeType.VISITOR_RESTRICTION
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.NotificationEventType.NON_ASSOCIATION_EVENT
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.NotificationEventType.PRISONER_RELEASED_EVENT
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.NotificationEventType.PRISONER_RESTRICTION_CHANGE_EVENT
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.VisitNotificationEventAttributeDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.VisitNotificationEventDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import java.util.*

@DisplayName("GET visits/notification/CFI/count and /visits/notification/count")
class GetVisitNotificationEventsTest : IntegrationTestBase() {

  val prisonCode = "ABC"

  @Test
  fun `when get notification events called for visit reference then all notification events are returned`() {
    // Given
    val bookingReference = "v9*d7*ed*7u"
    val notification1 = createNotificationEvent(NON_ASSOCIATION_EVENT)
    val notification2EventAttribute1 = VisitNotificationEventAttributeDto(VISITOR_ID, "10001")
    val notification2EventAttribute2 = VisitNotificationEventAttributeDto(VISITOR_RESTRICTION, "BAN")
    val notification2 = createNotificationEvent(PRISONER_RELEASED_EVENT, additionalData = listOf(notification2EventAttribute1, notification2EventAttribute2))
    val notification3 = createNotificationEvent(PRISONER_RESTRICTION_CHANGE_EVENT)

    visitSchedulerMockServer.stubGetVisitNotificationEvents(
      bookingReference,
      listOf(notification1, notification2, notification3),
    )

    // When
    val responseSpec = callGetVisitNotificationEvents(webTestClient, bookingReference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val notifications = this.getNotificationEvents(responseSpec)
    Assertions.assertThat(notifications.size).isEqualTo(3)
    assertNotificationEvent(notifications[0], notification1.type, emptyList())
    assertNotificationEvent(notifications[1], notification2.type, listOf(notification2EventAttribute1, notification2EventAttribute2))
    assertNotificationEvent(notifications[2], notification3.type, emptyList())
  }

  fun getNotificationEvents(responseSpec: ResponseSpec): Array<VisitNotificationEventDto> = objectMapper.readValue(responseSpec.expectBody().returnResult().responseBody, Array<VisitNotificationEventDto>::class.java)

  fun callGetVisitNotificationEvents(
    webTestClient: WebTestClient,
    bookingReference: String,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): ResponseSpec = webTestClient.get().uri(VISIT_NOTIFICATION_EVENTS.replace("{reference}", bookingReference))
    .headers(authHttpHeaders)
    .exchange()
}
