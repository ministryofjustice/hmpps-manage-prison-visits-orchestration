package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.sessions

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpStatus
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.AlertsApiClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonerContactRegistryClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitSchedulerClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.WhereAboutsApiClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.alerts.api.enums.PrisonerSupportedAlertCodeType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.OffenderRestrictionsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.request.review.PrisonerRestrictionsForReview
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.request.review.VisitorRestrictionsForReview
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.AvailableVisitSessionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.DateRange
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionTimeSlotDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSchedulerPrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionRestriction.OPEN
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType.PUBLIC
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import java.time.LocalDate
import java.time.LocalTime

@DisplayName("Get available visit sessions marked for review test")
class AvailableVisitSessionsForReviewTest : IntegrationTestBase() {

  @MockitoSpyBean
  private lateinit var visitSchedulerClientSpy: VisitSchedulerClient

  @MockitoSpyBean
  private lateinit var prisonApiClientSpy: PrisonApiClient

  @MockitoSpyBean
  private lateinit var prisonerContactRegistryClientSpy: PrisonerContactRegistryClient

  @MockitoSpyBean
  private lateinit var whereAboutsApiClientSpy: WhereAboutsApiClient

  @MockitoSpyBean
  private lateinit var alertsApiClientSpy: AlertsApiClient

