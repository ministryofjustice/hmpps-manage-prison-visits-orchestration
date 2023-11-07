package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.visit

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.NotificationCountDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase

@DisplayName("GET visits/notification/CFI/count and /visits/notification/count")
class CountVisitNotificationTest : IntegrationTestBase() {

  val prisonCode = "ABC"

  @Test
  fun `when notification count is requested for all prisons`() {
    // Given
    visitSchedulerMockServer.stubGetCountVisitNotification()

    // When
    val responseSpec = callCountVisitNotification(webTestClient, roleVisitSchedulerHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val notificationCount = this.getNotificationCountDto(responseSpec)
    Assertions.assertThat(notificationCount.count).isEqualTo(2)
  }

  @Test
  fun `when notification count is requested for a prisons`() {
    // Given
    visitSchedulerMockServer.stubGetCountVisitNotificationForPrison(prisonCode)
    // When
    val responseSpec = callCountVisitNotificationForAPrison(webTestClient, prisonCode, roleVisitSchedulerHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val notificationCount = this.getNotificationCountDto(responseSpec)
    Assertions.assertThat(notificationCount.count).isEqualTo(1)
  }

  fun getNotificationCountDto(responseSpec: ResponseSpec): NotificationCountDto =
    objectMapper.readValue(responseSpec.expectBody().returnResult().responseBody, NotificationCountDto::class.java)

  fun callCountVisitNotificationForAPrison(
    webTestClient: WebTestClient,
    prisonCode: String,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    return webTestClient.get().uri("/visits/notification/$prisonCode/count")
      .headers(authHttpHeaders)
      .exchange()
  }

  fun callCountVisitNotification(
    webTestClient: WebTestClient,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    return webTestClient.get().uri("/visits/notification/count")
      .headers(authHttpHeaders)
      .exchange()
  }
}
