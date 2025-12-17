package uk.gov.justice.digital.hmpps.orchestration.integration.prisons

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.orchestration.dto.visit.scheduler.prisons.ExcludeDateDto
import uk.gov.justice.digital.hmpps.orchestration.integration.IntegrationTestBase
import java.time.LocalDate

@DisplayName("Remove prison exclude dates tests")
class PrisonExcludeDateRemoveTest : IntegrationTestBase() {
  final val prisonCode = "HEI"

  fun callRemoveExcludeDate(
    webTestClient: WebTestClient,
    prisonCode: String,
    excludeDateDto: ExcludeDateDto,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec = webTestClient.put()
    .uri("/config/prisons/prison/$prisonCode/exclude-date/remove")
    .body(BodyInserters.fromValue(excludeDateDto))
    .headers(authHttpHeaders)
    .exchange()

  @Test
  fun `when remove exclude date added and is successful a successful response is returned`() {
    // Given
    val excludeDateFuture = LocalDate.now().plusDays(3)
    val excludeDateDto = ExcludeDateDto(excludeDateFuture, "user-6")

    visitSchedulerMockServer.stubRemoveExcludeDate(prisonCode, listOf(LocalDate.now()))

    // When
    val responseSpec = callRemoveExcludeDate(webTestClient, "HEI", excludeDateDto, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().is2xxSuccessful
  }

  @Test
  fun `when NOT_FOUND is returned from visit scheduler then NOT_FOUND status is sent back`() {
    // Given
    val prisonCode = "HEI"
    val excludeDateDto = ExcludeDateDto(LocalDate.now(), "user-6")
    visitSchedulerMockServer.stubRemoveExcludeDate(prisonCode, null, HttpStatus.NOT_FOUND)

    // When
    val responseSpec = callRemoveExcludeDate(webTestClient, "HEI", excludeDateDto, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
  }

  @Test
  fun `when BAD_REQUEST is returned from visit scheduler then BAD_REQUEST status is sent back`() {
    // Given
    val prisonCode = "HEI"
    val excludeDateDto = ExcludeDateDto(LocalDate.now(), "user-6")
    visitSchedulerMockServer.stubRemoveExcludeDate(prisonCode, null, HttpStatus.BAD_REQUEST)

    // When
    val responseSpec = callRemoveExcludeDate(webTestClient, "HEI", excludeDateDto, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isBadRequest
  }
}
