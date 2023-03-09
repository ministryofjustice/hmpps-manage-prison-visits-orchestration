package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.visit

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase

@DisplayName("Get multiple visits by filter")
class VisitsByFilterTest : IntegrationTestBase() {
  fun callGetVisits(
    webTestClient: WebTestClient,
    prisonerId: String,
    visitStatus: String,
    page: Int,
    size: Int,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    return webTestClient.get().uri("/visits/search?prisonerId=$prisonerId&visitStatus=$visitStatus&page=$page&size=$size")
      .headers(authHttpHeaders)
      .exchange()
  }

  @Test
  fun `when visits for parameters exist then get visits by filter returns content`() {
    // Given
    val visitDto = createVisitDto(reference = "ss-bb")
    val visitDto2 = createVisitDto(reference = "xx-bb")
    val prisonerId = "ABC"
    val visitStatus = "BOOKED"
    val visitsList = mutableListOf(visitDto, visitDto2)
    visitSchedulerMockServer.stubGetVisits(listOf(visitStatus), prisonerId, 1, 10, visitsList)

    // When
    val responseSpec = callGetVisits(webTestClient, prisonerId, visitStatus, 1, 10, roleVisitSchedulerHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
      .expectBody()
      .jsonPath("$.content").isNotEmpty
      .jsonPath("$.content.size()").isEqualTo(2)
  }

  @Test
  fun `when visits for parameters do not exist then get visits by filter returns empty content`() {
    // Given
    val prisonerId = "AABBCC"
    val visitStatus = "BOOKED"
    val emptyList = mutableListOf<VisitDto>()
    visitSchedulerMockServer.stubGetVisits(listOf(visitStatus), prisonerId, 1, 10, emptyList)

    // When
    val responseSpec = callGetVisits(webTestClient, prisonerId, visitStatus, 1, 10, roleVisitSchedulerHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
      .expectBody()
      .jsonPath("$.content").isEmpty
  }
}
