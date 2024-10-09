package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.visit

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.IgnoreVisitNotificationsOrchestrationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase

@DisplayName("Ignore visit notifications")
class IgnoreVisitNotificationsTest : IntegrationTestBase() {
  fun callIgnoreVisitNotifications(
    webTestClient: WebTestClient,
    reference: String,
    ignoreVisitNotificationsOrchestration: IgnoreVisitNotificationsOrchestrationDto,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    return webTestClient.put().uri("/visits/notification/visit/$reference/ignore")
      .headers(authHttpHeaders)
      .body(BodyInserters.fromValue(ignoreVisitNotificationsOrchestration))
      .exchange()
  }

  @Test
  fun `when ignore visit notifications is successful then OK status is returned`() {
    // Given
    val reference = "aa-bb-cc-dd"
    val username = "A_USER"
    val ignoreVisitNotifications = IgnoreVisitNotificationsOrchestrationDto("reason", username)
    val visitDto = createVisitDto(reference = reference)
    visitSchedulerMockServer.stubIgnoreVisitNotifications(reference, visitDto)

    // When
    val responseSpec = callIgnoreVisitNotifications(webTestClient, reference, ignoreVisitNotifications, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
      .expectBody()
      .jsonPath("$.reference").isEqualTo(reference)
  }

  @Test
  fun `when ignore visit notifications is unsuccessful then NOT_FOUND status is returned`() {
    // Given
    val reference = "aa-bb-cc-dd"
    val username = "A_USER"
    val ignoreVisitNotifications = IgnoreVisitNotificationsOrchestrationDto("reason", username)
    val visitDto = null
    visitSchedulerMockServer.stubIgnoreVisitNotifications(reference, visitDto)

    // When
    val responseSpec = callIgnoreVisitNotifications(webTestClient, reference, ignoreVisitNotifications, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
  }

  @Test
  fun `when ignore visit notifications is unsuccessful with forbidden then FORBIDDEN status is returned`() {
    // Given
    val reference = "aa-bb-cc-dd"
    val username = "A_USER"
    val ignoreVisitNotifications = IgnoreVisitNotificationsOrchestrationDto("reason", username)
    val visitDto = null
    visitSchedulerMockServer.stubIgnoreVisitNotifications(reference, visitDto, HttpStatus.FORBIDDEN)

    // When
    val responseSpec = callIgnoreVisitNotifications(webTestClient, reference, ignoreVisitNotifications, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isForbidden
  }
}
