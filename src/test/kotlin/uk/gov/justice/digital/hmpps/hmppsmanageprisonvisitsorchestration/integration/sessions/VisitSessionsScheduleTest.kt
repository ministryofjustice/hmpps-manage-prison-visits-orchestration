package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.sessions

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionCapacityDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionScheduleDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import java.time.LocalDate
import java.time.LocalTime

@DisplayName("Get visits by reference")
class VisitSessionsScheduleTest : IntegrationTestBase() {
  fun callVisitsSessionsSchedule(
    webTestClient: WebTestClient,
    prisonCode: String,
    sessionDate: LocalDate,
    authHttpHeaders: (HttpHeaders) -> Unit
  ): WebTestClient.ResponseSpec {
    return webTestClient.get().uri("/visit-sessions/schedule?prisonId=$prisonCode&sessionDate=$sessionDate")
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
      startTime = LocalTime.of(9, 0), endTime = LocalTime.of(10, 0)
    )
    val sessionScheduleDto2 = createSessionScheduleDto(
      reference = "reference-2",
      startTime = LocalTime.of(10, 0), endTime = LocalTime.of(11, 0)
    )
    visitSchedulerMockServer.stubGetSessionSchedule(
      prisonCode,
      sessionDate,
      mutableListOf(sessionScheduleDto1, sessionScheduleDto2)
    )

    // When
    val responseSpec = callVisitsSessionsSchedule(webTestClient, prisonCode, sessionDate, roleVisitSchedulerHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(2)
      .jsonPath("$[0].sessionTemplateReference").isEqualTo(sessionScheduleDto1.sessionTemplateReference)
      .jsonPath("$[0].startTime").isEqualTo("09:00:00")
      .jsonPath("$[0].endTime").isEqualTo("10:00:00")
      .jsonPath("$[1].sessionTemplateReference").isEqualTo(sessionScheduleDto2.sessionTemplateReference)
      .jsonPath("$[1].startTime").isEqualTo("10:00:00")
      .jsonPath("$[1].endTime").isEqualTo("11:00:00")
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
    sessionTemplateFrequency: String = "WEEKLY",
    sessionTemplateEndDate: LocalDate? = null
  ): SessionScheduleDto {
    return SessionScheduleDto(
      sessionTemplateReference = reference, startTime = startTime, endTime = endTime,
      capacity = sessionCapacityDto, sessionTemplateFrequency = sessionTemplateFrequency,
      prisonerLocationGroupNames = mutableListOf(), sessionTemplateEndDate = sessionTemplateEndDate
    )
  }
}
