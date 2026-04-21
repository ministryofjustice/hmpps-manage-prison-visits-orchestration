package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.sessions

import org.assertj.core.api.Assertions.assertThat
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
  fun callVisitsSessionsSchedule(
    webTestClient: WebTestClient,
    prisonCode: String,
    sessionDate: LocalDate,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec = webTestClient.get().uri("/visit-sessions/schedule?prisonId=$prisonCode&date=$sessionDate")
    .headers(authHttpHeaders)
    .exchange()

  @Test
  fun `when multiple session schedules exist for a prison all session schedules are returned`() {
    // Given
    val prisonCode = "MDI"
    val sessionDate = LocalDate.now().plusDays(1)
    val sessionScheduleDto1 = createSessionScheduleDto(
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
    )
    val sessionScheduleDto2 = createSessionScheduleDto(
      reference = "reference-2",
      startTime = LocalTime.of(10, 0),
      endTime = LocalTime.of(11, 0),
      validFromDate = sessionDate.minusWeeks(2),
      areLocationGroupsInclusive = false,
      areCategoryGroupsInclusive = false,
      areIncentiveGroupsInclusive = false,
      visitRoom = "Visit Room 2",
    )
    visitSchedulerMockServer.stubGetSessionSchedule(
      prisonCode,
      sessionDate,
      mutableListOf(sessionScheduleDto1, sessionScheduleDto2),
    )

    // When
    val responseSpec = callVisitsSessionsSchedule(webTestClient, prisonCode, sessionDate, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val sessionScheduleResults = getResults(returnResult)
    assertThat(sessionScheduleResults.size).isEqualTo(2)
    assertThat(sessionScheduleResults[0].sessionTemplateReference).isEqualTo(sessionScheduleDto1.sessionTemplateReference)
    assertThat(sessionScheduleResults[0].sessionTimeSlot.startTime).isEqualTo(LocalTime.parse("09:00:00"))
    assertThat(sessionScheduleResults[0].sessionTimeSlot.endTime).isEqualTo(LocalTime.parse("10:00:00"))
    assertThat(sessionScheduleResults[0].sessionDateRange.validFromDate).isEqualTo(sessionDate.minusWeeks(1))
    assertThat(sessionScheduleResults[0].sessionDateRange.validToDate).isEqualTo(sessionDate.plusWeeks(2))
    assertThat(sessionScheduleResults[0].prisonerLocationGroupNames.size).isEqualTo(2)
    assertThat(sessionScheduleResults[0].prisonerCategoryGroupNames.size).isEqualTo(3)
    assertThat(sessionScheduleResults[0].prisonerIncentiveLevelGroupNames.size).isEqualTo(4)
    assertThat(sessionScheduleResults[0].areLocationGroupsInclusive).isTrue()
    assertThat(sessionScheduleResults[0].areCategoryGroupsInclusive).isTrue()
    assertThat(sessionScheduleResults[0].areIncentiveGroupsInclusive).isTrue()
    assertThat(sessionScheduleResults[0].visitRoom).isEqualTo("Visit Room 1")

    assertThat(sessionScheduleResults[1].sessionTemplateReference).isEqualTo(sessionScheduleDto2.sessionTemplateReference)
    assertThat(sessionScheduleResults[1].sessionTimeSlot.startTime).isEqualTo(LocalTime.parse("10:00"))
    assertThat(sessionScheduleResults[1].sessionTimeSlot.endTime).isEqualTo(LocalTime.parse("11:00"))
    assertThat(sessionScheduleResults[1].sessionDateRange.validFromDate).isEqualTo(sessionDate.minusWeeks(2))
    assertThat(sessionScheduleResults[1].sessionDateRange.validToDate).isNull()
    assertThat(sessionScheduleResults[1].prisonerLocationGroupNames.size).isEqualTo(0)
    assertThat(sessionScheduleResults[1].prisonerCategoryGroupNames.size).isEqualTo(0)
    assertThat(sessionScheduleResults[1].prisonerIncentiveLevelGroupNames.size).isEqualTo(0)
    assertThat(sessionScheduleResults[1].areLocationGroupsInclusive).isFalse()
    assertThat(sessionScheduleResults[1].areCategoryGroupsInclusive).isFalse()
    assertThat(sessionScheduleResults[1].areIncentiveGroupsInclusive).isFalse()
    assertThat(sessionScheduleResults[1].visitRoom).isEqualTo("Visit Room 2")
  }

  @Test
  fun `when no session schedules exist for a prison no session schedules are returned`() {
    // Given
    val prisonCode = "MDI"
    val sessionDate = LocalDate.now()

    visitSchedulerMockServer.stubGetSessionSchedule(prisonCode, sessionDate, mutableListOf())

    // When
    val responseSpec = callVisitsSessionsSchedule(webTestClient, prisonCode, sessionDate, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(0)
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
  )

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): Array<SessionScheduleDto> = TestObjectMapper.mapper.readValue(returnResult.returnResult().responseBody, Array<SessionScheduleDto>::class.java)
}
