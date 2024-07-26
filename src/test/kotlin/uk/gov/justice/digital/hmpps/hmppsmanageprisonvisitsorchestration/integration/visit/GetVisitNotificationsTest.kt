package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.visit

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller.VISIT_NOTIFICATION_TYPES
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.NotificationEventType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.NotificationEventType.NON_ASSOCIATION_EVENT
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.NotificationEventType.PERSON_RESTRICTION_UPSERTED_EVENT
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.NotificationEventType.PRISONER_ALERTS_UPDATED_EVENT
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.NotificationEventType.PRISONER_RECEIVED_EVENT
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.NotificationEventType.PRISONER_RELEASED_EVENT
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.NotificationEventType.PRISONER_RESTRICTION_CHANGE_EVENT
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.NotificationEventType.PRISON_VISITS_BLOCKED_FOR_DATE
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase

@DisplayName("GET visits/notification/CFI/count and /visits/notification/count")
class GetVisitNotificationsTest : IntegrationTestBase() {

  val prisonCode = "ABC"

  @Test
  fun `when notification count is requested for all prisons`() {
    // Given
    val bookingReference = "v9*d7*ed*7u"
    visitSchedulerMockServer.stubGetVisitNotificationTypes(
      bookingReference,
      NON_ASSOCIATION_EVENT,
      PRISONER_RELEASED_EVENT,
      PRISONER_RESTRICTION_CHANGE_EVENT,
      PRISONER_ALERTS_UPDATED_EVENT,
      PRISON_VISITS_BLOCKED_FOR_DATE,
      PRISONER_RECEIVED_EVENT,
      PERSON_RESTRICTION_UPSERTED_EVENT,
    )

    // When
    val responseSpec = callGetVisitNotificationTypes(webTestClient, bookingReference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val notifications = this.getNotificationTypes(responseSpec)
    Assertions.assertThat(notifications.size).isEqualTo(7)
    Assertions.assertThat(notifications.contains(NON_ASSOCIATION_EVENT)).isTrue()
    Assertions.assertThat(notifications.contains(PRISONER_RELEASED_EVENT)).isTrue()
    Assertions.assertThat(notifications.contains(PRISONER_RESTRICTION_CHANGE_EVENT)).isTrue()
    Assertions.assertThat(notifications.contains(PRISONER_ALERTS_UPDATED_EVENT)).isTrue()
    Assertions.assertThat(notifications.contains(PRISON_VISITS_BLOCKED_FOR_DATE)).isTrue()
    Assertions.assertThat(notifications.contains(PRISONER_RECEIVED_EVENT)).isTrue()
    Assertions.assertThat(notifications.contains(PERSON_RESTRICTION_UPSERTED_EVENT)).isTrue()
  }

  fun getNotificationTypes(responseSpec: ResponseSpec): Array<NotificationEventType> =
    objectMapper.readValue(responseSpec.expectBody().returnResult().responseBody, Array<NotificationEventType>::class.java)

  fun callGetVisitNotificationTypes(
    webTestClient: WebTestClient,
    bookingReference: String,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): ResponseSpec {
    return webTestClient.get().uri(VISIT_NOTIFICATION_TYPES.replace("{reference}", bookingReference))
      .headers(authHttpHeaders)
      .exchange()
  }
}
