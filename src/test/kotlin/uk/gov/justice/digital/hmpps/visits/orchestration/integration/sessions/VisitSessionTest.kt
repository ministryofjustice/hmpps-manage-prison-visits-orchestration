package uk.gov.justice.digital.hmpps.visits.orchestration.integration.sessions

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.VisitSessionDto
import uk.gov.justice.digital.hmpps.visits.orchestration.integration.IntegrationTestBase
import java.time.LocalDate

@DisplayName("Get Session Test")
class VisitSessionTest : IntegrationTestBase() {
  fun callGetSession(
    webTestClient: WebTestClient,
    prisonCode: String,
    sessionDate: LocalDate,
    sessionTemplateReference: String,
    visitSessionDto: VisitSessionDto?,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec = webTestClient.get().uri("/visit-sessions/session?prisonCode=$prisonCode&sessionDate=$sessionDate&sessionTemplateReference=$sessionTemplateReference")
    .headers(authHttpHeaders)
    .exchange()

  @Test
  fun `when session exists then it is returned`() {
    // Given
    val prisonCode = "MDI"
    val sessionDate = LocalDate.now()
    val sessionTemplateReference = "abc-def-ghi"
    val visitSessionDto = createVisitSessionDto(prisonCode, sessionTemplateReference)

    visitSchedulerMockServer.stubGetSession(prisonCode, sessionDate, sessionTemplateReference, visitSessionDto)

    // When
    val responseSpec = callGetSession(webTestClient, prisonCode, sessionDate, sessionTemplateReference, visitSessionDto, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val visitSessionDtoResponse = objectMapper.readValue(responseSpec.expectBody().returnResult().responseBody, VisitSessionDto::class.java)

    Assertions.assertThat(visitSessionDtoResponse.sessionTemplateReference).isEqualTo(sessionTemplateReference)
  }

  @Test
  fun `when session is invalid or doesn't exist then a NOT_FOUND status is returned`() {
    // Given
    val prisonCode = "MDI"
    val sessionDate = LocalDate.now()
    val sessionTemplateReference = "abc-def-ghi"
    val visitSessionDto = null

    visitSchedulerMockServer.stubGetSession(prisonCode, sessionDate, sessionTemplateReference, visitSessionDto)

    // When
    val responseSpec = callGetSession(webTestClient, prisonCode, sessionDate, sessionTemplateReference, visitSessionDto, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
  }
}