  private val prisonCode = "MDI"
  private val prisonerId = "AA123456B"
  private val visitSession1 = AvailableVisitSessionDto(LocalDate.now().plusDays(3), "session1", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
  private val visitSession2 = AvailableVisitSessionDto(LocalDate.now().plusDays(10), "session2", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)
  private val visitSession3 = AvailableVisitSessionDto(LocalDate.now().plusDays(17), "session3", SessionTimeSlotDto(LocalTime.of(9, 0), LocalTime.of(10, 0)), OPEN)

  private val visitSchedulerPrisonDto = VisitSchedulerPrisonDto(prisonCode, true, 2, 28, 6, 3, 3, 18)
  private val visitorIds = listOf(1L, 2L, 3L)

  val visitorRestrictionsForReview = VisitorRestrictionsForReview.entries.map { it.name }

  @BeforeEach
  fun setupMocks() {
    visitSchedulerMockServer.stubGetPrison(prisonCode, visitSchedulerPrisonDto)
    prisonerContactRegistryMockServer.stubDoVisitorsHaveClosedRestrictions(prisonerId, visitorIds = visitorIds, result = false)
  }

  @Test
  fun `when there are no prisoner alerts or restrictions or visitor restrictions sessionForReview flag is set to false`() {
    // Given
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), userType = PUBLIC)
    prisonerContactRegistryMockServer.stubGetBannedRestrictionDateRage(prisonerId, visitorIds = visitorIds, dateRange = dateRange, result = dateRange)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, OffenderRestrictionsDto(offenderRestrictions = emptyList()))
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, mutableListOf())
    prisonerContactRegistryMockServer.stubGetVisitorRestrictionsDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange, emptyList())
    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, emptyList())

    // When
    val responseSpec = callGetAvailableVisitSessionsV2(webTestClient, prisonCode, prisonerId, visitorIds = visitorIds, excludedApplicationReference = null, userType = PUBLIC, userName = null, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()

    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(3)
    assertThat(availableSessions[0].sessionTemplateReference).isEqualTo(visitSession1.sessionTemplateReference)
    assertThat(availableSessions[0].sessionForReview).isFalse
    assertThat(availableSessions[1].sessionTemplateReference).isEqualTo(visitSession2.sessionTemplateReference)
    assertThat(availableSessions[1].sessionForReview).isFalse
    assertThat(availableSessions[2].sessionTemplateReference).isEqualTo(visitSession3.sessionTemplateReference)
    assertThat(availableSessions[2].sessionForReview).isFalse
    verify(prisonerContactRegistryClientSpy, times(1)).doVisitorsHaveClosedRestrictions(prisonerId, visitorIds)
    verify(prisonerContactRegistryClientSpy, times(1)).getBannedRestrictionDateRange(prisonerId, visitorIds, dateRange)
    verify(prisonerContactRegistryClientSpy, times(1)).getVisitorRestrictionDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange)
    verify(prisonApiClientSpy, times(2)).getPrisonerRestrictions(prisonerId)
    verify(alertsApiClientSpy, times(1)).getPrisonerAlerts(prisonerId)
    verify(visitSchedulerClientSpy, times(1)).getPrison(prisonCode)
    verify(visitSchedulerClientSpy, times(1)).getAvailableVisitSessions(prisonCode, prisonerId, OPEN, dateRange, null, null, PUBLIC)
    verify(whereAboutsApiClientSpy, times(1)).getEvents(prisonerId, dateRange.fromDate, dateRange.toDate)
  }

  @Test
  fun `when there are prisoner alerts (to date null) in passed date range sessionForReview flag for affected sessions is set to true`() {
    // Given
    val alert1 = createAlertResponseDto(code = PrisonerSupportedAlertCodeType.CC1.name, activeFrom = LocalDate.now(), activeTo = null)

    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), userType = PUBLIC)
    prisonerContactRegistryMockServer.stubGetBannedRestrictionDateRage(prisonerId, visitorIds = visitorIds, dateRange = dateRange, result = dateRange)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, OffenderRestrictionsDto(offenderRestrictions = emptyList()))
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, mutableListOf(alert1))
    prisonerContactRegistryMockServer.stubGetVisitorRestrictionsDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange, emptyList())
    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, emptyList())

    // When
    val responseSpec = callGetAvailableVisitSessionsV2(webTestClient, prisonCode, prisonerId, visitorIds = visitorIds, excludedApplicationReference = null, userType = PUBLIC, userName = null, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

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
    verify(prisonerContactRegistryClientSpy, times(1)).doVisitorsHaveClosedRestrictions(prisonerId, visitorIds)
    verify(prisonerContactRegistryClientSpy, times(1)).getBannedRestrictionDateRange(prisonerId, visitorIds, dateRange)
    verify(prisonerContactRegistryClientSpy, times(0)).getVisitorRestrictionDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange)
    verify(prisonApiClientSpy, times(1)).getPrisonerRestrictions(prisonerId)
    verify(alertsApiClientSpy, times(1)).getPrisonerAlerts(prisonerId)
    verify(visitSchedulerClientSpy, times(1)).getPrison(prisonCode)
    verify(visitSchedulerClientSpy, times(1)).getAvailableVisitSessions(prisonCode, prisonerId, OPEN, dateRange, null, null, PUBLIC)
    verify(whereAboutsApiClientSpy, times(1)).getEvents(prisonerId, dateRange.fromDate, dateRange.toDate)
  }

  @Test
  fun `when there are prisoner alerts that do not affect visits in passed date range sessionForReview flag for affected sessions is set to false`() {
    // Given
    // alert is not in the list that affects visits
    val alert1 = createAlertResponseDto(code = "testing", activeFrom = LocalDate.now(), activeTo = null)

    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), userType = PUBLIC)
    prisonerContactRegistryMockServer.stubGetBannedRestrictionDateRage(prisonerId, visitorIds = visitorIds, dateRange = dateRange, result = dateRange)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, OffenderRestrictionsDto(offenderRestrictions = emptyList()))
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, mutableListOf(alert1))
    prisonerContactRegistryMockServer.stubGetVisitorRestrictionsDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange, emptyList())
    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, emptyList())

    // When
    val responseSpec = callGetAvailableVisitSessionsV2(webTestClient, prisonCode, prisonerId, visitorIds = visitorIds, excludedApplicationReference = null, userType = PUBLIC, userName = null, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()

    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(3)
    assertThat(availableSessions[0].sessionTemplateReference).isEqualTo(visitSession1.sessionTemplateReference)
    assertThat(availableSessions[0].sessionForReview).isFalse
    assertThat(availableSessions[1].sessionTemplateReference).isEqualTo(visitSession2.sessionTemplateReference)
    assertThat(availableSessions[1].sessionForReview).isFalse
    assertThat(availableSessions[2].sessionTemplateReference).isEqualTo(visitSession3.sessionTemplateReference)
    assertThat(availableSessions[2].sessionForReview).isFalse
    verify(prisonerContactRegistryClientSpy, times(1)).doVisitorsHaveClosedRestrictions(prisonerId, visitorIds)
    verify(prisonerContactRegistryClientSpy, times(1)).getBannedRestrictionDateRange(prisonerId, visitorIds, dateRange)
    verify(prisonerContactRegistryClientSpy, times(1)).getVisitorRestrictionDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange)
    verify(prisonApiClientSpy, times(2)).getPrisonerRestrictions(prisonerId)
    verify(alertsApiClientSpy, times(1)).getPrisonerAlerts(prisonerId)
    verify(visitSchedulerClientSpy, times(1)).getPrison(prisonCode)
    verify(visitSchedulerClientSpy, times(1)).getAvailableVisitSessions(prisonCode, prisonerId, OPEN, dateRange, null, null, PUBLIC)
    verify(whereAboutsApiClientSpy, times(1)).getEvents(prisonerId, dateRange.fromDate, dateRange.toDate)
  }

  @Test
  fun `when there are prisoner alerts (with valid to date) in passed date range sessionForReview flag for affected sessions is set to true`() {
    // Given
    // alert ends in 3 days so only visit session 1 is affected
    val alert1 = createAlertResponseDto(code = PrisonerSupportedAlertCodeType.CC1.name, activeFrom = LocalDate.now(), activeTo = LocalDate.now().plusDays(3))

    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), userType = PUBLIC)
    prisonerContactRegistryMockServer.stubGetBannedRestrictionDateRage(prisonerId, visitorIds = visitorIds, dateRange = dateRange, result = dateRange)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, OffenderRestrictionsDto(offenderRestrictions = emptyList()))
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, mutableListOf(alert1))
    prisonerContactRegistryMockServer.stubGetVisitorRestrictionsDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange, emptyList())
    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, emptyList())

    // When
    val responseSpec = callGetAvailableVisitSessionsV2(webTestClient, prisonCode, prisonerId, visitorIds = visitorIds, excludedApplicationReference = null, userType = PUBLIC, userName = null, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()

    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(3)
    assertThat(availableSessions[0].sessionTemplateReference).isEqualTo(visitSession1.sessionTemplateReference)
    assertThat(availableSessions[0].sessionForReview).isTrue
    assertThat(availableSessions[1].sessionTemplateReference).isEqualTo(visitSession2.sessionTemplateReference)
    assertThat(availableSessions[1].sessionForReview).isFalse
    assertThat(availableSessions[2].sessionTemplateReference).isEqualTo(visitSession3.sessionTemplateReference)
    assertThat(availableSessions[2].sessionForReview).isFalse
    verify(prisonerContactRegistryClientSpy, times(1)).doVisitorsHaveClosedRestrictions(prisonerId, visitorIds)
    verify(prisonerContactRegistryClientSpy, times(1)).getBannedRestrictionDateRange(prisonerId, visitorIds, dateRange)
    verify(prisonerContactRegistryClientSpy, times(1)).getVisitorRestrictionDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange)
    verify(prisonApiClientSpy, times(2)).getPrisonerRestrictions(prisonerId)
    verify(alertsApiClientSpy, times(1)).getPrisonerAlerts(prisonerId)
    verify(visitSchedulerClientSpy, times(1)).getPrison(prisonCode)
    verify(visitSchedulerClientSpy, times(1)).getAvailableVisitSessions(prisonCode, prisonerId, OPEN, dateRange, null, null, PUBLIC)
    verify(whereAboutsApiClientSpy, times(1)).getEvents(prisonerId, dateRange.fromDate, dateRange.toDate)
  }

  @Test
  fun `when there are prisoner alerts (with from date after date range and valid to date) in passed date range sessionForReview flag for affected sessions is set to true`() {
    // Given
    // alert starts after session 1 so session 1 should not be flagged for review
    // alert ends before session 3 so session 3 should not be flagged for review
    val alert1 = createAlertResponseDto(code = PrisonerSupportedAlertCodeType.CC1.name, activeFrom = LocalDate.now().plusDays(5), activeTo = LocalDate.now().plusDays(14))

    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), userType = PUBLIC)
    prisonerContactRegistryMockServer.stubGetBannedRestrictionDateRage(prisonerId, visitorIds = visitorIds, dateRange = dateRange, result = dateRange)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, OffenderRestrictionsDto(offenderRestrictions = emptyList()))
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, mutableListOf(alert1))
    prisonerContactRegistryMockServer.stubGetVisitorRestrictionsDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange, emptyList())
    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, emptyList())

    // When
    val responseSpec = callGetAvailableVisitSessionsV2(webTestClient, prisonCode, prisonerId, visitorIds = visitorIds, excludedApplicationReference = null, userType = PUBLIC, userName = null, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()

    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(3)
    assertThat(availableSessions[0].sessionTemplateReference).isEqualTo(visitSession1.sessionTemplateReference)
    assertThat(availableSessions[0].sessionForReview).isFalse
    assertThat(availableSessions[1].sessionTemplateReference).isEqualTo(visitSession2.sessionTemplateReference)
    assertThat(availableSessions[1].sessionForReview).isTrue
    assertThat(availableSessions[2].sessionTemplateReference).isEqualTo(visitSession3.sessionTemplateReference)
    assertThat(availableSessions[2].sessionForReview).isFalse
    verify(prisonerContactRegistryClientSpy, times(1)).doVisitorsHaveClosedRestrictions(prisonerId, visitorIds)
    verify(prisonerContactRegistryClientSpy, times(1)).getBannedRestrictionDateRange(prisonerId, visitorIds, dateRange)
    verify(prisonerContactRegistryClientSpy, times(1)).getVisitorRestrictionDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange)
    verify(prisonApiClientSpy, times(2)).getPrisonerRestrictions(prisonerId)
    verify(alertsApiClientSpy, times(1)).getPrisonerAlerts(prisonerId)
    verify(visitSchedulerClientSpy, times(1)).getPrison(prisonCode)
    verify(visitSchedulerClientSpy, times(1)).getAvailableVisitSessions(prisonCode, prisonerId, OPEN, dateRange, null, null, PUBLIC)
    verify(whereAboutsApiClientSpy, times(1)).getEvents(prisonerId, dateRange.fromDate, dateRange.toDate)
  }

  @Test
  fun `when there are prisoner alerts ending starting and ending before passed date range sessionForReview flag for affected sessions is set to false`() {
    // Given
    // alert starts and ends before passed date range
    val alert1 = createAlertResponseDto(code = PrisonerSupportedAlertCodeType.CC1.name, activeFrom = LocalDate.now().minusDays(21), activeTo = LocalDate.now().plusDays(2))

    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), userType = PUBLIC)
    prisonerContactRegistryMockServer.stubGetBannedRestrictionDateRage(prisonerId, visitorIds = visitorIds, dateRange = dateRange, result = dateRange)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, OffenderRestrictionsDto(offenderRestrictions = emptyList()))
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, mutableListOf(alert1))
    prisonerContactRegistryMockServer.stubGetVisitorRestrictionsDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange, emptyList())
    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, emptyList())

    // When
    val responseSpec = callGetAvailableVisitSessionsV2(webTestClient, prisonCode, prisonerId, visitorIds = visitorIds, excludedApplicationReference = null, userType = PUBLIC, userName = null, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()

    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(3)
    assertThat(availableSessions[0].sessionTemplateReference).isEqualTo(visitSession1.sessionTemplateReference)
    assertThat(availableSessions[0].sessionForReview).isFalse
    assertThat(availableSessions[1].sessionTemplateReference).isEqualTo(visitSession2.sessionTemplateReference)
    assertThat(availableSessions[1].sessionForReview).isFalse
    assertThat(availableSessions[2].sessionTemplateReference).isEqualTo(visitSession3.sessionTemplateReference)
    assertThat(availableSessions[2].sessionForReview).isFalse
    verify(prisonerContactRegistryClientSpy, times(1)).doVisitorsHaveClosedRestrictions(prisonerId, visitorIds)
    verify(prisonerContactRegistryClientSpy, times(1)).getBannedRestrictionDateRange(prisonerId, visitorIds, dateRange)
    verify(prisonerContactRegistryClientSpy, times(1)).getVisitorRestrictionDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange)
    verify(prisonApiClientSpy, times(2)).getPrisonerRestrictions(prisonerId)
    verify(alertsApiClientSpy, times(1)).getPrisonerAlerts(prisonerId)
    verify(visitSchedulerClientSpy, times(1)).getPrison(prisonCode)
    verify(visitSchedulerClientSpy, times(1)).getAvailableVisitSessions(prisonCode, prisonerId, OPEN, dateRange, null, null, PUBLIC)
    verify(whereAboutsApiClientSpy, times(1)).getEvents(prisonerId, dateRange.fromDate, dateRange.toDate)
  }

  @Test
  fun `when there are prisoner alerts ending starting and ending after passed date range sessionForReview flag for affected sessions is set to false`() {
    // Given
    // alert starts and ends after passed date range
    val alert1 = createAlertResponseDto(code = PrisonerSupportedAlertCodeType.CC1.name, activeFrom = LocalDate.now().plusDays(21), activeTo = LocalDate.now().plusDays(42))

    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), userType = PUBLIC)
    prisonerContactRegistryMockServer.stubGetBannedRestrictionDateRage(prisonerId, visitorIds = visitorIds, dateRange = dateRange, result = dateRange)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, OffenderRestrictionsDto(offenderRestrictions = emptyList()))
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, mutableListOf(alert1))
    prisonerContactRegistryMockServer.stubGetVisitorRestrictionsDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange, emptyList())
    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, emptyList())

    // When
    val responseSpec = callGetAvailableVisitSessionsV2(webTestClient, prisonCode, prisonerId, visitorIds = visitorIds, excludedApplicationReference = null, userType = PUBLIC, userName = null, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()

    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(3)
    assertThat(availableSessions[0].sessionTemplateReference).isEqualTo(visitSession1.sessionTemplateReference)
    assertThat(availableSessions[0].sessionForReview).isFalse
    assertThat(availableSessions[1].sessionTemplateReference).isEqualTo(visitSession2.sessionTemplateReference)
    assertThat(availableSessions[1].sessionForReview).isFalse
    assertThat(availableSessions[2].sessionTemplateReference).isEqualTo(visitSession3.sessionTemplateReference)
    assertThat(availableSessions[2].sessionForReview).isFalse
    verify(prisonerContactRegistryClientSpy, times(1)).doVisitorsHaveClosedRestrictions(prisonerId, visitorIds)
    verify(prisonerContactRegistryClientSpy, times(1)).getBannedRestrictionDateRange(prisonerId, visitorIds, dateRange)
    verify(prisonerContactRegistryClientSpy, times(1)).getVisitorRestrictionDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange)
    verify(prisonApiClientSpy, times(2)).getPrisonerRestrictions(prisonerId)
    verify(alertsApiClientSpy, times(1)).getPrisonerAlerts(prisonerId)
    verify(visitSchedulerClientSpy, times(1)).getPrison(prisonCode)
    verify(visitSchedulerClientSpy, times(1)).getAvailableVisitSessions(prisonCode, prisonerId, OPEN, dateRange, null, null, PUBLIC)
    verify(whereAboutsApiClientSpy, times(1)).getEvents(prisonerId, dateRange.fromDate, dateRange.toDate)
  }

  @Test
  fun `when there are multiple prisoner alerts in passed date range sessionForReview flag for affected sessions is set to true`() {
    // Given
    // alert starts and ends after passed date range
    // alert1 affects session 1
    val alert1 = createAlertResponseDto(code = PrisonerSupportedAlertCodeType.CC1.name, activeFrom = LocalDate.now(), activeTo = LocalDate.now().plusDays(7))
    // alert2 affects session 3
    val alert2 = createAlertResponseDto(code = PrisonerSupportedAlertCodeType.CC1.name, activeFrom = LocalDate.now().plusDays(15), activeTo = LocalDate.now().plusDays(28))

    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), userType = PUBLIC)
    prisonerContactRegistryMockServer.stubGetBannedRestrictionDateRage(prisonerId, visitorIds = visitorIds, dateRange = dateRange, result = dateRange)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, OffenderRestrictionsDto(offenderRestrictions = emptyList()))
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, mutableListOf(alert1, alert2))
    prisonerContactRegistryMockServer.stubGetVisitorRestrictionsDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange, emptyList())
    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, emptyList())

    // When
    val responseSpec = callGetAvailableVisitSessionsV2(webTestClient, prisonCode, prisonerId, visitorIds = visitorIds, excludedApplicationReference = null, userType = PUBLIC, userName = null, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()

    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(3)
    assertThat(availableSessions[0].sessionTemplateReference).isEqualTo(visitSession1.sessionTemplateReference)
    assertThat(availableSessions[0].sessionForReview).isTrue
    assertThat(availableSessions[1].sessionTemplateReference).isEqualTo(visitSession2.sessionTemplateReference)
    assertThat(availableSessions[1].sessionForReview).isFalse
    assertThat(availableSessions[2].sessionTemplateReference).isEqualTo(visitSession3.sessionTemplateReference)
    assertThat(availableSessions[2].sessionForReview).isTrue
    verify(prisonerContactRegistryClientSpy, times(1)).doVisitorsHaveClosedRestrictions(prisonerId, visitorIds)
    verify(prisonerContactRegistryClientSpy, times(1)).getBannedRestrictionDateRange(prisonerId, visitorIds, dateRange)
    verify(prisonerContactRegistryClientSpy, times(1)).getVisitorRestrictionDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange)
    verify(prisonApiClientSpy, times(2)).getPrisonerRestrictions(prisonerId)
    verify(alertsApiClientSpy, times(1)).getPrisonerAlerts(prisonerId)
    verify(visitSchedulerClientSpy, times(1)).getPrison(prisonCode)
    verify(visitSchedulerClientSpy, times(1)).getAvailableVisitSessions(prisonCode, prisonerId, OPEN, dateRange, null, null, PUBLIC)
    verify(whereAboutsApiClientSpy, times(1)).getEvents(prisonerId, dateRange.fromDate, dateRange.toDate)
  }

  @Test
  fun `when there are prisoner restrictions (to date null) in passed date range sessionForReview flag for affected sessions is set to true`() {
    // Given
    val prisonerRestriction = createOffenderRestrictionDto(restrictionType = PrisonerRestrictionsForReview.RESTRICTED.name, startDate = LocalDate.now(), expiryDate = null)

    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), userType = PUBLIC)
    prisonerContactRegistryMockServer.stubGetBannedRestrictionDateRage(prisonerId, visitorIds = visitorIds, dateRange = dateRange, result = dateRange)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, OffenderRestrictionsDto(offenderRestrictions = listOf(prisonerRestriction)))
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, mutableListOf())
    prisonerContactRegistryMockServer.stubGetVisitorRestrictionsDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange, emptyList())
    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, emptyList())

    // When
    val responseSpec = callGetAvailableVisitSessionsV2(webTestClient, prisonCode, prisonerId, visitorIds = visitorIds, excludedApplicationReference = null, userType = PUBLIC, userName = null, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

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
    verify(prisonerContactRegistryClientSpy, times(1)).doVisitorsHaveClosedRestrictions(prisonerId, visitorIds)
    verify(prisonerContactRegistryClientSpy, times(1)).getBannedRestrictionDateRange(prisonerId, visitorIds, dateRange)
    verify(prisonerContactRegistryClientSpy, times(0)).getVisitorRestrictionDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange)
    verify(prisonApiClientSpy, times(2)).getPrisonerRestrictions(prisonerId)
    verify(alertsApiClientSpy, times(1)).getPrisonerAlerts(prisonerId)
    verify(visitSchedulerClientSpy, times(1)).getPrison(prisonCode)
    verify(visitSchedulerClientSpy, times(1)).getAvailableVisitSessions(prisonCode, prisonerId, OPEN, dateRange, null, null, PUBLIC)
    verify(whereAboutsApiClientSpy, times(1)).getEvents(prisonerId, dateRange.fromDate, dateRange.toDate)
  }

  @Test
  fun `when there are prisoner restrictions that do not affect visits in passed date range sessionForReview flag for affected sessions is set to false`() {
    // Given
    // prisonerRestriction is not in the list of prisoner restrictions that affect visits
    val prisonerRestriction = createOffenderRestrictionDto(restrictionType = "testing", startDate = LocalDate.now(), expiryDate = null)

    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), userType = PUBLIC)
    prisonerContactRegistryMockServer.stubGetBannedRestrictionDateRage(prisonerId, visitorIds = visitorIds, dateRange = dateRange, result = dateRange)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, OffenderRestrictionsDto(offenderRestrictions = listOf(prisonerRestriction)))
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, mutableListOf())
    prisonerContactRegistryMockServer.stubGetVisitorRestrictionsDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange, emptyList())
    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, emptyList())

    // When
    val responseSpec = callGetAvailableVisitSessionsV2(webTestClient, prisonCode, prisonerId, visitorIds = visitorIds, excludedApplicationReference = null, userType = PUBLIC, userName = null, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()

    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(3)
    assertThat(availableSessions[0].sessionTemplateReference).isEqualTo(visitSession1.sessionTemplateReference)
    assertThat(availableSessions[0].sessionForReview).isFalse
    assertThat(availableSessions[1].sessionTemplateReference).isEqualTo(visitSession2.sessionTemplateReference)
    assertThat(availableSessions[1].sessionForReview).isFalse
    assertThat(availableSessions[2].sessionTemplateReference).isEqualTo(visitSession3.sessionTemplateReference)
    assertThat(availableSessions[2].sessionForReview).isFalse
    verify(prisonerContactRegistryClientSpy, times(1)).doVisitorsHaveClosedRestrictions(prisonerId, visitorIds)
    verify(prisonerContactRegistryClientSpy, times(1)).getBannedRestrictionDateRange(prisonerId, visitorIds, dateRange)
    verify(prisonerContactRegistryClientSpy, times(1)).getVisitorRestrictionDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange)
    verify(prisonApiClientSpy, times(2)).getPrisonerRestrictions(prisonerId)
    verify(alertsApiClientSpy, times(1)).getPrisonerAlerts(prisonerId)
    verify(visitSchedulerClientSpy, times(1)).getPrison(prisonCode)
    verify(visitSchedulerClientSpy, times(1)).getAvailableVisitSessions(prisonCode, prisonerId, OPEN, dateRange, null, null, PUBLIC)
    verify(whereAboutsApiClientSpy, times(1)).getEvents(prisonerId, dateRange.fromDate, dateRange.toDate)
  }

  @Test
  fun `when there are prisoner restrictions (with valid to date) in passed date range sessionForReview flag for affected sessions is set to true`() {
    // Given
    // prisoner restriction ends in 3 days so only visit session 1 is affected
    val prisonerRestriction = createOffenderRestrictionDto(restrictionType = PrisonerRestrictionsForReview.RESTRICTED.name, startDate = LocalDate.now(), expiryDate = LocalDate.now().plusDays(3))

    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), userType = PUBLIC)
    prisonerContactRegistryMockServer.stubGetBannedRestrictionDateRage(prisonerId, visitorIds = visitorIds, dateRange = dateRange, result = dateRange)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, OffenderRestrictionsDto(offenderRestrictions = listOf(prisonerRestriction)))
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, mutableListOf())
    prisonerContactRegistryMockServer.stubGetVisitorRestrictionsDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange, emptyList())
    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, emptyList())

    // When
    val responseSpec = callGetAvailableVisitSessionsV2(webTestClient, prisonCode, prisonerId, visitorIds = visitorIds, excludedApplicationReference = null, userType = PUBLIC, userName = null, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()

    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(3)
    assertThat(availableSessions[0].sessionTemplateReference).isEqualTo(visitSession1.sessionTemplateReference)
    assertThat(availableSessions[0].sessionForReview).isTrue
    assertThat(availableSessions[1].sessionTemplateReference).isEqualTo(visitSession2.sessionTemplateReference)
    assertThat(availableSessions[1].sessionForReview).isFalse
    assertThat(availableSessions[2].sessionTemplateReference).isEqualTo(visitSession3.sessionTemplateReference)
    assertThat(availableSessions[2].sessionForReview).isFalse
    verify(prisonerContactRegistryClientSpy, times(1)).doVisitorsHaveClosedRestrictions(prisonerId, visitorIds)
    verify(prisonerContactRegistryClientSpy, times(1)).getBannedRestrictionDateRange(prisonerId, visitorIds, dateRange)
    verify(prisonerContactRegistryClientSpy, times(1)).getVisitorRestrictionDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange)
    verify(prisonApiClientSpy, times(2)).getPrisonerRestrictions(prisonerId)
    verify(alertsApiClientSpy, times(1)).getPrisonerAlerts(prisonerId)
    verify(visitSchedulerClientSpy, times(1)).getPrison(prisonCode)
    verify(visitSchedulerClientSpy, times(1)).getAvailableVisitSessions(prisonCode, prisonerId, OPEN, dateRange, null, null, PUBLIC)
    verify(whereAboutsApiClientSpy, times(1)).getEvents(prisonerId, dateRange.fromDate, dateRange.toDate)
  }

  @Test
  fun `when there are prisoner restrictions (with from date after date range and valid to date) in passed date range sessionForReview flag for affected sessions is set to true`() {
    // Given
    // prisoner restriction starts after session 1 so session 1 should not be flagged for review
    // prisoner restriction ends before session 3 so session 3 should not be flagged for review
    val prisonerRestriction = createOffenderRestrictionDto(restrictionType = PrisonerRestrictionsForReview.RESTRICTED.name, startDate = LocalDate.now().plusDays(5), expiryDate = LocalDate.now().plusDays(14))

    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), userType = PUBLIC)
    prisonerContactRegistryMockServer.stubGetBannedRestrictionDateRage(prisonerId, visitorIds = visitorIds, dateRange = dateRange, result = dateRange)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, OffenderRestrictionsDto(offenderRestrictions = listOf(prisonerRestriction)))
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, mutableListOf())
    prisonerContactRegistryMockServer.stubGetVisitorRestrictionsDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange, emptyList())
    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, emptyList())

    // When
    val responseSpec = callGetAvailableVisitSessionsV2(webTestClient, prisonCode, prisonerId, visitorIds = visitorIds, excludedApplicationReference = null, userType = PUBLIC, userName = null, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()

    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(3)
    assertThat(availableSessions[0].sessionTemplateReference).isEqualTo(visitSession1.sessionTemplateReference)
    assertThat(availableSessions[0].sessionForReview).isFalse
    assertThat(availableSessions[1].sessionTemplateReference).isEqualTo(visitSession2.sessionTemplateReference)
    assertThat(availableSessions[1].sessionForReview).isTrue
    assertThat(availableSessions[2].sessionTemplateReference).isEqualTo(visitSession3.sessionTemplateReference)
    assertThat(availableSessions[2].sessionForReview).isFalse
    verify(prisonerContactRegistryClientSpy, times(1)).doVisitorsHaveClosedRestrictions(prisonerId, visitorIds)
    verify(prisonerContactRegistryClientSpy, times(1)).getBannedRestrictionDateRange(prisonerId, visitorIds, dateRange)
    verify(prisonerContactRegistryClientSpy, times(1)).getVisitorRestrictionDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange)
    verify(prisonApiClientSpy, times(2)).getPrisonerRestrictions(prisonerId)
    verify(alertsApiClientSpy, times(1)).getPrisonerAlerts(prisonerId)
    verify(visitSchedulerClientSpy, times(1)).getPrison(prisonCode)
    verify(visitSchedulerClientSpy, times(1)).getAvailableVisitSessions(prisonCode, prisonerId, OPEN, dateRange, null, null, PUBLIC)
    verify(whereAboutsApiClientSpy, times(1)).getEvents(prisonerId, dateRange.fromDate, dateRange.toDate)
  }

  @Test
  fun `when there are prisoner restrictions ending starting and ending before passed date range sessionForReview flag for affected sessions is set to false`() {
    // Given
    // prisoner restriction starts and ends before passed date range
    val prisonerRestriction = createOffenderRestrictionDto(restrictionType = PrisonerRestrictionsForReview.RESTRICTED.name, startDate = LocalDate.now().minusDays(21), expiryDate = LocalDate.now().plusDays(2))

    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), userType = PUBLIC)
    prisonerContactRegistryMockServer.stubGetBannedRestrictionDateRage(prisonerId, visitorIds = visitorIds, dateRange = dateRange, result = dateRange)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, OffenderRestrictionsDto(offenderRestrictions = listOf(prisonerRestriction)))
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, mutableListOf())
    prisonerContactRegistryMockServer.stubGetVisitorRestrictionsDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange, emptyList())
    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, emptyList())

    // When
    val responseSpec = callGetAvailableVisitSessionsV2(webTestClient, prisonCode, prisonerId, visitorIds = visitorIds, excludedApplicationReference = null, userType = PUBLIC, userName = null, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()

    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(3)
    assertThat(availableSessions[0].sessionTemplateReference).isEqualTo(visitSession1.sessionTemplateReference)
    assertThat(availableSessions[0].sessionForReview).isFalse
    assertThat(availableSessions[1].sessionTemplateReference).isEqualTo(visitSession2.sessionTemplateReference)
    assertThat(availableSessions[1].sessionForReview).isFalse
    assertThat(availableSessions[2].sessionTemplateReference).isEqualTo(visitSession3.sessionTemplateReference)
    assertThat(availableSessions[2].sessionForReview).isFalse
    verify(prisonerContactRegistryClientSpy, times(1)).doVisitorsHaveClosedRestrictions(prisonerId, visitorIds)
    verify(prisonerContactRegistryClientSpy, times(1)).getBannedRestrictionDateRange(prisonerId, visitorIds, dateRange)
    verify(prisonerContactRegistryClientSpy, times(1)).getVisitorRestrictionDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange)
    verify(prisonApiClientSpy, times(2)).getPrisonerRestrictions(prisonerId)
    verify(alertsApiClientSpy, times(1)).getPrisonerAlerts(prisonerId)
    verify(visitSchedulerClientSpy, times(1)).getPrison(prisonCode)
    verify(visitSchedulerClientSpy, times(1)).getAvailableVisitSessions(prisonCode, prisonerId, OPEN, dateRange, null, null, PUBLIC)
    verify(whereAboutsApiClientSpy, times(1)).getEvents(prisonerId, dateRange.fromDate, dateRange.toDate)
  }

  @Test
  fun `when there are prisoner restrictions ending starting and ending after passed date range sessionForReview flag for affected sessions is set to false`() {
    // Given
    // prisoner restriction starts and ends before passed date range
    val prisonerRestriction = createOffenderRestrictionDto(restrictionType = PrisonerRestrictionsForReview.RESTRICTED.name, startDate = LocalDate.now().plusDays(21), expiryDate = LocalDate.now().plusDays(42))

    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), userType = PUBLIC)
    prisonerContactRegistryMockServer.stubGetBannedRestrictionDateRage(prisonerId, visitorIds = visitorIds, dateRange = dateRange, result = dateRange)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, OffenderRestrictionsDto(offenderRestrictions = listOf(prisonerRestriction)))
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, mutableListOf())
    prisonerContactRegistryMockServer.stubGetVisitorRestrictionsDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange, emptyList())
    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, emptyList())

    // When
    val responseSpec = callGetAvailableVisitSessionsV2(webTestClient, prisonCode, prisonerId, visitorIds = visitorIds, excludedApplicationReference = null, userType = PUBLIC, userName = null, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()

    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(3)
    assertThat(availableSessions[0].sessionTemplateReference).isEqualTo(visitSession1.sessionTemplateReference)
    assertThat(availableSessions[0].sessionForReview).isFalse
    assertThat(availableSessions[1].sessionTemplateReference).isEqualTo(visitSession2.sessionTemplateReference)
    assertThat(availableSessions[1].sessionForReview).isFalse
    assertThat(availableSessions[2].sessionTemplateReference).isEqualTo(visitSession3.sessionTemplateReference)
    assertThat(availableSessions[2].sessionForReview).isFalse
    verify(prisonerContactRegistryClientSpy, times(1)).doVisitorsHaveClosedRestrictions(prisonerId, visitorIds)
    verify(prisonerContactRegistryClientSpy, times(1)).getBannedRestrictionDateRange(prisonerId, visitorIds, dateRange)
    verify(prisonerContactRegistryClientSpy, times(1)).getVisitorRestrictionDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange)
    verify(prisonApiClientSpy, times(2)).getPrisonerRestrictions(prisonerId)
    verify(alertsApiClientSpy, times(1)).getPrisonerAlerts(prisonerId)
    verify(visitSchedulerClientSpy, times(1)).getPrison(prisonCode)
    verify(visitSchedulerClientSpy, times(1)).getAvailableVisitSessions(prisonCode, prisonerId, OPEN, dateRange, null, null, PUBLIC)
    verify(whereAboutsApiClientSpy, times(1)).getEvents(prisonerId, dateRange.fromDate, dateRange.toDate)
  }

  @Test
  fun `when there are multiple prisoner restrictions in passed date range sessionForReview flag for affected sessions is set to true`() {
    // Given
    // prisonerRestriction1 affects session 1
    val prisonerRestriction1 = createOffenderRestrictionDto(restrictionType = PrisonerRestrictionsForReview.RESTRICTED.name, startDate = LocalDate.now(), expiryDate = LocalDate.now().plusDays(7))
    // prisonerRestriction2 affects session 3
    val prisonerRestriction2 = createOffenderRestrictionDto(restrictionType = PrisonerRestrictionsForReview.CHILD.name, startDate = LocalDate.now().plusDays(15), expiryDate = LocalDate.now().plusDays(28))

    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), userType = PUBLIC)
    prisonerContactRegistryMockServer.stubGetBannedRestrictionDateRage(prisonerId, visitorIds = visitorIds, dateRange = dateRange, result = dateRange)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, OffenderRestrictionsDto(offenderRestrictions = listOf(prisonerRestriction1, prisonerRestriction2)))
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, mutableListOf())
    prisonerContactRegistryMockServer.stubGetVisitorRestrictionsDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange, emptyList())
    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, emptyList())

    // When
    val responseSpec = callGetAvailableVisitSessionsV2(webTestClient, prisonCode, prisonerId, visitorIds = visitorIds, excludedApplicationReference = null, userType = PUBLIC, userName = null, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()

    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(3)
    assertThat(availableSessions[0].sessionTemplateReference).isEqualTo(visitSession1.sessionTemplateReference)
    assertThat(availableSessions[0].sessionForReview).isTrue
    assertThat(availableSessions[1].sessionTemplateReference).isEqualTo(visitSession2.sessionTemplateReference)
    assertThat(availableSessions[1].sessionForReview).isFalse
    assertThat(availableSessions[2].sessionTemplateReference).isEqualTo(visitSession3.sessionTemplateReference)
    assertThat(availableSessions[2].sessionForReview).isTrue
    verify(prisonerContactRegistryClientSpy, times(1)).doVisitorsHaveClosedRestrictions(prisonerId, visitorIds)
    verify(prisonerContactRegistryClientSpy, times(1)).getBannedRestrictionDateRange(prisonerId, visitorIds, dateRange)
    verify(prisonerContactRegistryClientSpy, times(1)).getVisitorRestrictionDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange)
    verify(prisonApiClientSpy, times(2)).getPrisonerRestrictions(prisonerId)
    verify(alertsApiClientSpy, times(1)).getPrisonerAlerts(prisonerId)
    verify(visitSchedulerClientSpy, times(1)).getPrison(prisonCode)
    verify(visitSchedulerClientSpy, times(1)).getAvailableVisitSessions(prisonCode, prisonerId, OPEN, dateRange, null, null, PUBLIC)
    verify(whereAboutsApiClientSpy, times(1)).getEvents(prisonerId, dateRange.fromDate, dateRange.toDate)
  }

  // DVS

  @Test
  fun `when there are visitor restrictions in passed date range sessionForReview flag for affected sessions is set to true`() {
    // Given
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), userType = PUBLIC)
    val visitorRestrictionDateRanges = listOf(dateRange)
    prisonerContactRegistryMockServer.stubGetBannedRestrictionDateRage(prisonerId, visitorIds = visitorIds, dateRange = dateRange, result = dateRange)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, OffenderRestrictionsDto(offenderRestrictions = listOf()))
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, mutableListOf())
    prisonerContactRegistryMockServer.stubGetVisitorRestrictionsDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange, visitorRestrictionDateRanges)
    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, emptyList())

    // When
    val responseSpec = callGetAvailableVisitSessionsV2(webTestClient, prisonCode, prisonerId, visitorIds = visitorIds, excludedApplicationReference = null, userType = PUBLIC, userName = null, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

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
    verify(prisonerContactRegistryClientSpy, times(1)).doVisitorsHaveClosedRestrictions(prisonerId, visitorIds)
    verify(prisonerContactRegistryClientSpy, times(1)).getBannedRestrictionDateRange(prisonerId, visitorIds, dateRange)
    verify(prisonerContactRegistryClientSpy, times(1)).getVisitorRestrictionDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange)
    verify(prisonApiClientSpy, times(2)).getPrisonerRestrictions(prisonerId)
    verify(alertsApiClientSpy, times(1)).getPrisonerAlerts(prisonerId)
    verify(visitSchedulerClientSpy, times(1)).getPrison(prisonCode)
    verify(visitSchedulerClientSpy, times(1)).getAvailableVisitSessions(prisonCode, prisonerId, OPEN, dateRange, null, null, PUBLIC)
    verify(whereAboutsApiClientSpy, times(1)).getEvents(prisonerId, dateRange.fromDate, dateRange.toDate)
  }

  @Test
  fun `when there are no visitor restrictions that affect visits in passed date range sessionForReview flag for affected sessions is set to false`() {
    // Given
    val visitorRestrictionDateRanges = emptyList<DateRange>()

    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), userType = PUBLIC)
    prisonerContactRegistryMockServer.stubGetBannedRestrictionDateRage(prisonerId, visitorIds = visitorIds, dateRange = dateRange, result = dateRange)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, OffenderRestrictionsDto(offenderRestrictions = emptyList()))
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, mutableListOf())
    prisonerContactRegistryMockServer.stubGetVisitorRestrictionsDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange, visitorRestrictionDateRanges)
    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, emptyList())

    // When
    val responseSpec = callGetAvailableVisitSessionsV2(webTestClient, prisonCode, prisonerId, visitorIds = visitorIds, excludedApplicationReference = null, userType = PUBLIC, userName = null, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()

    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(3)
    assertThat(availableSessions[0].sessionTemplateReference).isEqualTo(visitSession1.sessionTemplateReference)
    assertThat(availableSessions[0].sessionForReview).isFalse
    assertThat(availableSessions[1].sessionTemplateReference).isEqualTo(visitSession2.sessionTemplateReference)
    assertThat(availableSessions[1].sessionForReview).isFalse
    assertThat(availableSessions[2].sessionTemplateReference).isEqualTo(visitSession3.sessionTemplateReference)
    assertThat(availableSessions[2].sessionForReview).isFalse
    verify(prisonerContactRegistryClientSpy, times(1)).doVisitorsHaveClosedRestrictions(prisonerId, visitorIds)
    verify(prisonerContactRegistryClientSpy, times(1)).getBannedRestrictionDateRange(prisonerId, visitorIds, dateRange)
    verify(prisonerContactRegistryClientSpy, times(1)).getVisitorRestrictionDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange)
    verify(prisonApiClientSpy, times(2)).getPrisonerRestrictions(prisonerId)
    verify(alertsApiClientSpy, times(1)).getPrisonerAlerts(prisonerId)
    verify(visitSchedulerClientSpy, times(1)).getPrison(prisonCode)
    verify(visitSchedulerClientSpy, times(1)).getAvailableVisitSessions(prisonCode, prisonerId, OPEN, dateRange, null, null, PUBLIC)
    verify(whereAboutsApiClientSpy, times(1)).getEvents(prisonerId, dateRange.fromDate, dateRange.toDate)
  }

  @Test
  fun `when there are multiple visitor restrictions in passed date range sessionForReview flag for affected sessions is set to true`() {
    // Given
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), userType = PUBLIC)

    // multiple visitor restriction date range returned
    // restriction 1 overlaps with session 1 date
    val visitorRestriction1DateRange = DateRange(LocalDate.now(), LocalDate.now().plusDays(7))

    // restriction 2 overlaps with session 3 date
    val visitorRestriction2DateRange = DateRange(LocalDate.now().plusDays(14), dateRange.toDate)
    val visitorRestrictionDateRanges = listOf(visitorRestriction1DateRange, visitorRestriction2DateRange)
    prisonerContactRegistryMockServer.stubGetBannedRestrictionDateRage(prisonerId, visitorIds = visitorIds, dateRange = dateRange, result = dateRange)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, OffenderRestrictionsDto(offenderRestrictions = listOf()))
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, mutableListOf())
    prisonerContactRegistryMockServer.stubGetVisitorRestrictionsDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange, visitorRestrictionDateRanges)
    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, emptyList())

    // When
    val responseSpec = callGetAvailableVisitSessionsV2(webTestClient, prisonCode, prisonerId, visitorIds = visitorIds, excludedApplicationReference = null, userType = PUBLIC, userName = null, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()

    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(3)
    assertThat(availableSessions[0].sessionTemplateReference).isEqualTo(visitSession1.sessionTemplateReference)
    assertThat(availableSessions[0].sessionForReview).isTrue
    assertThat(availableSessions[1].sessionTemplateReference).isEqualTo(visitSession2.sessionTemplateReference)
    assertThat(availableSessions[1].sessionForReview).isFalse
    assertThat(availableSessions[2].sessionTemplateReference).isEqualTo(visitSession3.sessionTemplateReference)
    assertThat(availableSessions[2].sessionForReview).isTrue
    verify(prisonerContactRegistryClientSpy, times(1)).doVisitorsHaveClosedRestrictions(prisonerId, visitorIds)
    verify(prisonerContactRegistryClientSpy, times(1)).getBannedRestrictionDateRange(prisonerId, visitorIds, dateRange)
    verify(prisonerContactRegistryClientSpy, times(1)).getVisitorRestrictionDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange)
    verify(prisonApiClientSpy, times(2)).getPrisonerRestrictions(prisonerId)
    verify(alertsApiClientSpy, times(1)).getPrisonerAlerts(prisonerId)
    verify(visitSchedulerClientSpy, times(1)).getPrison(prisonCode)
    verify(visitSchedulerClientSpy, times(1)).getAvailableVisitSessions(prisonCode, prisonerId, OPEN, dateRange, null, null, PUBLIC)
    verify(whereAboutsApiClientSpy, times(1)).getEvents(prisonerId, dateRange.fromDate, dateRange.toDate)
  }

  @Test
  fun `when there are no available sessions no calls are made for alerts, prisoner or visitor restrictions or whereabouts`() {
    // Given
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(), userType = PUBLIC)
    prisonerContactRegistryMockServer.stubGetBannedRestrictionDateRage(prisonerId, visitorIds = visitorIds, dateRange = dateRange, result = dateRange)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, OffenderRestrictionsDto(offenderRestrictions = emptyList()))
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, mutableListOf())
    prisonerContactRegistryMockServer.stubGetVisitorRestrictionsDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange, emptyList())
    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, emptyList())

    // When
    val responseSpec = callGetAvailableVisitSessionsV2(webTestClient, prisonCode, prisonerId, visitorIds = visitorIds, excludedApplicationReference = null, userType = PUBLIC, userName = null, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()

    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(0)
    verify(prisonerContactRegistryClientSpy, times(1)).doVisitorsHaveClosedRestrictions(prisonerId, visitorIds)
    verify(prisonerContactRegistryClientSpy, times(1)).getBannedRestrictionDateRange(prisonerId, visitorIds, dateRange)
    verify(prisonerContactRegistryClientSpy, times(0)).getVisitorRestrictionDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange)
    verify(prisonApiClientSpy, times(1)).getPrisonerRestrictions(prisonerId)
    verify(alertsApiClientSpy, times(0)).getPrisonerAlerts(prisonerId)
    verify(visitSchedulerClientSpy, times(1)).getPrison(prisonCode)
    verify(visitSchedulerClientSpy, times(1)).getAvailableVisitSessions(prisonCode, prisonerId, OPEN, dateRange, null, null, PUBLIC)
    verify(whereAboutsApiClientSpy, times(0)).getEvents(prisonerId, dateRange.fromDate, dateRange.toDate)
  }

  @Test
  fun `when call to alerts API throws NOT_FOUND then all sessions are sent back with sessionForReview flag set to false`() {
    // Given
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, null, HttpStatus.NOT_FOUND)
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), userType = PUBLIC)

    prisonerContactRegistryMockServer.stubGetBannedRestrictionDateRage(prisonerId, visitorIds = visitorIds, dateRange = dateRange, result = dateRange)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, OffenderRestrictionsDto(offenderRestrictions = listOf()))
    prisonerContactRegistryMockServer.stubGetVisitorRestrictionsDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange, emptyList())
    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, emptyList())

    // When
    val responseSpec = callGetAvailableVisitSessionsV2(webTestClient, prisonCode, prisonerId, visitorIds = visitorIds, excludedApplicationReference = null, userType = PUBLIC, userName = null, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(3)
    assertThat(availableSessions[0].sessionTemplateReference).isEqualTo(visitSession1.sessionTemplateReference)
    assertThat(availableSessions[0].sessionForReview).isFalse
    assertThat(availableSessions[1].sessionTemplateReference).isEqualTo(visitSession2.sessionTemplateReference)
    assertThat(availableSessions[1].sessionForReview).isFalse
    assertThat(availableSessions[2].sessionTemplateReference).isEqualTo(visitSession3.sessionTemplateReference)
    assertThat(availableSessions[2].sessionForReview).isFalse

    verify(prisonerContactRegistryClientSpy, times(1)).doVisitorsHaveClosedRestrictions(prisonerId, visitorIds)
    verify(prisonerContactRegistryClientSpy, times(1)).getBannedRestrictionDateRange(prisonerId, visitorIds, dateRange)
    verify(prisonerContactRegistryClientSpy, times(1)).getVisitorRestrictionDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange)
    verify(prisonApiClientSpy, times(2)).getPrisonerRestrictions(prisonerId)
    verify(alertsApiClientSpy, times(1)).getPrisonerAlerts(prisonerId)
    verify(visitSchedulerClientSpy, times(1)).getPrison(prisonCode)
    verify(visitSchedulerClientSpy, times(1)).getAvailableVisitSessions(prisonCode, prisonerId, OPEN, dateRange, null, null, PUBLIC)
    verify(whereAboutsApiClientSpy, times(1)).getEvents(prisonerId, dateRange.fromDate, dateRange.toDate)
  }

  @Test
  fun `when call to alerts API throws INTERNAL_SERVER_ERROR then all sessions are sent back with sessionForReview flag set to false`() {
    // Given
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, null, HttpStatus.INTERNAL_SERVER_ERROR)
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), userType = PUBLIC)

    prisonerContactRegistryMockServer.stubGetBannedRestrictionDateRage(prisonerId, visitorIds = visitorIds, dateRange = dateRange, result = dateRange)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, OffenderRestrictionsDto(offenderRestrictions = listOf()))
    prisonerContactRegistryMockServer.stubGetVisitorRestrictionsDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange, emptyList())
    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, emptyList())

    // When
    val responseSpec = callGetAvailableVisitSessionsV2(webTestClient, prisonCode, prisonerId, visitorIds = visitorIds, excludedApplicationReference = null, userType = PUBLIC, userName = null, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(3)
    assertThat(availableSessions[0].sessionTemplateReference).isEqualTo(visitSession1.sessionTemplateReference)
    assertThat(availableSessions[0].sessionForReview).isFalse
    assertThat(availableSessions[1].sessionTemplateReference).isEqualTo(visitSession2.sessionTemplateReference)
    assertThat(availableSessions[1].sessionForReview).isFalse
    assertThat(availableSessions[2].sessionTemplateReference).isEqualTo(visitSession3.sessionTemplateReference)
    assertThat(availableSessions[2].sessionForReview).isFalse

    verify(prisonerContactRegistryClientSpy, times(1)).doVisitorsHaveClosedRestrictions(prisonerId, visitorIds)
    verify(prisonerContactRegistryClientSpy, times(1)).getBannedRestrictionDateRange(prisonerId, visitorIds, dateRange)
    verify(prisonerContactRegistryClientSpy, times(1)).getVisitorRestrictionDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange)
    verify(prisonApiClientSpy, times(2)).getPrisonerRestrictions(prisonerId)
    verify(alertsApiClientSpy, times(1)).getPrisonerAlerts(prisonerId)
    verify(visitSchedulerClientSpy, times(1)).getPrison(prisonCode)
    verify(visitSchedulerClientSpy, times(1)).getAvailableVisitSessions(prisonCode, prisonerId, OPEN, dateRange, null, null, PUBLIC)
    verify(whereAboutsApiClientSpy, times(1)).getEvents(prisonerId, dateRange.fromDate, dateRange.toDate)
  }

  @Test
  fun `when call to get prisoner restrictions throws NOT_FOUND then all sessions are sent back with sessionForReview flag set to false`() {
    // Given
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, null, HttpStatus.NOT_FOUND)
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), userType = PUBLIC)

    prisonerContactRegistryMockServer.stubGetBannedRestrictionDateRage(prisonerId, visitorIds = visitorIds, dateRange = dateRange, result = dateRange)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, OffenderRestrictionsDto(offenderRestrictions = listOf()))
    prisonerContactRegistryMockServer.stubGetVisitorRestrictionsDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange, emptyList())
    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, emptyList())
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, mutableListOf())

    // When
    val responseSpec = callGetAvailableVisitSessionsV2(webTestClient, prisonCode, prisonerId, visitorIds = visitorIds, excludedApplicationReference = null, userType = PUBLIC, userName = null, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(3)
    assertThat(availableSessions[0].sessionTemplateReference).isEqualTo(visitSession1.sessionTemplateReference)
    assertThat(availableSessions[0].sessionForReview).isFalse
    assertThat(availableSessions[1].sessionTemplateReference).isEqualTo(visitSession2.sessionTemplateReference)
    assertThat(availableSessions[1].sessionForReview).isFalse
    assertThat(availableSessions[2].sessionTemplateReference).isEqualTo(visitSession3.sessionTemplateReference)
    assertThat(availableSessions[2].sessionForReview).isFalse

    verify(prisonerContactRegistryClientSpy, times(1)).doVisitorsHaveClosedRestrictions(prisonerId, visitorIds)
    verify(prisonerContactRegistryClientSpy, times(1)).getBannedRestrictionDateRange(prisonerId, visitorIds, dateRange)
    verify(prisonerContactRegistryClientSpy, times(1)).getVisitorRestrictionDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange)
    verify(prisonApiClientSpy, times(2)).getPrisonerRestrictions(prisonerId)
    verify(alertsApiClientSpy, times(1)).getPrisonerAlerts(prisonerId)
    verify(visitSchedulerClientSpy, times(1)).getPrison(prisonCode)
    verify(visitSchedulerClientSpy, times(1)).getAvailableVisitSessions(prisonCode, prisonerId, OPEN, dateRange, null, null, PUBLIC)
    verify(whereAboutsApiClientSpy, times(1)).getEvents(prisonerId, dateRange.fromDate, dateRange.toDate)
  }

  @Test
  fun `when call to prisoner restrictions throws INTERNAL_SERVER_ERROR then all sessions are sent back with sessionForReview flag set to false`() {
    // Given
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, null, HttpStatus.INTERNAL_SERVER_ERROR)
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), userType = PUBLIC)

    prisonerContactRegistryMockServer.stubGetBannedRestrictionDateRage(prisonerId, visitorIds = visitorIds, dateRange = dateRange, result = dateRange)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, OffenderRestrictionsDto(offenderRestrictions = listOf()))
    prisonerContactRegistryMockServer.stubGetVisitorRestrictionsDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange, emptyList())
    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, emptyList())

    // When
    val responseSpec = callGetAvailableVisitSessionsV2(webTestClient, prisonCode, prisonerId, visitorIds = visitorIds, excludedApplicationReference = null, userType = PUBLIC, userName = null, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(3)
    assertThat(availableSessions[0].sessionTemplateReference).isEqualTo(visitSession1.sessionTemplateReference)
    assertThat(availableSessions[0].sessionForReview).isFalse
    assertThat(availableSessions[1].sessionTemplateReference).isEqualTo(visitSession2.sessionTemplateReference)
    assertThat(availableSessions[1].sessionForReview).isFalse
    assertThat(availableSessions[2].sessionTemplateReference).isEqualTo(visitSession3.sessionTemplateReference)
    assertThat(availableSessions[2].sessionForReview).isFalse

    verify(prisonerContactRegistryClientSpy, times(1)).doVisitorsHaveClosedRestrictions(prisonerId, visitorIds)
    verify(prisonerContactRegistryClientSpy, times(1)).getBannedRestrictionDateRange(prisonerId, visitorIds, dateRange)
    verify(prisonerContactRegistryClientSpy, times(1)).getVisitorRestrictionDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange)
    verify(prisonApiClientSpy, times(2)).getPrisonerRestrictions(prisonerId)
    verify(alertsApiClientSpy, times(1)).getPrisonerAlerts(prisonerId)
    verify(visitSchedulerClientSpy, times(1)).getPrison(prisonCode)
    verify(visitSchedulerClientSpy, times(1)).getAvailableVisitSessions(prisonCode, prisonerId, OPEN, dateRange, null, null, PUBLIC)
    verify(whereAboutsApiClientSpy, times(1)).getEvents(prisonerId, dateRange.fromDate, dateRange.toDate)
  }

  @Test
  fun `when call to get visitor restrictions throws NOT_FOUND then all sessions are sent back with sessionForReview flag set to false`() {
    // Given
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), userType = PUBLIC)

    // get visitor restriction date ranges throws 404
    prisonerContactRegistryMockServer.stubGetVisitorRestrictionsDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange, null, HttpStatus.NOT_FOUND)

    prisonerContactRegistryMockServer.stubGetBannedRestrictionDateRage(prisonerId, visitorIds = visitorIds, dateRange = dateRange, result = dateRange)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, OffenderRestrictionsDto(offenderRestrictions = listOf()))
    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, emptyList())
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, mutableListOf())

    // When
    val responseSpec = callGetAvailableVisitSessionsV2(webTestClient, prisonCode, prisonerId, visitorIds = visitorIds, excludedApplicationReference = null, userType = PUBLIC, userName = null, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(3)
    assertThat(availableSessions[0].sessionTemplateReference).isEqualTo(visitSession1.sessionTemplateReference)
    assertThat(availableSessions[0].sessionForReview).isFalse
    assertThat(availableSessions[1].sessionTemplateReference).isEqualTo(visitSession2.sessionTemplateReference)
    assertThat(availableSessions[1].sessionForReview).isFalse
    assertThat(availableSessions[2].sessionTemplateReference).isEqualTo(visitSession3.sessionTemplateReference)
    assertThat(availableSessions[2].sessionForReview).isFalse

    verify(prisonerContactRegistryClientSpy, times(1)).doVisitorsHaveClosedRestrictions(prisonerId, visitorIds)
    verify(prisonerContactRegistryClientSpy, times(1)).getBannedRestrictionDateRange(prisonerId, visitorIds, dateRange)
    verify(prisonerContactRegistryClientSpy, times(1)).getVisitorRestrictionDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange)
    verify(prisonApiClientSpy, times(2)).getPrisonerRestrictions(prisonerId)
    verify(alertsApiClientSpy, times(1)).getPrisonerAlerts(prisonerId)
    verify(visitSchedulerClientSpy, times(1)).getPrison(prisonCode)
    verify(visitSchedulerClientSpy, times(1)).getAvailableVisitSessions(prisonCode, prisonerId, OPEN, dateRange, null, null, PUBLIC)
    verify(whereAboutsApiClientSpy, times(1)).getEvents(prisonerId, dateRange.fromDate, dateRange.toDate)
  }

  @Test
  fun `when call to visitor restrictions throws INTERNAL_SERVER_ERROR then all sessions are sent back with sessionForReview flag set to false`() {
    // Given
    val dateRange = visitSchedulerMockServer.stubGetAvailableVisitSessions(visitSchedulerPrisonDto, prisonerId, OPEN, mutableListOf(visitSession1, visitSession2, visitSession3), userType = PUBLIC)
    // get visitor restriction date ranges throws 500
    prisonerContactRegistryMockServer.stubGetVisitorRestrictionsDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange, null, HttpStatus.INTERNAL_SERVER_ERROR)

    prisonerContactRegistryMockServer.stubGetBannedRestrictionDateRage(prisonerId, visitorIds = visitorIds, dateRange = dateRange, result = dateRange)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, OffenderRestrictionsDto(offenderRestrictions = listOf()))
    prisonerContactRegistryMockServer.stubGetVisitorRestrictionsDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange, emptyList())
    whereaboutsApiMockServer.stubGetEvents(prisonerId, dateRange.fromDate, dateRange.toDate, emptyList())

    // When
    val responseSpec = callGetAvailableVisitSessionsV2(webTestClient, prisonCode, prisonerId, visitorIds = visitorIds, excludedApplicationReference = null, userType = PUBLIC, userName = null, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val availableSessions = getResults(returnResult)
    assertThat(availableSessions.size).isEqualTo(3)
    assertThat(availableSessions[0].sessionTemplateReference).isEqualTo(visitSession1.sessionTemplateReference)
    assertThat(availableSessions[0].sessionForReview).isFalse
    assertThat(availableSessions[1].sessionTemplateReference).isEqualTo(visitSession2.sessionTemplateReference)
    assertThat(availableSessions[1].sessionForReview).isFalse
    assertThat(availableSessions[2].sessionTemplateReference).isEqualTo(visitSession3.sessionTemplateReference)
    assertThat(availableSessions[2].sessionForReview).isFalse

    verify(prisonerContactRegistryClientSpy, times(1)).doVisitorsHaveClosedRestrictions(prisonerId, visitorIds)
    verify(prisonerContactRegistryClientSpy, times(1)).getBannedRestrictionDateRange(prisonerId, visitorIds, dateRange)
    verify(prisonerContactRegistryClientSpy, times(1)).getVisitorRestrictionDateRanges(prisonerId, visitorIds, visitorRestrictionsForReview, dateRange)
    verify(prisonApiClientSpy, times(2)).getPrisonerRestrictions(prisonerId)
    verify(alertsApiClientSpy, times(1)).getPrisonerAlerts(prisonerId)
    verify(visitSchedulerClientSpy, times(1)).getPrison(prisonCode)
    verify(visitSchedulerClientSpy, times(1)).getAvailableVisitSessions(prisonCode, prisonerId, OPEN, dateRange, null, null, PUBLIC)
    verify(whereAboutsApiClientSpy, times(1)).getEvents(prisonerId, dateRange.fromDate, dateRange.toDate)
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): Array<AvailableVisitSessionDto> = objectMapper.readValue(returnResult.returnResult().responseBody, Array<AvailableVisitSessionDto>::class.java)
}
