package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.sessions

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionCapacityDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionDateRangeDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionScheduleDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionTimeSlotDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.TestObjectMapper
import java.time.LocalDate
import java.time.LocalTime

@DisplayName("Get visits by reference")
class VisitSessionsScheduleTest : IntegrationTestBase() {
  val prisonCode = "MDI"
  val sessionDate: LocalDate = LocalDate.now().plusDays(1)

  lateinit var sessionScheduleDto1: SessionScheduleDto
  lateinit var sessionScheduleDto2: SessionScheduleDto

  @BeforeEach
  fun setupData() {
    sessionScheduleDto1 = createSessionScheduleDto(
      reference = "reference-1",
      startTime = LocalTime.of(9, 0),
      endTime = LocalTime.of(10, 0),
      prisonerLocationGroupNames = listOf("Location Group 1", "Location Group 2"),
      prisonerCategoryGroupNames = listOf("Category Group 1", "Category Group 2", "Category Group 3"),
      prisonerIncentiveLevelGroupNames = listOf("Incentive Group 1", "Incentive Group 2", "Incentive Group 3", "Incentive Group 4"),
      validFromDate = sessionDate.minusWeeks(1),
      validToDate = sessionDate.plusWeeks(2),
      areLocationGroupsInclusive = true,
      areCategoryGroupsInclusive = true,
      areIncentiveGroupsInclusive = true,
      visitRoom = "Visit Room 1",
      isSessionExcluded = false,
    )

    sessionScheduleDto2 = createSessionScheduleDto(
      reference = "reference-2",
      startTime = LocalTime.of(10, 0),
      endTime = LocalTime.of(11, 0),
      validFromDate = sessionDate.minusWeeks(2),
      areLocationGroupsInclusive = false,
      areCategoryGroupsInclusive = false,
      areIncentiveGroupsInclusive = false,
      visitRoom = "Visit Room 2",
      isSessionExcluded = true,
    )
  }

