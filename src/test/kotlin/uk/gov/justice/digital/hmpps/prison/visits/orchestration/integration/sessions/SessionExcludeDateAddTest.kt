package uk.gov.justice.digital.hmpps.prison.visits.orchestration.integration.sessions

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.visit.scheduler.prisons.ExcludeDateDto
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.integration.IntegrationTestBase
import java.time.LocalDate

@DisplayName("Add session exclude dates tests")
class SessionExcludeDateAddTest : IntegrationTestBase() {
  private final val sessionTemplateReference = "aaa-bbb-ccc"

  fun callAddSessionExcludeDate(
    webTestClient: WebTestClient,
    sessionTemplateReference: String,
    excludeDateDto: ExcludeDateDto,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec = webTestClient.put()
    .uri("/config/sessions/session/$sessionTemplateReference/exclude-date/add")
    .body(BodyInserters.fromValue(excludeDateDto))
    .headers(authHttpHeaders)
    .exchange()

  @Test
  fun `when add exclude date added and is successful a 201 is returned`() {
    // Given
    val excludeDateFuture = LocalDate.now().plusDays(3)
    val excludeDateDto = ExcludeDateDto(excludeDateFuture, "user-6")

    visitSchedulerMockServer.stubAddSessionTemplateExcludeDate(sessionTemplateReference, listOf(excludeDateFuture))

    // When
    val responseSpec = callAddSessionExcludeDate(webTestClient, sessionTemplateReference, excludeDateDto, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().is2xxSuccessful
  }

  @Test
  fun `when NOT_FOUND is returned from visit scheduler then NOT_FOUND status is sent back`() {
    // Given
    val sessionTemplateReference = sessionTemplateReference
    val excludeDateDto = ExcludeDateDto(LocalDate.now(), "user-6")
    visitSchedulerMockServer.stubAddSessionTemplateExcludeDate(sessionTemplateReference, null, HttpStatus.NOT_FOUND)

    // When
    val responseSpec = callAddSessionExcludeDate(webTestClient, sessionTemplateReference, excludeDateDto, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
  }

  @Test
  fun `when BAD_REQUEST is returned from visit scheduler then BAD_REQUEST status is sent back`() {
    // Given
    val sessionTemplateReference = sessionTemplateReference
    val excludeDateDto = ExcludeDateDto(LocalDate.now(), "user-6")
    visitSchedulerMockServer.stubAddSessionTemplateExcludeDate(sessionTemplateReference, null, HttpStatus.BAD_REQUEST)

    // When
    val responseSpec = callAddSessionExcludeDate(webTestClient, sessionTemplateReference, excludeDateDto, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isBadRequest
  }
}
