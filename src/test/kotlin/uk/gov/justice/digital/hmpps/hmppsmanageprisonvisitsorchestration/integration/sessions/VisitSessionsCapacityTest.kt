package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.sessions

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.SessionCapacityDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import java.time.LocalDate
import java.time.LocalTime

@DisplayName("Get visits by reference")
class VisitSessionsCapacityTest : IntegrationTestBase() {
  fun callVisitsSessionsCapacity(
    webTestClient: WebTestClient,
    prisonCode: String,
    sessionDate: LocalDate,
    sessionStartTime: LocalTime,
    sessionEndTime: LocalTime,
    sessionCapacityDto: SessionCapacityDto?,
    authHttpHeaders: (HttpHeaders) -> Unit
  ): WebTestClient.ResponseSpec {
    return webTestClient.get().uri("/visit-sessions/capacity?prisonId=$prisonCode&sessionDate=$sessionDate&sessionStartTime=$sessionStartTime&sessionEndTime=$sessionEndTime")
      .headers(authHttpHeaders)
      .exchange()
  }

  @Test
  fun `when session capacity exists open and closed counts are returned`() {
    // Given
    val prisonCode = "MDI"
    val sessionDate = LocalDate.now()
    val sessionStartTime = LocalTime.parse("13:45")
    val sessionEndTime = LocalTime.parse("14:45")
    val sessionCapacityDto = SessionCapacityDto(closed = 10, open = 20)
    visitSchedulerMockServer.stubGetSessionCapacity(prisonCode, sessionDate, sessionStartTime, sessionEndTime, sessionCapacityDto)

    // When
    val responseSpec = callVisitsSessionsCapacity(webTestClient, prisonCode, sessionDate, sessionStartTime, sessionEndTime, sessionCapacityDto, roleVisitSchedulerHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
      .expectBody()
      .jsonPath("$.closed").isEqualTo(sessionCapacityDto.closed)
      .jsonPath("$.open").isEqualTo(sessionCapacityDto.open)
  }

  @Test
  fun `when session capacity is invalid a NOT_FOUND status is returned`() {
    // Given
    val prisonCode = "MDI"
    val sessionDate = LocalDate.now()
    val sessionStartTime = LocalTime.parse("13:45")
    val sessionEndTime = LocalTime.parse("14:45")
    val sessionCapacityDto = null

    visitSchedulerMockServer.stubGetSessionCapacity(prisonCode, sessionDate, sessionStartTime, sessionEndTime, sessionCapacityDto)

    // When
    val responseSpec = callVisitsSessionsCapacity(webTestClient, prisonCode, sessionDate, sessionStartTime, sessionEndTime, sessionCapacityDto, roleVisitSchedulerHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
  }
}
