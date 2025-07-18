package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.sessions

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitSchedulerClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.OffenderRestrictionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.OffenderRestrictionsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.AvailableVisitSessionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.DateRange
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionTimeSlotDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSchedulerPrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionRestriction.CLOSED
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionRestriction.OPEN
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType.PUBLIC
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.AppointmentsService
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.PrisonerProfileService
import java.time.LocalDate
import java.time.LocalTime

@DisplayName("Get available visit sessions without appointments check")
class AvailableVisitSessionsWithoutAppointmentsCheckTest : IntegrationTestBase() {

  @MockitoSpyBean
  private lateinit var visitSchedulerClient: VisitSchedulerClient

  @MockitoSpyBean
  private lateinit var appointmentsService: AppointmentsService

  @MockitoSpyBean
  private lateinit var prisonerProfileService: PrisonerProfileService

  @Test
  fun `when visit sessions for parameters are available then these sessions are returned`() {
    // Given
    val prisonCode = "MDI"
    val prisonerId = "AA123456B"
    val visitSession1 = AvailableVisitSessionDto(LocalDate.now(), "session1", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    val visitSession2 = AvailableVisitSessionDto(LocalDate.now().plusDays(1), "session2", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    val visitSession3 = AvailableVisitSessionDto(LocalDate.now().plusDays(2), "session3", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)

    val visitSchedulerPrisonDto = VisitSchedulerPrisonDto(prisonCode, true, 2, 28, 6, 3, 3, 18)
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), userType = PUBLIC)
    visitSchedulerMockServer.stubGetPrison(prisonCode, visitSchedulerPrisonDto)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, OffenderRestrictionsDto(offenderRestrictions = emptyList()))

    val visitorIds = listOf(1L, 2L, 3L)
    prisonerContactRegistryMockServer.stubDoVisitorsHaveClosedRestrictions(prisonerId, visitorIds = visitorIds, result = false)
    prisonerContactRegistryMockServer.stubGetBannedRestrictionDateRage(prisonerId, visitorIds = visitorIds, dateRange = dateRange, result = dateRange)

    // When
    val responseSpec = callGetAvailableVisitSessions(webTestClient, prisonCode, prisonerId, OPEN, visitorIds = visitorIds, false, userType = PUBLIC, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(3)
    verify(appointmentsService, times(0)).getHigherPriorityAppointments(any(), any(), any())
  }

