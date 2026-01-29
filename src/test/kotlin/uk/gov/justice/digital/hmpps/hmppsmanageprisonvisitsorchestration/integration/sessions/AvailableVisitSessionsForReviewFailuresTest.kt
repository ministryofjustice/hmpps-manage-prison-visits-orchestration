package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.sessions

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpStatus
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.WhereAboutsApiClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.request.review.VisitorRestrictionsForReview
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.AvailableVisitSessionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionTimeSlotDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSchedulerPrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionRestriction.OPEN
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType.PUBLIC
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.TestObjectMapper
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.PrisonerProfileService
import java.time.LocalDate
import java.time.LocalTime

// tests have been moved here as prisonerProfileService needs to be mocked as we do 2 calls to get prisoner restrictions
// but only the 2nd call needs to fail
@DisplayName("Get available visit sessions marked for review test when prison API call to get prisoner restrictions returns an error")
class AvailableVisitSessionsForReviewFailuresTest : IntegrationTestBase() {

  @MockitoSpyBean
  private lateinit var whereAboutsApiClientSpy: WhereAboutsApiClient

  @MockitoBean
  private lateinit var prisonerProfileService: PrisonerProfileService

  private val prisonCode = "MDI"
  private val prisonerId = "AA123456B"
  val tomorrow: LocalDate = LocalDate.now().plusDays(1)
  private val visitSession1 = AvailableVisitSessionDto(tomorrow.plusDays(7), "session1", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
  private val visitSession2 = AvailableVisitSessionDto(tomorrow.plusDays(14), "session2", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
  private val visitSession3 = AvailableVisitSessionDto(tomorrow.plusDays(21), "session3", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)

  private val visitSchedulerPrisonDto = VisitSchedulerPrisonDto(prisonCode, true, 2, 28, 6, 3, 3, 18)
  private val visitorIds = listOf(1L, 2L, 3L)

  val visitorRestrictionsForReview = VisitorRestrictionsForReview.entries.map { it.name }

  @BeforeEach
  fun setupMocks() {
    visitSchedulerMockServer.stubGetPrison(prisonCode, visitSchedulerPrisonDto)
    prisonerContactRegistryMockServer.stubDoVisitorsHaveClosedRestrictions(prisonerId, visitorIds = visitorIds, result = false)
  }

  @Test
  fun `when call to get prisoner restrictions throws NOT_FOUND then all sessions are sent back with sessionForReview flag set to true`() {
    // Given
    // fails to get prisoner restrictions so all sessions should be marked for review
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, null, HttpStatus.NOT_FOUND)
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, visitSessions = mutableListOf(visitSession1, visitSession2, visitSession3), userType = PUBLIC)

    prisonerContactRegistryMockServer.stubGetBannedRestrictionDateRage(prisonerId, visitorIds = visitorIds, dateRange = dateRange, result = dateRange)
    prisonerContactRegistryMockServer.stubGetVisitorRestrictionsDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange, emptyList())
    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, emptyList())
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, mutableListOf())
    Mockito.`when`(prisonerProfileService.hasPrisonerGotClosedRestrictions(prisonerId)).thenReturn(false)
    // When
    val responseSpec = callGetAvailableVisitSessionsPublic(webTestClient, prisonCode, prisonerId, visitorIds = visitorIds, excludedApplicationReference = null, userType = PUBLIC, userName = null, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(3)
    assertThat(availableSessions[0].sessionTemplateReference).isEqualTo(visitSession1.sessionTemplateReference)
    assertThat(availableSessions[0].sessionForReview).isTrue
    assertThat(availableSessions[1].sessionTemplateReference).isEqualTo(visitSession2.sessionTemplateReference)
    assertThat(availableSessions[1].sessionForReview).isTrue
    assertThat(availableSessions[2].sessionTemplateReference).isEqualTo(visitSession3.sessionTemplateReference)
    assertThat(availableSessions[2].sessionForReview).isTrue

    verify(alertsApiClientSpy, times(1)).getPrisonerAlerts(prisonerId)
    verify(visitSchedulerClientSpy, times(1)).getPrison(prisonCode)
    verify(visitSchedulerClientSpy, times(1)).getAvailableVisitSessions(prisonCode, prisonerId, OPEN, dateRange, null, null, PUBLIC)
    verify(whereAboutsApiClientSpy, times(1)).getEvents(prisonerId, dateRange.fromDate, dateRange.toDate)
  }

  @Test
  fun `when call to prisoner restrictions throws INTERNAL_SERVER_ERROR then all sessions are sent back with sessionForReview flag set to true`() {
    // Given

    // fails to get prisoner restrictions so all sessions should be marked for review
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, null, HttpStatus.INTERNAL_SERVER_ERROR)
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), userType = PUBLIC)

    prisonerContactRegistryMockServer.stubGetBannedRestrictionDateRage(prisonerId, visitorIds = visitorIds, dateRange = dateRange, result = dateRange)
    prisonerContactRegistryMockServer.stubGetVisitorRestrictionsDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange, emptyList())
    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, emptyList())
    Mockito.`when`(prisonerProfileService.hasPrisonerGotClosedRestrictions(prisonerId)).thenReturn(false)

    // When
    val responseSpec = callGetAvailableVisitSessionsPublic(webTestClient, prisonCode, prisonerId, visitorIds = visitorIds, excludedApplicationReference = null, userType = PUBLIC, userName = null, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(3)
    assertThat(availableSessions[0].sessionTemplateReference).isEqualTo(visitSession1.sessionTemplateReference)
    assertThat(availableSessions[0].sessionForReview).isTrue
    assertThat(availableSessions[1].sessionTemplateReference).isEqualTo(visitSession2.sessionTemplateReference)
    assertThat(availableSessions[1].sessionForReview).isTrue
    assertThat(availableSessions[2].sessionTemplateReference).isEqualTo(visitSession3.sessionTemplateReference)
    assertThat(availableSessions[2].sessionForReview).isTrue

    verify(alertsApiClientSpy, times(1)).getPrisonerAlerts(prisonerId)
    verify(visitSchedulerClientSpy, times(1)).getPrison(prisonCode)
    verify(visitSchedulerClientSpy, times(1)).getAvailableVisitSessions(prisonCode, prisonerId, OPEN, dateRange, null, null, PUBLIC)
    verify(whereAboutsApiClientSpy, times(1)).getEvents(prisonerId, dateRange.fromDate, dateRange.toDate)
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): Array<AvailableVisitSessionDto> = TestObjectMapper.mapper.readValue(returnResult.returnResult().responseBody, Array<AvailableVisitSessionDto>::class.java)
}
