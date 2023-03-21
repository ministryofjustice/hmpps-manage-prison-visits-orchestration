package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.visit

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase

@DisplayName("Get supported prisons")
class SupportedPrisonsTest : IntegrationTestBase() {
  fun callGetSupportedPrisons(
    webTestClient: WebTestClient,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    return webTestClient.get().uri("/config/prisons/supported")
      .headers(authHttpHeaders)
      .exchange()
  }

  @Test
  fun `when active prisons exist then all active prisons are returned`() {
    // Given
    val prisons = arrayOf("BLI", "HEI")
    visitSchedulerMockServer.stubGetSupportedPrisons(prisons.toMutableList())

    // When
    val responseSpec = callGetSupportedPrisons(webTestClient, roleVisitSchedulerHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val results = getResults(returnResult)

    Assertions.assertThat(results.size).isEqualTo(2)
    Assertions.assertThat(results).containsExactlyInAnyOrder(*prisons)
  }

  @Test
  fun `when active prisons do not exist then empty list is returned`() {
    // Given
    visitSchedulerMockServer.stubGetSupportedPrisons(mutableListOf())

    // When
    val responseSpec = callGetSupportedPrisons(webTestClient, roleVisitSchedulerHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(0)
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): Array<String> {
    return objectMapper.readValue(returnResult.returnResult().responseBody, Array<String>::class.java)
  }
}