  @Test
  fun `when excluded application reference is requested the request is forwarded to the client`() {
    // Given
    val prisonCode = "MDI"
    val prisonerId = "AA123456B"
    val visitSession1 = AvailableVisitSessionDto(LocalDate.now(), "session1", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)

    val visitSchedulerPrisonDto = VisitSchedulerPrisonDto(prisonCode, true, 2, 28, 6, 3, 3, 18)
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1), excludedApplicationReference = "aledTheGreat", userType = PUBLIC)
    visitSchedulerMockServer.stubGetPrison(prisonCode, visitSchedulerPrisonDto)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, OffenderRestrictionsDto(offenderRestrictions = emptyList()))

    val visitorIds = listOf(1L)
    prisonerContactRegistryMockServer.stubDoVisitorsHaveClosedRestrictions(prisonerId, visitorIds = visitorIds, result = false)
    prisonerContactRegistryMockServer.stubGetBannedRestrictionDateRage(prisonerId, visitorIds = visitorIds, dateRange = dateRange, result = dateRange)

    // When
    val responseSpec = callGetAvailableVisitSessions(webTestClient, prisonCode, prisonerId, OPEN, visitorIds = visitorIds, false, userType = PUBLIC, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders, excludedApplicationReference = "aledTheGreat")

    // Then
    responseSpec.expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(1)
  }

  @Test
  fun `when visit sessions for parameters are available and no visitors are supplied`() {
    // Given
    val prisonCode = "MDI"
    val prisonerId = "AA123456B"
    val visitSession1 = AvailableVisitSessionDto(LocalDate.now(), "s1", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    val prisonDto = VisitSchedulerPrisonDto(prisonCode, true, 2, 28, 6, 3, 3, 18)
    visitSchedulerMockServer.stubGetAvailableVisitSessions(prisonDto, prisonerId, OPEN, mutableListOf(visitSession1), userType = PUBLIC)
    visitSchedulerMockServer.stubGetPrison(prisonCode, prisonDto)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, OffenderRestrictionsDto(offenderRestrictions = emptyList()))

    // When
    val responseSpec = callGetAvailableVisitSessions(webTestClient, prisonCode, prisonerId, OPEN, withAppointmentsCheck = false, userType = PUBLIC, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(1)
    verify(appointmentsService, times(0)).getHigherPriorityAppointments(any(), any(), any())
  }

  @Test
  fun `when visit sessions for parameters are available but no dateRange is returned`() {
    // Given
    val prisonCode = "MDI"
    val prisonerId = "AA123456B"
    val visitSession1 = AvailableVisitSessionDto(LocalDate.now(), "s1", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    val visitSession2 = AvailableVisitSessionDto(LocalDate.now().plusDays(1), "s2", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    val visitSession3 = AvailableVisitSessionDto(LocalDate.now().plusDays(2), "s3", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    val prisonDto = VisitSchedulerPrisonDto(prisonCode, true, 2, 28, 6, 3, 3, 18)
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(prisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), userType = PUBLIC)
    visitSchedulerMockServer.stubGetPrison(prisonCode, prisonDto)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, OffenderRestrictionsDto(offenderRestrictions = emptyList()))
    val visitorIds = listOf(1L, 2L, 3L)
    prisonerContactRegistryMockServer.stubDoVisitorsHaveClosedRestrictions(prisonerId, visitorIds = visitorIds, result = false)
    prisonerContactRegistryMockServer.stubGetBannedRestrictionDateRage(prisonerId, visitorIds = visitorIds, dateRange = dateRange, result = null)

    // When
    val responseSpec = callGetAvailableVisitSessions(webTestClient, prisonCode, prisonerId, OPEN, visitorIds = visitorIds, withAppointmentsCheck = false, userType = PUBLIC, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(0)

    verify(prisonerProfileService, times(1)).getBannedRestrictionDateRage(any(), any(), any())
    verify(visitSchedulerClient, times(0)).getAvailableVisitSessions(any(), any(), any(), any(), any(), anyOrNull(), any())
    verify(appointmentsService, times(0)).getHigherPriorityAppointments(any(), any(), any())
  }

  @Test
  fun `when an empty visitors list is supplied a bad request error should be thrown`() {
    // Given
    val prisonCode = "MDI"
    val prisonerId = "AA123456B"
    val visitorIds = listOf<Long>()

    // When
    val responseSpec = callGetAvailableVisitSessions(webTestClient, prisonCode, prisonerId, OPEN, visitorIds = visitorIds, withAppointmentsCheck = false, userType = PUBLIC, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

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

    val visitSession1 = AvailableVisitSessionDto(LocalDate.now(), "s1", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
    val prisonDto = VisitSchedulerPrisonDto(prisonCode, true, 2, 28, 6, 3, 3, 18)

    val toDay = LocalDate.now()
    val fromDate = toDay.plusDays(prisonDto.policyNoticeDaysMin.toLong().plus(1))
    val toDate = toDay.plusDays(prisonDto.policyNoticeDaysMax.toLong())
    val dateRange = DateRange(fromDate, toDate)

    val moderatedDateRange = DateRange(toDay, toDay.plusWeeks(1))
    visitSchedulerMockServer.stubGetAvailableVisitSessions(prisonDto, prisonerId, OPEN, mutableListOf(visitSession1), dateRange = moderatedDateRange, userType = PUBLIC)

    visitSchedulerMockServer.stubGetPrison(prisonCode, prisonDto)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, OffenderRestrictionsDto(offenderRestrictions = emptyList()))

    prisonerContactRegistryMockServer.stubDoVisitorsHaveClosedRestrictions(prisonerId, visitorIds = visitorIds, result = false)
    prisonerContactRegistryMockServer.stubGetBannedRestrictionDateRage(prisonerId, visitorIds = visitorIds, dateRange = dateRange, result = moderatedDateRange)

    // When
    val responseSpec = callGetAvailableVisitSessions(webTestClient, prisonCode, prisonerId, OPEN, visitorIds = visitorIds, withAppointmentsCheck = false, userType = PUBLIC, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(1)
    verify(appointmentsService, times(0)).getHigherPriorityAppointments(any(), any(), any())
  }

  @Test
  fun `when prisoner has CLOSED restriction then these only closed sessions are returned`() {
    // Given
    val prisonCode = "MDI"
    val prisonerId = "AA123456B"
    val visitSession1 = AvailableVisitSessionDto(LocalDate.now(), "session1", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), CLOSED)

    val offenderRestrictionsDto = OffenderRestrictionsDto(
      bookingId = 1,
      offenderRestrictions = listOf(
        OffenderRestrictionDto(restrictionId = 1, restrictionType = "CLOSED", restrictionTypeDescription = "", startDate = LocalDate.now(), expiryDate = LocalDate.now(), active = true),
      ),
    )

    val visitSchedulerPrisonDto = VisitSchedulerPrisonDto(prisonCode, true, 2, 28, 6, 3, 3, 18)
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, CLOSED, mutableListOf(visitSession1), userType = PUBLIC)
    visitSchedulerMockServer.stubGetPrison(prisonCode, visitSchedulerPrisonDto)
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
      false,
      userType = PUBLIC,
      authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders,
    )

    // Then
    responseSpec.expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(1)
    verify(appointmentsService, times(0)).getHigherPriorityAppointments(any(), any(), any())
  }

  @Test
  fun `when prisoner has CLOSED restriction with no expiry date then these only closed sessions are returned`() {
    // Given
    val prisonCode = "MDI"
    val prisonerId = "AA123456B"
    val visitSession1 = AvailableVisitSessionDto(LocalDate.now(), "s1", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), CLOSED)

    val offenderRestrictionsDto = OffenderRestrictionsDto(
      bookingId = 1,
      offenderRestrictions = listOf(
        // CLOSED restriction with expiry date as NULL
        OffenderRestrictionDto(restrictionId = 1, restrictionType = "CLOSED", restrictionTypeDescription = "", startDate = LocalDate.now(), expiryDate = null, active = true),
      ),
    )

    val visitSchedulerPrisonDto = VisitSchedulerPrisonDto(prisonCode, true, 2, 28, 6, 3, 3, 18)
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, CLOSED, mutableListOf(visitSession1), userType = PUBLIC)
    visitSchedulerMockServer.stubGetPrison(prisonCode, visitSchedulerPrisonDto)
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
      false,
      userType = PUBLIC,
      authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders,
    )

    // Then
    responseSpec.expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(1)
    verify(appointmentsService, times(0)).getHigherPriorityAppointments(any(), any(), any())
  }

  @Test
  fun `when visitor has a CLOSED restriction then these only closed sessions are returned`() {
    // Given
    val prisonCode = "MDI"
    val prisonerId = "AA123456B"
    val visitSession1 = AvailableVisitSessionDto(LocalDate.now(), "s1", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), CLOSED)

    val prisonDto = VisitSchedulerPrisonDto(prisonCode, true, 2, 28, 6, 3, 3, 18)
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(prisonDto, prisonerId, CLOSED, mutableListOf(visitSession1), userType = PUBLIC)
    visitSchedulerMockServer.stubGetPrison(prisonCode, prisonDto)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, OffenderRestrictionsDto(offenderRestrictions = emptyList()))

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
      false,
      userType = PUBLIC,
      authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders,
    )

    // Then
    responseSpec.expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(1)
    verify(appointmentsService, times(0)).getHigherPriorityAppointments(any(), any(), any())
  }

  @Test
  fun `when visit sessions for parameters do not exist then empty list is returned`() {
    // Given
    val prisonCode = "MDI"
    val prisonerId = "AA123456B"

    val visitSchedulerPrisonDto = VisitSchedulerPrisonDto(prisonCode, true, 2, 28, 6, 3, 3, 18)
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(), userType = PUBLIC)
    visitSchedulerMockServer.stubGetPrison(prisonCode, visitSchedulerPrisonDto)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, OffenderRestrictionsDto(offenderRestrictions = emptyList()))

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
      false,
      userType = PUBLIC,
      authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders,
    )

    // Then
    responseSpec.expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(0)
    verify(appointmentsService, times(0)).getHigherPriorityAppointments(any(), any(), any())
  }

  @Test
  fun `when call to visit scheduler throws 404 then same 404 error status is sent back`() {
    // Given
    val prisonCode = "MDI"
    val prisonerId = "AA123456B"

    val visitSchedulerPrisonDto = VisitSchedulerPrisonDto(prisonCode, true, 2, 28, 6, 3, 3, 18)
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(), NOT_FOUND, userType = PUBLIC)
    visitSchedulerMockServer.stubGetPrison(prisonCode, visitSchedulerPrisonDto)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, OffenderRestrictionsDto(offenderRestrictions = emptyList()))

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
      false,
      userType = PUBLIC,
      authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders,
    )

    // Then
    responseSpec.expectStatus().isNotFound
    verify(visitSchedulerClient, times(1)).getAvailableVisitSessions(any(), any(), any(), any(), anyOrNull(), anyOrNull(), any())
    verify(appointmentsService, times(0)).getHigherPriorityAppointments(any(), any(), any())
  }

  @Test
  fun `when call to prisoner offender search throws 404 then same 404 error status is sent back`() {
    // Given
    val prisonCode = "MDI"
    val prisonerId = "AA123456B"

    val visitSchedulerPrisonDto = VisitSchedulerPrisonDto(prisonCode, true, 2, 28, 6, 3, 3, 18)
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(), userType = PUBLIC)
    visitSchedulerMockServer.stubGetPrison(prisonCode, visitSchedulerPrisonDto)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, null)

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
      false,
      userType = PUBLIC,
      authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders,
    )

    // Then
    responseSpec.expectStatus().isNotFound
    verify(visitSchedulerClient, times(0)).getAvailableVisitSessions(prisonCode, prisonerId, OPEN, dateRange, userType = PUBLIC)
    verify(appointmentsService, times(0)).getHigherPriorityAppointments(any(), any(), any())
  }

  @Test
  fun `when call to prisoner offender search throws INTERNAL_SERVER_ERROR then same INTERNAL_SERVER_ERROR error status is sent back`() {
    // Given
    val prisonCode = "MDI"
    val prisonerId = "AA123456B"

    val visitSchedulerPrisonDto = VisitSchedulerPrisonDto(prisonCode, true, 2, 28, 6, 3, 3, 18)
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(), userType = PUBLIC)
    visitSchedulerMockServer.stubGetPrison(prisonCode, visitSchedulerPrisonDto)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, null, INTERNAL_SERVER_ERROR)

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
      false,
      userType = PUBLIC,
      authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders,
    )

    // Then
    responseSpec.expectStatus().is5xxServerError
    verify(visitSchedulerClient, times(0)).getAvailableVisitSessions(prisonCode, prisonerId, OPEN, dateRange, userType = PUBLIC)
    verify(appointmentsService, times(0)).getHigherPriorityAppointments(any(), any(), any())
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
      false,
      userType = PUBLIC,
      authHttpHeaders = invalidRole,
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
