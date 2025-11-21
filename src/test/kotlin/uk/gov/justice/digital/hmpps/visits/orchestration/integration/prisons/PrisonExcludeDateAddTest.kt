package uk.gov.justice.digital.hmpps.visits.orchestration.integration.prisons

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.prisons.ExcludeDateDto
import uk.gov.justice.digital.hmpps.visits.orchestration.integration.IntegrationTestBase
import java.time.LocalDate

@DisplayName("Add prison exclude dates tests")
class PrisonExcludeDateAddTest : IntegrationTestBase() {
  final val prisonCode = "HEI"

  fun callAddExcludeDate(
    webTestClient: WebTestClient,
    prisonCode: String,
    excludeDateDto: ExcludeDateDto,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec = webTestClient.put()
    .uri("/config/prisons/prison/$prisonCode/exclude-date/add")
    .body(BodyInserters.fromValue(excludeDateDto))
    .headers(authHttpHeaders)
    .exchange()

  @Test
  fun `when add exclude date added and is successful a 201 is returned`() {
    // Given
    val excludeDateFuture = LocalDate.now().plusDays(3)
    val excludeDateDto = ExcludeDateDto(excludeDateFuture, "user-6")

    visitSchedulerMockServer.stubAddExcludeDate(prisonCode, listOf(excludeDateFuture))

    // When
    val responseSpec = callAddExcludeDate(webTestClient, "HEI", excludeDateDto, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().is2xxSuccessful
  }

  @Test
  fun `when NOT_FOUND is returned from visit scheduler then NOT_FOUND status is sent back`() {
    // Given
    val prisonCode = "HEI"
    val excludeDateDto = ExcludeDateDto(LocalDate.now(), "user-6")
    visitSchedulerMockServer.stubAddExcludeDate(prisonCode, null, HttpStatus.NOT_FOUND)

    // When
    val responseSpec = callAddExcludeDate(webTestClient, "HEI", excludeDateDto, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
  }

  @Test
  fun `when BAD_REQUEST is returned from visit scheduler then BAD_REQUEST status is sent back`() {
    // Given
    val prisonCode = "HEI"
    val excludeDateDto = ExcludeDateDto(LocalDate.now(), "user-6")
    visitSchedulerMockServer.stubAddExcludeDate(prisonCode, null, HttpStatus.BAD_REQUEST)

    // When
    val responseSpec = callAddExcludeDate(webTestClient, "HEI", excludeDateDto, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isBadRequest
  }
}