  @Test
  fun `when multiple session schedules exist for a prison and includeExcludedSessions is not passed no excluded session schedules are returned`() {
    // Given
    val includeExcludedSessions = null
    visitSchedulerMockServer.stubGetSessionSchedule(
      prisonCode,
      sessionDate,
      mutableListOf(sessionScheduleDto1, sessionScheduleDto2),
    )

    // When
    val responseSpec = callVisitsSessionsSchedule(webTestClient = webTestClient, prisonCode = prisonCode, sessionDate = sessionDate, includeExcludedSessions = includeExcludedSessions, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val sessionScheduleResults = getResults(returnResult)
    assertThat(sessionScheduleResults.size).isEqualTo(1)
    // only isSessionExcluded false is returned
    assertSessionSchedule(sessionSchedule = sessionScheduleResults[0], expectedSessionScheduleDto = sessionScheduleDto1)
  }

  @Test
  fun `when multiple session schedules exist for a prison and includeExcludedSessions is passed as true all session schedules are returned`() {
    // Given
    val includeExcludedSessions = true
    visitSchedulerMockServer.stubGetSessionSchedule(
      prisonCode,
      sessionDate,
      mutableListOf(sessionScheduleDto1, sessionScheduleDto2),
    )

    // When
    val responseSpec = callVisitsSessionsSchedule(webTestClient = webTestClient, prisonCode = prisonCode, sessionDate = sessionDate, includeExcludedSessions = includeExcludedSessions, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val sessionScheduleResults = getResults(returnResult)
    assertThat(sessionScheduleResults.size).isEqualTo(2)
    assertSessionSchedule(sessionSchedule = sessionScheduleResults[0], expectedSessionScheduleDto = sessionScheduleDto1)
    assertSessionSchedule(sessionSchedule = sessionScheduleResults[1], expectedSessionScheduleDto = sessionScheduleDto2)
  }

  @Test
  fun `when multiple session schedules exist for a prison and includeExcludedSessions is passed as false excluded session schedules for the date are not returned`() {
    // Given
    val includeExcludedSessions = false
    visitSchedulerMockServer.stubGetSessionSchedule(
      prisonCode,
      sessionDate,
      mutableListOf(sessionScheduleDto1, sessionScheduleDto2),
    )

    // When
    val responseSpec = callVisitsSessionsSchedule(webTestClient = webTestClient, prisonCode = prisonCode, sessionDate = sessionDate, includeExcludedSessions = includeExcludedSessions, authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val sessionScheduleResults = getResults(returnResult)

    // only 1 session should be returned
    assertThat(sessionScheduleResults.size).isEqualTo(1)
    assertSessionSchedule(sessionSchedule = sessionScheduleResults[0], expectedSessionScheduleDto = sessionScheduleDto1)
  }

  @Test
  fun `when no session schedules exist for a prison no session schedules are returned`() {
    // Given
    val prisonCode = "MDI"
    val sessionDate = LocalDate.now()

    visitSchedulerMockServer.stubGetSessionSchedule(prisonCode, sessionDate, mutableListOf())

    // When
    val responseSpec = callVisitsSessionsSchedule(
      webTestClient = webTestClient,
      prisonCode = prisonCode,
      sessionDate = sessionDate,
      includeExcludedSessions = null,
      authHttpHeaders = roleVSIPOrchestrationServiceHttpHeaders,
    )

    // Then
    responseSpec.expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(0)
  }

  private fun callVisitsSessionsSchedule(
    webTestClient: WebTestClient,
    prisonCode: String,
    sessionDate: LocalDate,
    includeExcludedSessions: Boolean?,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    var uri = "/visit-sessions/schedule?prisonId=$prisonCode&date=$sessionDate"
    includeExcludedSessions?.let {
      uri += "&includeExcludedSessions=$includeExcludedSessions"
    }
    println(uri)
    return webTestClient.get().uri(uri)
      .headers(authHttpHeaders)
      .exchange()
  }

  private fun assertSessionSchedule(
    sessionSchedule: SessionScheduleDto,
    expectedSessionScheduleDto: SessionScheduleDto,
  ) {
    assertThat(sessionSchedule.sessionTemplateReference).isEqualTo(expectedSessionScheduleDto.sessionTemplateReference)
    assertThat(sessionSchedule.sessionTimeSlot.startTime).isEqualTo(expectedSessionScheduleDto.sessionTimeSlot.startTime)
    assertThat(sessionSchedule.sessionTimeSlot.endTime).isEqualTo(expectedSessionScheduleDto.sessionTimeSlot.endTime)
    assertThat(sessionSchedule.sessionDateRange.validFromDate).isEqualTo(expectedSessionScheduleDto.sessionDateRange.validFromDate)
    assertThat(sessionSchedule.sessionDateRange.validToDate).isEqualTo(expectedSessionScheduleDto.sessionDateRange.validToDate)
    assertThat(sessionSchedule.prisonerLocationGroupNames).isEqualTo(expectedSessionScheduleDto.prisonerLocationGroupNames)
    assertThat(sessionSchedule.prisonerCategoryGroupNames).isEqualTo(expectedSessionScheduleDto.prisonerCategoryGroupNames)
    assertThat(sessionSchedule.prisonerIncentiveLevelGroupNames).isEqualTo(expectedSessionScheduleDto.prisonerIncentiveLevelGroupNames)
    assertThat(sessionSchedule.areLocationGroupsInclusive).isEqualTo(expectedSessionScheduleDto.areLocationGroupsInclusive)
    assertThat(sessionSchedule.areCategoryGroupsInclusive).isEqualTo(expectedSessionScheduleDto.areCategoryGroupsInclusive)
    assertThat(sessionSchedule.areIncentiveGroupsInclusive).isEqualTo(expectedSessionScheduleDto.areIncentiveGroupsInclusive)
    assertThat(sessionSchedule.visitRoom).isEqualTo(expectedSessionScheduleDto.visitRoom)
    assertThat(sessionSchedule.isSessionExcluded).isEqualTo(expectedSessionScheduleDto.isSessionExcluded)
  }

  private fun createSessionScheduleDto(
    reference: String,
    startTime: LocalTime,
    endTime: LocalTime,
    sessionCapacityDto: SessionCapacityDto = SessionCapacityDto(2, 30),
    visitType: VisitType = VisitType.SOCIAL,
    areLocationGroupsInclusive: Boolean,
    areCategoryGroupsInclusive: Boolean,
    areIncentiveGroupsInclusive: Boolean,
    weeklyFrequency: Int = 1,
    validFromDate: LocalDate,
    validToDate: LocalDate? = null,
    visitRoom: String,
    prisonerLocationGroupNames: List<String> = mutableListOf(),
    prisonerCategoryGroupNames: List<String> = mutableListOf(),
    prisonerIncentiveLevelGroupNames: List<String> = mutableListOf(),
    isSessionExcluded: Boolean = false,
  ): SessionScheduleDto = SessionScheduleDto(
    sessionTemplateReference = reference,
    sessionDateRange = SessionDateRangeDto(validFromDate, validToDate),
    sessionTimeSlot = SessionTimeSlotDto(startTime = startTime, endTime = endTime),
    capacity = sessionCapacityDto,
    visitType = visitType,
    areLocationGroupsInclusive = areLocationGroupsInclusive,
    weeklyFrequency = weeklyFrequency,
    prisonerLocationGroupNames = prisonerLocationGroupNames,
    areCategoryGroupsInclusive = areCategoryGroupsInclusive,
    prisonerCategoryGroupNames = prisonerCategoryGroupNames,
    areIncentiveGroupsInclusive = areIncentiveGroupsInclusive,
    prisonerIncentiveLevelGroupNames = prisonerIncentiveLevelGroupNames,
    visitRoom = visitRoom,
    isSessionExcluded = isSessionExcluded,
  )

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): Array<SessionScheduleDto> = TestObjectMapper.mapper.readValue(returnResult.returnResult().responseBody, Array<SessionScheduleDto>::class.java)
}
