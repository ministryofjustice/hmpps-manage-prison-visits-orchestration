package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.sessions

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitSchedulerClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.OffenderRestrictionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.OffenderRestrictionsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.AvailableVisitSessionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.DateRange
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.PrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionTimeSlotDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionRestriction
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionRestriction.CLOSED
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionRestriction.OPEN
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitRestriction
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import java.time.LocalDate
import java.time.LocalTime

@DisplayName("Get available visit sessions")
class AvailableVisitSessionsTest : IntegrationTestBase() {

  @SpyBean
  private lateinit var visitSchedulerClient: VisitSchedulerClient

  fun callGetAvailableVisitSessions(
    webTestClient: WebTestClient,
    prisonCode: String,
    prisonerId: String,
    sessionRestriction: SessionRestriction,
    visitorIds: List<Long>? = null,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    val uri = visitorIds?.let {
      val visitorIdsString = it.joinToString(",")
      "/visit-sessions/available?prisonId=$prisonCode&prisonerId=$prisonerId&sessionRestriction=$sessionRestriction&visitors=$visitorIdsString"
    } ?: run {
      "/visit-sessions/available?prisonId=$prisonCode&prisonerId=$prisonerId&sessionRestriction=$sessionRestriction"
    }

    return webTestClient.get().uri(uri)
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
    val prisonDto = PrisonDto(prisonCode, true, 2, 28, 6, 3, 3, 18, setOf(LocalDate.now()))
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(prisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3))
    visitSchedulerMockServer.stubGetPrison(prisonCode, prisonDto)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId)
    val visitorIds = listOf(1L, 2L, 3L)
    prisonerContactRegistryMockServer.stubDoVisitorsHaveClosedRestrictions(prisonerId, visitorIds = visitorIds, result = false)
    prisonerContactRegistryMockServer.stubGetBannedRestrictionDateRage(prisonerId, visitorIds = visitorIds, dateRange = dateRange, result = dateRange)

    // When
    val responseSpec = callGetAvailableVisitSessions(webTestClient, prisonCode, prisonerId, OPEN, visitorIds = visitorIds, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(3)
  }

  @Test
  fun `when visit sessions for parameters are available and no visitors are supplied`() {
    // Given
    val prisonCode = "MDI"
    val prisonerId = "AA123456B"
    val visitSession1 = AvailableVisitSessionDto(LocalDate.now(), SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), VisitRestriction.OPEN)
    val prisonDto = PrisonDto(prisonCode, true, 2, 28, 6, 3, 3, 18, setOf(LocalDate.now()))
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(prisonDto, prisonerId, OPEN, mutableListOf(visitSession1))
    visitSchedulerMockServer.stubGetPrison(prisonCode, prisonDto)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId)

    // When
    val responseSpec = callGetAvailableVisitSessions(webTestClient, prisonCode, prisonerId, OPEN, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(1)
  }

  @Test
  fun `when an empty visitors list is supplied a bad request error should be thrown`() {
    // Given
    val prisonCode = "MDI"
    val prisonerId = "AA123456B"
    val visitorIds = listOf<Long>()

    // When
    val responseSpec = callGetAvailableVisitSessions(webTestClient, prisonCode, prisonerId, OPEN, visitorIds = visitorIds, roleVSIPOrchestrationServiceHttpHeaders)

    // Then

    val returnResult = responseSpec.expectStatus().isBadRequest.expectBody()
    val errorResponse = objectMapper.readValue(returnResult.returnResult().responseBody, ErrorResponse::class.java)
    assertThat(errorResponse.userMessage).isEqualTo("400 BAD_REQUEST \"Validation failure\"")
  }

  @Test
  fun `when visit sessions has a banned restriction for a visitor a new moderated date range is used`() {
    // Given
    val prisonCode = "MDI"
    val prisonerId = "AA123456B"
    val visitorIds = listOf(1L)

    val visitSession1 = AvailableVisitSessionDto(LocalDate.now(), SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), VisitRestriction.OPEN)
    val prisonDto = PrisonDto(prisonCode, true, 2, 28, 6, 3, 3, 18, setOf(LocalDate.now()))

    val toDay = LocalDate.now()
    val fromDate = toDay.plusDays(prisonDto.policyNoticeDaysMin.toLong())
    val toDate = toDay.plusDays(prisonDto.policyNoticeDaysMax.toLong())
    val dateRange = DateRange(fromDate, toDate)

    val moderatedDateRange = DateRange(toDay, toDay.plusWeeks(1))
    visitSchedulerMockServer.stubGetAvailableVisitSessions(prisonDto, prisonerId, OPEN, mutableListOf(visitSession1), dateRange = moderatedDateRange)

    visitSchedulerMockServer.stubGetPrison(prisonCode, prisonDto)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId)

    prisonerContactRegistryMockServer.stubDoVisitorsHaveClosedRestrictions(prisonerId, visitorIds = visitorIds, result = false)
    prisonerContactRegistryMockServer.stubGetBannedRestrictionDateRage(prisonerId, visitorIds = visitorIds, dateRange = dateRange, result = moderatedDateRange)

    // When
    val responseSpec = callGetAvailableVisitSessions(webTestClient, prisonCode, prisonerId, OPEN, visitorIds = visitorIds, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(1)
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

    val prisonDto = PrisonDto(prisonCode, true, 2, 28, 6, 3, 3, 18, setOf(LocalDate.now()))
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(prisonDto, prisonerId, CLOSED, mutableListOf(visitSession1))
    visitSchedulerMockServer.stubGetPrison(prisonCode, prisonDto)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, offenderRestrictionsDto)

    val visitorIds = listOf(1L, 2L, 3L)
    prisonerContactRegistryMockServer.stubDoVisitorsHaveClosedRestrictions(prisonerId, visitorIds = visitorIds, result = false)
    prisonerContactRegistryMockServer.stubGetBannedRestrictionDateRage(prisonerId, visitorIds = visitorIds, dateRange = dateRange, result = dateRange)

    // When
    val responseSpec = callGetAvailableVisitSessions(
      webTestClient,
      prisonCode,
      prisonerId,
      OPEN,
      visitorIds,
      roleVSIPOrchestrationServiceHttpHeaders,
    )

    // Then
    responseSpec.expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(1)
  }

  @Test
  fun `when visitor has a CLOSED restriction then these only closed sessions are returned`() {
    // Given
    val prisonCode = "MDI"
    val prisonerId = "AA123456B"
    val visitSession1 = AvailableVisitSessionDto(LocalDate.now(), SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), VisitRestriction.CLOSED)

    val prisonDto = PrisonDto(prisonCode, true, 2, 28, 6, 3, 3, 18, setOf(LocalDate.now()))
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(prisonDto, prisonerId, CLOSED, mutableListOf(visitSession1))
    visitSchedulerMockServer.stubGetPrison(prisonCode, prisonDto)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId)

    val visitorIds = listOf(1L, 2L, 3L)
    prisonerContactRegistryMockServer.stubDoVisitorsHaveClosedRestrictions(prisonerId, visitorIds = visitorIds, result = true)
    prisonerContactRegistryMockServer.stubGetBannedRestrictionDateRage(prisonerId, visitorIds = visitorIds, dateRange = dateRange, result = dateRange)

    // When
    val responseSpec = callGetAvailableVisitSessions(
      webTestClient,
      prisonCode,
      prisonerId,
      OPEN,
      visitorIds,
      roleVSIPOrchestrationServiceHttpHeaders,
    )

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

    val prisonDto = PrisonDto(prisonCode, true, 2, 28, 6, 3, 3, 18, setOf(LocalDate.now()))
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(prisonDto, prisonerId, OPEN, mutableListOf())
    visitSchedulerMockServer.stubGetPrison(prisonCode, prisonDto)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId)

    val visitorIds = listOf(1L, 2L, 3L)
    prisonerContactRegistryMockServer.stubDoVisitorsHaveClosedRestrictions(prisonerId, visitorIds = visitorIds, result = false)
    prisonerContactRegistryMockServer.stubGetBannedRestrictionDateRage(prisonerId, visitorIds = visitorIds, dateRange = dateRange, result = dateRange)

    // When
    val responseSpec = callGetAvailableVisitSessions(
      webTestClient,
      prisonCode,
      prisonerId,
      OPEN,
      visitorIds,
      roleVSIPOrchestrationServiceHttpHeaders,
    )

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

    val prisonDto = PrisonDto(prisonCode, true, 2, 28, 6, 3, 3, 18, setOf(LocalDate.now()))
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(prisonDto, prisonerId, OPEN, mutableListOf(), NOT_FOUND)
    visitSchedulerMockServer.stubGetPrison(prisonCode, prisonDto)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId)

    val visitorIds = listOf(1L, 2L, 3L)
    prisonerContactRegistryMockServer.stubDoVisitorsHaveClosedRestrictions(prisonerId, visitorIds = visitorIds, result = false)
    prisonerContactRegistryMockServer.stubGetBannedRestrictionDateRage(prisonerId, visitorIds = visitorIds, dateRange = dateRange, result = dateRange)

    // When
    val responseSpec = callGetAvailableVisitSessions(
      webTestClient,
      prisonCode,
      prisonerId,
      OPEN,
      visitorIds,
      roleVSIPOrchestrationServiceHttpHeaders,
    )

    // Then
    responseSpec.expectStatus().isNotFound
    verify(visitSchedulerClient, times(1)).getAvailableVisitSessions(prisonCode, prisonerId, OPEN, dateRange)
  }

  @Test
  fun `when call to visit scheduler called without correct role then access forbidden is returned`() {
    // Given
    val prisonCode = "MDI"
    val prisonerId = "AA123456B"
    val invalidRole = setAuthorisation(roles = listOf("ROLE_ORCHESTRATION_SERVICE__VISIT_BOOKER_REGISTRY"))
    val visitorIds = listOf(1L, 2L, 3L)

    // When
    val responseSpec = callGetAvailableVisitSessions(
      webTestClient,
      prisonCode,
      prisonerId,
      CLOSED,
      visitorIds,
      invalidRole,
    )

    // Then
    responseSpec.expectStatus().isForbidden
  }

  @Test
  fun `when call to visit scheduler called without token then unauthorised status returned`() {
    // Given
    val prisonCode = "MDI"
    val prisonerId = "AA123456B"
    val sessionRestriction = CLOSED
    val visitorIds = listOf(1L, 2L, 3L)

    // When
    val responseSpec = webTestClient.get().uri("/visit-sessions/available?prisonId=$prisonCode&prisonerId=$prisonerId&sessionRestriction=$sessionRestriction&visitors=$visitorIds")
      .exchange()

    // Then
    responseSpec.expectStatus().isUnauthorized
  }
}
