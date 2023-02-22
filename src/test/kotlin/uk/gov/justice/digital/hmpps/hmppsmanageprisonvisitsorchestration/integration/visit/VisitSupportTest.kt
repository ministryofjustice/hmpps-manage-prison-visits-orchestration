package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.visit

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SupportTypeDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase

@DisplayName("Get visit support")
class VisitSupportTest : IntegrationTestBase() {
  fun callGetVisitSupport(
    webTestClient: WebTestClient,
    authHttpHeaders: (HttpHeaders) -> Unit
  ): WebTestClient.ResponseSpec {
    return webTestClient.get().uri("/visit-support")
      .headers(authHttpHeaders)
      .exchange()
  }

  @Test
  fun `when visit support values exist then all values are returned`() {
    // Given
    val supportTypeDto1 = SupportTypeDto("wheelchair", "wheelchair support")
    val supportTypeDto2 = SupportTypeDto("mask", "mask")
    visitSchedulerMockServer.stubGetVisitSupport(mutableListOf(supportTypeDto1, supportTypeDto2))

    // When
    val responseSpec = callGetVisitSupport(webTestClient, roleVisitSchedulerHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(2)
  }

  @Test
  fun `when visit support values do not exist then empty list is returned`() {
    // Given
    visitSchedulerMockServer.stubGetVisitSupport(mutableListOf())

    // When
    val responseSpec = callGetVisitSupport(webTestClient, roleVisitSchedulerHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(0)
  }
}
