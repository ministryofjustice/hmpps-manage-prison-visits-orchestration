package uk.gov.justice.digital.hmpps.visits.orchestration.integration.visit

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec
import uk.gov.justice.digital.hmpps.visits.orchestration.controller.VISIT_REQUESTS_COUNT_FOR_PRISON_PATH
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.VisitRequestsCountDto
import uk.gov.justice.digital.hmpps.visits.orchestration.integration.IntegrationTestBase

@DisplayName("GET - $VISIT_REQUESTS_COUNT_FOR_PRISON_PATH")
class CountVisitRequestsTest : IntegrationTestBase() {

  @BeforeEach
  fun resetStubs() {
    visitSchedulerMockServer.resetAll()
  }

  val prisonCode = "ABC"

  @Test
  fun `when visit requests count is requested for a prison then count is returned`() {
    // Given
    visitSchedulerMockServer.stubGetCountVisitRequestsForPrison(prisonCode, 1)

    // When
    val responseSpec = callCountVisitRequestsForAPrison(webTestClient, prisonCode, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val visitRequestsCountDto = getVisitRequestsCountDto(responseSpec)
    Assertions.assertThat(visitRequestsCountDto.count).isEqualTo(1)
  }

  @Test
  fun `when visit requests count is requested but error is returned, it is returned to caller`() {
    // Given
    visitSchedulerMockServer.stubGetCountVisitRequestsForPrison(prisonCode, 2, HttpStatus.INTERNAL_SERVER_ERROR)

    // When
    val responseSpec = callCountVisitRequestsForAPrison(webTestClient, prisonCode, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().is5xxServerError
  }

  fun getVisitRequestsCountDto(responseSpec: ResponseSpec): VisitRequestsCountDto = objectMapper.readValue(responseSpec.expectBody().returnResult().responseBody, VisitRequestsCountDto::class.java)

  fun callCountVisitRequestsForAPrison(
    webTestClient: WebTestClient,
    prisonCode: String,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): ResponseSpec {
    val url = VISIT_REQUESTS_COUNT_FOR_PRISON_PATH.replace("{prisonCode}", prisonCode)

    return webTestClient.get().uri(url)
      .headers(authHttpHeaders)
      .exchange()
  }
}
