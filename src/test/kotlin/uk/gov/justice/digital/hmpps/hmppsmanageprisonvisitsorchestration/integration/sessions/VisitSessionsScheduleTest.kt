package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.sessions

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionCapacityDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionScheduleDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionTemplateFrequency
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import java.time.LocalDate
import java.time.LocalTime

@DisplayName("Get visits by reference")
class VisitSessionsScheduleTest : IntegrationTestBase() {
  fun callVisitsSessionsSchedule(
    webTestClient: WebTestClient,
    prisonCode: String,
    sessionDate: LocalDate,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    return webTestClient.get().uri("/visit-sessions/schedule?prisonId=$prisonCode&date=$sessionDate")
      .headers(authHttpHeaders)
      .exchange()
  }

  @Test
  fun `when multiple session schedules exist for a prison all session schedules are returned`() {
    // Given
    val prisonCode = "MDI"
    val sessionDate = LocalDate.now().plusDays(1)
    val sessionScheduleDto1 = createSessionScheduleDto(
      reference = "reference-1",
      startTime = LocalTime.of(9, 0),
      endTime = LocalTime.of(10, 0),
      enhanced = true,
      prisonerLocationGroupNames = listOf("Location Group 1", "Location Group 2"),
      prisonerCategoryGroupNames = listOf("Category Group 1", "Category Group 2", "Category Group 3"),
    )
    val sessionScheduleDto2 = createSessionScheduleDto(
      reference = "reference-2",
      startTime = LocalTime.of(10, 0),
      endTime = LocalTime.of(11, 0),
      enhanced = false,
    )
    visitSchedulerMockServer.stubGetSessionSchedule(
      prisonCode,
      sessionDate,
      mutableListOf(sessionScheduleDto1, sessionScheduleDto2),
    )

    // When
    val responseSpec = callVisitsSessionsSchedule(webTestClient, prisonCode, sessionDate, roleVisitSchedulerHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val sessionScheduleResults = getResults(returnResult)
    assertThat(sessionScheduleResults.size).isEqualTo(2)
    assertThat(sessionScheduleResults[0].sessionTemplateReference).isEqualTo(sessionScheduleDto1.sessionTemplateReference)
    assertThat(sessionScheduleResults[0].startTime).isEqualTo(LocalTime.parse("09:00:00"))
    assertThat(sessionScheduleResults[0].endTime).isEqualTo(LocalTime.parse("10:00:00"))
    assertThat(sessionScheduleResults[0].enhanced).isTrue
    assertThat(sessionScheduleResults[0].prisonerLocationGroupNames.size).isEqualTo(2)
    assertThat(sessionScheduleResults[0].prisonerCategoryGroupNames.size).isEqualTo(3)

    assertThat(sessionScheduleResults[1].sessionTemplateReference).isEqualTo(sessionScheduleDto2.sessionTemplateReference)
    assertThat(sessionScheduleResults[1].startTime).isEqualTo(LocalTime.parse("10:00"))
    assertThat(sessionScheduleResults[1].endTime).isEqualTo(LocalTime.parse("11:00"))
    assertThat(sessionScheduleResults[1].enhanced).isFalse
    assertThat(sessionScheduleResults[1].prisonerLocationGroupNames.size).isEqualTo(0)
    assertThat(sessionScheduleResults[1].prisonerCategoryGroupNames.size).isEqualTo(0)
  }

  @Test
  fun `when no session schedules exist for a prison no session schedules are returned`() {
    // Given
    val prisonCode = "MDI"
    val sessionDate = LocalDate.now()

    visitSchedulerMockServer.stubGetSessionSchedule(prisonCode, sessionDate, mutableListOf())

    // When
    val responseSpec = callVisitsSessionsSchedule(webTestClient, prisonCode, sessionDate, roleVisitSchedulerHttpHeaders)

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
    sessionTemplateFrequency: SessionTemplateFrequency = SessionTemplateFrequency.WEEKLY,
    sessionTemplateEndDate: LocalDate? = null,
    enhanced: Boolean,
    prisonerLocationGroupNames: List<String> = mutableListOf(),
    prisonerCategoryGroupNames: List<String> = mutableListOf(),
  ): SessionScheduleDto {
    return SessionScheduleDto(
      sessionTemplateReference = reference,
      startTime = startTime,
      endTime = endTime,
      capacity = sessionCapacityDto,
      sessionTemplateFrequency = sessionTemplateFrequency,
      prisonerLocationGroupNames = prisonerLocationGroupNames,
      prisonerCategoryGroupNames = prisonerCategoryGroupNames,
      sessionTemplateEndDate = sessionTemplateEndDate,
      enhanced = enhanced,
    )
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): Array<SessionScheduleDto> {
    return objectMapper.readValue(returnResult.returnResult().responseBody, Array<SessionScheduleDto>::class.java)
  }
}
