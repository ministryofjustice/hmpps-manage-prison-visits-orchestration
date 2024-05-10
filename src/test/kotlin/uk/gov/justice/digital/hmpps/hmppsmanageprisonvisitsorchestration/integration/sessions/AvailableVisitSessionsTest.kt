package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.sessions

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.OffenderRestrictionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.OffenderRestrictionsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.AvailableVisitSessionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionTimeSlotDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSchedulerPrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionRestriction
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionRestriction.CLOSED
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionRestriction.OPEN
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitRestriction
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import java.time.LocalDate
import java.time.LocalTime

@DisplayName("Get available visit sessions")
class AvailableVisitSessionsTest : IntegrationTestBase() {
  fun callGetAvailableVisitSessions(
    webTestClient: WebTestClient,
    prisonCode: String,
    prisonerId: String,
    sessionRestriction: SessionRestriction,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    return webTestClient.get().uri("/visit-sessions/available?prisonId=$prisonCode&prisonerId=$prisonerId&sessionRestriction=$sessionRestriction")
      .headers(authHttpHeaders)
      .exchange()
  }

  @Test
  fun `when visit sessions for parameters are available then these sessions are returned`() {
    // Given
    val prisonCode = "MDI"
    val prisonerId = "AA123456B"
    val visitSession1 = AvailableVisitSessionDto(LocalDate.now(), SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), VisitRestriction.OPEN)
    val visitSession2 = AvailableVisitSessionDto(LocalDate.now().plusDays(1), SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), VisitRestriction.OPEN)
    val visitSession3 = AvailableVisitSessionDto(LocalDate.now().plusDays(2), SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), VisitRestriction.OPEN)

    val visitSchedulerPrisonDto = VisitSchedulerPrisonDto(prisonCode, true, 2, 28, 6, 3, 3, 18, setOf(LocalDate.now()))
    visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3))
    visitSchedulerMockServer.stubGetPrison(prisonCode, visitSchedulerPrisonDto)
    prisonApiMockServer.stubGetRestrictions(prisonerId)

    // When
    val responseSpec = callGetAvailableVisitSessions(webTestClient, prisonCode, prisonerId, OPEN, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(3)
  }

  @Test
  fun `when prisoner has CLOSED restriction then these only closed sessions are returned`() {
    // Given
    val prisonCode = "MDI"
    val prisonerId = "AA123456B"
    val visitSession1 = AvailableVisitSessionDto(LocalDate.now(), SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), VisitRestriction.CLOSED)

    val offenderRestrictionsDto = OffenderRestrictionsDto(
      bookingId = 1,
      offenderRestrictions = listOf(
        OffenderRestrictionDto(restrictionId = 1, restrictionType = "CLOSED", restrictionTypeDescription = "", startDate = LocalDate.now(), expiryDate = LocalDate.now(), active = true),
      ),
    )

    val visitSchedulerPrisonDto = VisitSchedulerPrisonDto(prisonCode, true, 2, 28, 6, 3, 3, 18, setOf(LocalDate.now()))
    visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, CLOSED, mutableListOf(visitSession1))
    visitSchedulerMockServer.stubGetPrison(prisonCode, visitSchedulerPrisonDto)
    prisonApiMockServer.stubGetRestrictions(prisonerId, offenderRestrictionsDto)

    // When
    val responseSpec = callGetAvailableVisitSessions(webTestClient, prisonCode, prisonerId, OPEN, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(1)
  }

  @Test
  fun `when visit sessions for parameters do not exist then empty list is returned`() {
    // Given
    val prisonCode = "MDI"
    val prisonerId = "AA123456B"

    val visitSchedulerPrisonDto = VisitSchedulerPrisonDto(prisonCode, true, 2, 28, 6, 3, 3, 18, setOf(LocalDate.now()))
    visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf())
    visitSchedulerMockServer.stubGetPrison(prisonCode, visitSchedulerPrisonDto)
    prisonApiMockServer.stubGetRestrictions(prisonerId)

    // When
    val responseSpec = callGetAvailableVisitSessions(webTestClient, prisonCode, prisonerId, OPEN, roleVSIPOrchestrationServiceHttpHeaders)

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

    val visitSchedulerPrisonDto = VisitSchedulerPrisonDto(prisonCode, true, 2, 28, 6, 3, 3, 18, setOf(LocalDate.now()))
    visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(), NOT_FOUND)
    visitSchedulerMockServer.stubGetPrison(prisonCode, visitSchedulerPrisonDto)
    prisonApiMockServer.stubGetRestrictions(prisonerId)

    // When
    val responseSpec = callGetAvailableVisitSessions(webTestClient, prisonCode, prisonerId, OPEN, roleVSIPOrchestrationServiceHttpHeaders)

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
    val responseSpec = callGetAvailableVisitSessions(webTestClient, prisonCode, prisonerId, CLOSED, invalidRole)

    // Then
    responseSpec.expectStatus().isForbidden
  }

  @Test
  fun `when call to visit scheduler called without token then unauthorised status returned`() {
    // Given
    val prisonCode = "MDI"
    val prisonerId = "AA123456B"
    val sessionRestriction = CLOSED

    // When
    val responseSpec = webTestClient.get().uri("/visit-sessions/available?prisonId=$prisonCode&prisonerId=$prisonerId&sessionRestriction=$sessionRestriction")
      .exchange()

    // Then
    responseSpec.expectStatus().isUnauthorized
  }
}
