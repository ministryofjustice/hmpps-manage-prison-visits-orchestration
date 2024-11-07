package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.sessions

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.prisons.ExcludeDateDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import java.time.LocalDate

@DisplayName("Remove session exclude dates tests")
class SessionExcludeDateRemoveTest : IntegrationTestBase() {
  private final val sessionTemplateReference = "aaa-bbb-ccc"

  fun callRemoveSessionExcludeDate(
    webTestClient: WebTestClient,
    sessionTemplateReference: String,
    excludeDateDto: ExcludeDateDto,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    return webTestClient.put()
      .uri("/config/sessions/session/$sessionTemplateReference/exclude-date/remove")
      .body(BodyInserters.fromValue(excludeDateDto))
      .headers(authHttpHeaders)
      .exchange()
  }

  @Test
  fun `when remove exclude date added and is successful a successful response is returned`() {
    // Given
    val excludeDateFuture = LocalDate.now().plusDays(3)
    val excludeDateDto = ExcludeDateDto(excludeDateFuture, "user-6")

    visitSchedulerMockServer.stubRemoveSessionTemplateExcludeDate(sessionTemplateReference, listOf(LocalDate.now()))

    // When
    val responseSpec = callRemoveSessionExcludeDate(webTestClient, sessionTemplateReference, excludeDateDto, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().is2xxSuccessful
  }

  @Test
  fun `when NOT_FOUND is returned from visit scheduler then NOT_FOUND status is sent back`() {
    // Given
    val excludeDateDto = ExcludeDateDto(LocalDate.now(), "user-6")
    visitSchedulerMockServer.stubRemoveSessionTemplateExcludeDate(sessionTemplateReference, null, HttpStatus.NOT_FOUND)

    // When
    val responseSpec = callRemoveSessionExcludeDate(webTestClient, sessionTemplateReference, excludeDateDto, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
  }

  @Test
  fun `when BAD_REQUEST is returned from visit scheduler then BAD_REQUEST status is sent back`() {
    // Given
    val excludeDateDto = ExcludeDateDto(LocalDate.now(), "user-6")
    visitSchedulerMockServer.stubRemoveSessionTemplateExcludeDate(sessionTemplateReference, null, HttpStatus.BAD_REQUEST)

    // When
    val responseSpec = callRemoveSessionExcludeDate(webTestClient, sessionTemplateReference, excludeDateDto, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isBadRequest
  }
}
