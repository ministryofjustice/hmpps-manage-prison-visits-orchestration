package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.sessions

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase

@DisplayName("Get visit sessions")
class VisitSessionsTest : IntegrationTestBase() {
  fun callGetVisitSessions(
    webTestClient: WebTestClient,
    prisonCode: String,
    prisonerId: String,
    authHttpHeaders: (HttpHeaders) -> Unit
  ): WebTestClient.ResponseSpec {
    return webTestClient.get().uri("/visit-sessions?prisonId=$prisonCode&prisonerId=$prisonerId")
      .headers(authHttpHeaders)
      .exchange()
  }

  @Test
  fun `when visit sessions for parameters exist then allowed sessions are returned`() {
    // Given
    val prisonCode = "MDI"
    val prisonerId = "ABC"
    val visitSessionDto1 = createVisitSessionDto(prisonCode, 1)
    val visitSessionDto2 = createVisitSessionDto(prisonCode, 2)
    val visitSessionDto3 = createVisitSessionDto(prisonCode, 3)
    val visitSessionDto4 = createVisitSessionDto(prisonCode, 4)
    val visitSessionDto5 = createVisitSessionDto(prisonCode, 5)

    visitSchedulerMockServer.stubGetVisitSessions(prisonCode, prisonerId, mutableListOf(visitSessionDto1, visitSessionDto2, visitSessionDto3, visitSessionDto4, visitSessionDto5))

    // When
    val responseSpec = callGetVisitSessions(webTestClient, prisonCode, prisonerId, roleVisitSchedulerHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(5)
  }

  @Test
  fun `when visit sessions for parameters do not exist then empty list is returned`() {
    // Given
    val prisonCode = "MDI"
    val prisonerId = "ABC"

    visitSchedulerMockServer.stubGetVisitSessions(prisonCode, prisonerId, mutableListOf())

    // When
    val responseSpec = callGetVisitSessions(webTestClient, prisonCode, prisonerId, roleVisitSchedulerHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(0)
  }
}
