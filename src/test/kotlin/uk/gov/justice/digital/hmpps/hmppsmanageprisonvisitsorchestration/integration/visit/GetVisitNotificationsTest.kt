package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.visit

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller.VISIT_NOTIFICATION_TYPES
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.NotificationEventType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.NotificationEventType.PRISONER_RELEASED_EVENT
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.NotificationEventType.PRISONER_RESTRICTION_CHANGE_EVENT
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase

@DisplayName("GET visits/notification/CFI/count and /visits/notification/count")
class GetVisitNotificationsTest : IntegrationTestBase() {

  val prisonCode = "ABC"

  @Test
  fun `when notification count is requested for all prisons`() {
    // Given
    val bookingReference = "v9*d7*ed*7u"
    visitSchedulerMockServer.stubGetVisitNotificationTypes(bookingReference, PRISONER_RELEASED_EVENT, PRISONER_RESTRICTION_CHANGE_EVENT)

    // When
    val responseSpec = callGetVisitNotificationTypes(webTestClient, bookingReference, roleVisitSchedulerHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val notifications = this.getNotificationTypes(responseSpec)
    Assertions.assertThat(notifications.size).isEqualTo(2)
    Assertions.assertThat(notifications[0]).isEqualTo(PRISONER_RELEASED_EVENT)
    Assertions.assertThat(notifications[1]).isEqualTo(PRISONER_RESTRICTION_CHANGE_EVENT)
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
