package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.visit

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase

@DisplayName("Get supported prisons")
class SupportedPrisonsTest : IntegrationTestBase() {
  fun callGetSupportedPrisons(
    webTestClient: WebTestClient,
    authHttpHeaders: (HttpHeaders) -> Unit
  ): WebTestClient.ResponseSpec {
    return webTestClient.get().uri("/config/prisons/supported")
      .headers(authHttpHeaders)
      .exchange()
  }

  @Test
  fun `when visit support values exist then all values are returned`() {
    // Given
    visitSchedulerMockServer.stubGetSupportedPrisons(mutableListOf("BLI", "HEI"))

    // When
    val responseSpec = callGetSupportedPrisons(webTestClient, roleVisitSchedulerHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(2)
  }

  @Test
  fun `when visit support values do not exist then empty list is returned`() {
    // Given
    visitSchedulerMockServer.stubGetSupportedPrisons(mutableListOf())

    // When
    val responseSpec = callGetSupportedPrisons(webTestClient, roleVisitSchedulerHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(0)
  }
}
