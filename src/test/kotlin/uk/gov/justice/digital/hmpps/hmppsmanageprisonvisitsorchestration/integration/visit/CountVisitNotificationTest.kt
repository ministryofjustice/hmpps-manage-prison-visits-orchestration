package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.visit

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller.VISIT_NOTIFICATION_COUNT_FOR_PRISON_PATH
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.NotificationCountDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.NotificationEventType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.NotificationEventType.PRISON_VISITS_BLOCKED_FOR_DATE
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.TestObjectMapper

@DisplayName("GET visits/notification/{prisonCode}/count")
class CountVisitNotificationTest : IntegrationTestBase() {

  @BeforeEach
  fun resetStubs() {
    visitSchedulerMockServer.resetAll()
  }

  val prisonCode = "ABC"

  @Test
  fun `when notification count is requested for a prison without notification types`() {
    // Given
    visitSchedulerMockServer.stubGetCountVisitNotificationForPrison(prisonCode, notificationEventTypes = null, 1)
    // When
    val responseSpec = callCountVisitNotificationForAPrison(webTestClient, prisonCode, null, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val notificationCount = this.getNotificationCountDto(responseSpec)
    Assertions.assertThat(notificationCount.count).isEqualTo(1)
  }

  @Test
  fun `when notification count is requested for a prison with notification types then counts are returned`() {
    // Given
    val notificationEventTypes = listOf(PRISON_VISITS_BLOCKED_FOR_DATE)
    visitSchedulerMockServer.stubGetCountVisitNotificationForPrison(prisonCode, notificationEventTypes, 2)
    // When
    val responseSpec = callCountVisitNotificationForAPrison(webTestClient, prisonCode, notificationEventTypes, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val notificationCount = this.getNotificationCountDto(responseSpec)
    Assertions.assertThat(notificationCount.count).isEqualTo(2)
  }

  fun getNotificationCountDto(responseSpec: ResponseSpec): NotificationCountDto = TestObjectMapper.mapper.readValue(responseSpec.expectBody().returnResult().responseBody, NotificationCountDto::class.java)

  fun callCountVisitNotificationForAPrison(
    webTestClient: WebTestClient,
    prisonCode: String,
    notificationEventTypes: List<NotificationEventType>?,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): ResponseSpec {
    var url = VISIT_NOTIFICATION_COUNT_FOR_PRISON_PATH.replace("{prisonCode}", prisonCode)
    url = if (notificationEventTypes != null) {
      url + "?types=${notificationEventTypes.joinToString(",") { it.name }}"
    } else {
      url
    }

    return webTestClient.get().uri(url)
      .headers(authHttpHeaders)
      .exchange()
  }
}
