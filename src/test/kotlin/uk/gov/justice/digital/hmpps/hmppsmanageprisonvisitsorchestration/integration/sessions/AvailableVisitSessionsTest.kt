package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.sessions

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.AvailableVisitSessionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitRestriction
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.visitscheduler.dto.sessions.SessionTimeSlotDto
import java.time.LocalDate
import java.time.LocalTime

@DisplayName("Get available visit sessions")
class AvailableVisitSessionsTest : IntegrationTestBase() {
  fun callGetAvailableVisitSessions(
    webTestClient: WebTestClient,
    prisonCode: String,
    prisonerId: String,
    visitRestriction: VisitRestriction,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    return webTestClient.get().uri("/visit-sessions/available?prisonId=$prisonCode&prisonerId=$prisonerId&visitRestriction=$visitRestriction")
      .headers(authHttpHeaders)
      .exchange()
  }

  @Test
  fun `when visit sessions for parameters are available then these sessions are returned`() {
    // Given
    val prisonCode = "MDI"
    val prisonerId = "AA123456B"
    val visitSession1 = AvailableVisitSessionDto(LocalDate.now(), SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)))
    val visitSession2 = AvailableVisitSessionDto(LocalDate.now().plusDays(1), SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)))
    val visitSession3 = AvailableVisitSessionDto(LocalDate.now().plusDays(2), SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)))

    visitSchedulerMockServer.stubGetAvailableVisitSessions(prisonCode, prisonerId, VisitRestriction.OPEN, mutableListOf(visitSession1, visitSession2, visitSession3))

    // When
    val responseSpec = callGetAvailableVisitSessions(webTestClient, prisonCode, prisonerId, VisitRestriction.OPEN, rolePVBHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(3)
  }

  @Test
  fun `when visit sessions for parameters do not exist then empty list is returned`() {
    // Given
    val prisonCode = "MDI"
    val prisonerId = "AA123456B"

    visitSchedulerMockServer.stubGetAvailableVisitSessions(prisonCode, prisonerId, VisitRestriction.OPEN, mutableListOf())

    // When
    val responseSpec = callGetAvailableVisitSessions(webTestClient, prisonCode, prisonerId, VisitRestriction.OPEN, rolePVBHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(0)
  }

  @Test
  fun `when call to visit scheduler throws 404 then same 404 error status is sent back`() {
    // Given
    val prisonCode = "MDI"
    val prisonerId = "AA123456B"

    visitSchedulerMockServer.stubGetAvailableVisitSessions(prisonCode, prisonerId, VisitRestriction.OPEN, emptyList(), HttpStatus.NOT_FOUND)

    // When
    val responseSpec = callGetAvailableVisitSessions(webTestClient, prisonCode, prisonerId, VisitRestriction.CLOSED, rolePVBHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
  }

  @Test
  fun `when call to visit scheduler called without correct role then access forbidden is returned`() {
    // Given
    val prisonCode = "MDI"
    val prisonerId = "AA123456B"
    val invalidRole = setAuthorisation(roles = listOf("ROLE_ORCHESTRATION_SERVICE__VISIT_BOOKER_REGISTRY"))

    // When
    val responseSpec = callGetAvailableVisitSessions(webTestClient, prisonCode, prisonerId, VisitRestriction.CLOSED, invalidRole)

    // Then
    responseSpec.expectStatus().isForbidden
  }

  @Test
  fun `when call to visit scheduler called without token then unauthorised status returned`() {
    // Given
    val prisonCode = "MDI"
    val prisonerId = "AA123456B"
    val visitRestriction = VisitRestriction.CLOSED

    // When
    val responseSpec = webTestClient.get().uri("/visit-sessions/available?prisonId=$prisonCode&prisonerId=$prisonerId&visitRestriction=$visitRestriction")
      .exchange()

    // Then
    responseSpec.expectStatus().isUnauthorized
  }
}
