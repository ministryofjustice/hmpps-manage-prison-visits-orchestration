package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.sessions

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitSchedulerClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.WhereAboutsApiClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSchedulerPrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType.STAFF
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.sessions.VisitSessionsAndScheduleDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.TestObjectMapper
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@DisplayName("Get visit sessions")
class VisitSessionsAndScheduleTest : IntegrationTestBase() {
  private val prisonCode = "MDI"
  private val prisonerId = "ABC"
  private val sessionStartTime: LocalTime = LocalTime.of(10, 0)
  private val sessionEndTime: LocalTime = LocalTime.of(11, 0)
  private val minDays = 2
  private val maxDays = 12
  private val today = LocalDate.now()

  @MockitoSpyBean
  private lateinit var visitSchedulerClientSpy: VisitSchedulerClient

  @MockitoSpyBean
  private lateinit var whereAboutsApiClientSpy: WhereAboutsApiClient

  fun callGetVisitSessionsAndSchedule(
    webTestClient: WebTestClient,
    prisonCode: String,
    prisonerId: String,
    min: Int?,
    username: String?,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    val uri = "/visit-sessions-and-schedule"
    val uriQueryParams = mutableListOf("prisonId=$prisonCode", "prisonerId=$prisonerId").also { queryParams ->
      min?.let {
        queryParams.add("min=$min")
      }
      username?.let {
        queryParams.add("username=$username")
      }
    }.joinToString("&")

    return webTestClient.get().uri("$uri?$uriQueryParams")
      .headers(authHttpHeaders)
      .exchange()
  }

  @BeforeEach
  fun setup() {
    val visitSchedulerPrisonDto = VisitSchedulerPrisonDto(prisonCode, true, 2, 12, 6, 3, 3, 18)
    visitSchedulerMockServer.stubGetPrison(prisonCode, visitSchedulerPrisonDto)
  }

  @Test
  fun `when visit sessions and a schedule exists then allowed sessions are returned for all dates starting today`() {
    // Given
    val visitSessionDto1 = createVisitSessionDto(prisonCode, "1", startTimestamp = LocalDateTime.of(today.plusDays(3), sessionStartTime), endTimestamp = LocalDateTime.of(today.plusDays(3), sessionEndTime))
    val visitSessionDto2 = createVisitSessionDto(prisonCode, "2", startTimestamp = LocalDateTime.of(today.plusDays(4), sessionStartTime), endTimestamp = LocalDateTime.of(today.plusDays(4), sessionEndTime))
    val visitSessionDto3 = createVisitSessionDto(prisonCode, "3", startTimestamp = LocalDateTime.of(today.plusDays(5), sessionStartTime), endTimestamp = LocalDateTime.of(today.plusDays(5), sessionEndTime))
    val visitSessionDto4 = createVisitSessionDto(prisonCode, "4", startTimestamp = LocalDateTime.of(today.plusDays(6), sessionStartTime), endTimestamp = LocalDateTime.of(today.plusDays(6), sessionEndTime))
    val visitSessionDto5 = createVisitSessionDto(prisonCode, "5", startTimestamp = LocalDateTime.of(today.plusDays(7), sessionStartTime), endTimestamp = LocalDateTime.of(today.plusDays(7), sessionEndTime))

    visitSchedulerMockServer.stubGetVisitSessions(prisonCode, prisonerId, mutableListOf(visitSessionDto1, visitSessionDto2, visitSessionDto3, visitSessionDto4, visitSessionDto5), userType = STAFF)

    val appointment1 = createScheduledEvent(1L, today.plusDays(4), eventStartTime = LocalDateTime.of(today.plusDays(4), LocalTime.of(9, 0)), eventEndTime = LocalDateTime.of(today.plusDays(4), LocalTime.of(10, 0)))
    val appointment2 = createScheduledEvent(2L, today.plusDays(5), eventStartTime = LocalDateTime.of(today.plusDays(5), LocalTime.of(9, 0)), eventEndTime = LocalDateTime.of(today.plusDays(5), LocalTime.of(10, 0)))
    val appointment3 = createScheduledEvent(3L, today.plusDays(6), eventStartTime = LocalDateTime.of(today.plusDays(6), LocalTime.of(9, 0)), eventEndTime = LocalDateTime.of(today.plusDays(6), LocalTime.of(10, 0)))

    whereaboutsApiMockServer.stubGetEvents(prisonerId, fromDate = today.plusDays(minDays.toLong() + 1), toDate = today.plusDays(maxDays.toLong()), events = listOf(appointment1, appointment2, appointment3))

    // When
    val responseSpec = callGetVisitSessionsAndSchedule(webTestClient, prisonCode, prisonerId, min = null, username = null, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val sessionsAndScheduleDto = getResults(returnResult)
    assertThat(sessionsAndScheduleDto.scheduledEventsAvailable).isTrue
    assertThat(sessionsAndScheduleDto.sessionsAndSchedule.size).isEqualTo(13)
    for (i in 0..maxDays) {
      assertThat(sessionsAndScheduleDto.sessionsAndSchedule[i].date).isEqualTo(today.plusDays(i.toLong()))
    }

    val datesWithSessionsAndSchedule = listOf(today.plusDays(4), today.plusDays(5), today.plusDays(6))
    assertSessionsAndScheduleCount(sessionsAndScheduleDto, datesWithSessionsAndSchedule, 1, 1)

    val datesWithSessionsNoSchedule = listOf(today.plusDays(3), today.plusDays(7))
    assertSessionsAndScheduleCount(sessionsAndScheduleDto, datesWithSessionsNoSchedule, 1, 0)

    val datesWithNoSessionsOrSchedule = sessionsAndScheduleDto.sessionsAndSchedule.map { it.date }.filter { !datesWithSessionsAndSchedule.contains(it) && !datesWithSessionsNoSchedule.contains(it) }
    assertSessionsAndScheduleCount(sessionsAndScheduleDto, datesWithNoSessionsOrSchedule, 0, 0)
    verify(visitSchedulerClientSpy, times(1)).getVisitSessions(prisonCode, prisonerId, null, null, null, STAFF)
    verify(whereAboutsApiClientSpy, times(1)).getEvents(prisonerId, LocalDate.now().plusDays(minDays.toLong() + 1), LocalDate.now().plusDays(maxDays.toLong()))
  }

  @Test
  fun `when no visit sessions and no schedule exists then all dates are returned with empty sessions and schedules`() {
    // Given
    visitSchedulerMockServer.stubGetVisitSessions(prisonCode, prisonerId, emptyList(), userType = STAFF)
    whereaboutsApiMockServer.stubGetEvents(prisonerId, fromDate = today.plusDays(minDays.toLong() + 1), toDate = today.plusDays(maxDays.toLong()), events = emptyList())

    // When
    val responseSpec = callGetVisitSessionsAndSchedule(webTestClient, prisonCode, prisonerId, min = null, username = null, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val sessionsAndScheduleDto = getResults(returnResult)
    assertThat(sessionsAndScheduleDto.scheduledEventsAvailable).isTrue
    assertThat(sessionsAndScheduleDto.sessionsAndSchedule.size).isEqualTo(13)
    for (i in 0..maxDays) {
      assertThat(sessionsAndScheduleDto.sessionsAndSchedule[i].date).isEqualTo(today.plusDays(i.toLong()))
    }

    val datesWithNoSessionsOrSchedule = sessionsAndScheduleDto.sessionsAndSchedule.map { it.date }
    assertSessionsAndScheduleCount(sessionsAndScheduleDto, datesWithNoSessionsOrSchedule, 0, 0)
    verify(visitSchedulerClientSpy, times(1)).getVisitSessions(prisonCode, prisonerId, null, null, null, STAFF)
    verify(whereAboutsApiClientSpy, times(1)).getEvents(prisonerId, LocalDate.now().plusDays(minDays.toLong() + 1), LocalDate.now().plusDays(maxDays.toLong()))
  }

  @Test
  fun `when visit sessions and a schedule exists then schedules are only returned for dates with sessions`() {
    // Given
    val visitSessionDto1 = createVisitSessionDto(prisonCode, "1", startTimestamp = LocalDateTime.of(today.plusDays(3), sessionStartTime), endTimestamp = LocalDateTime.of(today.plusDays(3), sessionEndTime))
    val visitSessionDto2 = createVisitSessionDto(prisonCode, "2", startTimestamp = LocalDateTime.of(today.plusDays(4), sessionStartTime), endTimestamp = LocalDateTime.of(today.plusDays(4), sessionEndTime))
    val visitSessionDto3 = createVisitSessionDto(prisonCode, "3", startTimestamp = LocalDateTime.of(today.plusDays(5), sessionStartTime), endTimestamp = LocalDateTime.of(today.plusDays(5), sessionEndTime))
    val visitSessionDto4 = createVisitSessionDto(prisonCode, "4", startTimestamp = LocalDateTime.of(today.plusDays(6), sessionStartTime), endTimestamp = LocalDateTime.of(today.plusDays(6), sessionEndTime))
    val visitSessionDto5 = createVisitSessionDto(prisonCode, "5", startTimestamp = LocalDateTime.of(today.plusDays(7), sessionStartTime), endTimestamp = LocalDateTime.of(today.plusDays(7), sessionEndTime))

    visitSchedulerMockServer.stubGetVisitSessions(prisonCode, prisonerId, mutableListOf(visitSessionDto1, visitSessionDto2, visitSessionDto3, visitSessionDto4, visitSessionDto5), userType = STAFF)

    val appointment1 = createScheduledEvent(1L, today.plusDays(4), eventStartTime = LocalDateTime.of(today.plusDays(4), LocalTime.of(9, 0)), eventEndTime = LocalDateTime.of(today.plusDays(4), LocalTime.of(10, 0)))
    val appointment2 = createScheduledEvent(2L, today.plusDays(9), eventStartTime = LocalDateTime.of(today.plusDays(9), LocalTime.of(9, 0)), eventEndTime = LocalDateTime.of(today.plusDays(9), LocalTime.of(10, 0)))
    val appointment3 = createScheduledEvent(3L, today.plusDays(10), eventStartTime = LocalDateTime.of(today.plusDays(10), LocalTime.of(9, 0)), eventEndTime = LocalDateTime.of(today.plusDays(10), LocalTime.of(10, 0)))

    whereaboutsApiMockServer.stubGetEvents(prisonerId, fromDate = today.plusDays(minDays.toLong() + 1), toDate = today.plusDays(maxDays.toLong()), events = listOf(appointment1, appointment2, appointment3))

    // When
    val responseSpec = callGetVisitSessionsAndSchedule(webTestClient, prisonCode, prisonerId, min = null, username = null, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val sessionsAndScheduleDto = getResults(returnResult)
    assertThat(sessionsAndScheduleDto.scheduledEventsAvailable).isTrue
    assertThat(sessionsAndScheduleDto.sessionsAndSchedule.size).isEqualTo(13)
    for (i in 0..maxDays) {
      assertThat(sessionsAndScheduleDto.sessionsAndSchedule[i].date).isEqualTo(today.plusDays(i.toLong()))
    }

    // only 1 date will have sessions and schedule
    val datesWithSessionsAndSchedule = listOf(today.plusDays(4))
    assertSessionsAndScheduleCount(sessionsAndScheduleDto, datesWithSessionsAndSchedule, 1, 1)

    val datesWithSessionsNoSchedule = listOf(today.plusDays(3), today.plusDays(5), today.plusDays(6), today.plusDays(7))
    assertSessionsAndScheduleCount(sessionsAndScheduleDto, datesWithSessionsNoSchedule, 1, 0)

    val datesWithNoSessionsOrSchedule = sessionsAndScheduleDto.sessionsAndSchedule.map { it.date }.filter { !datesWithSessionsAndSchedule.contains(it) && !datesWithSessionsNoSchedule.contains(it) }
    assertSessionsAndScheduleCount(sessionsAndScheduleDto, datesWithNoSessionsOrSchedule, 0, 0)
    verify(visitSchedulerClientSpy, times(1)).getVisitSessions(prisonCode, prisonerId, null, null, null, STAFF)
    verify(whereAboutsApiClientSpy, times(1)).getEvents(prisonerId, LocalDate.now().plusDays(minDays.toLong() + 1), LocalDate.now().plusDays(maxDays.toLong()))
  }

  @Test
  fun `when visit sessions returns NOT_FOUND a NOT_FOUND error is returned`() {
    // Given
    visitSchedulerMockServer.stubGetVisitSessions(prisonCode, prisonerId, null, userType = STAFF, httpStatus = HttpStatus.NOT_FOUND)

    val appointment1 = createScheduledEvent(1L, today.plusDays(4), eventStartTime = LocalDateTime.of(today.plusDays(4), LocalTime.of(9, 0)), eventEndTime = LocalDateTime.of(today.plusDays(4), LocalTime.of(10, 0)))
    val appointment2 = createScheduledEvent(2L, today.plusDays(9), eventStartTime = LocalDateTime.of(today.plusDays(9), LocalTime.of(9, 0)), eventEndTime = LocalDateTime.of(today.plusDays(9), LocalTime.of(10, 0)))
    val appointment3 = createScheduledEvent(3L, today.plusDays(10), eventStartTime = LocalDateTime.of(today.plusDays(10), LocalTime.of(9, 0)), eventEndTime = LocalDateTime.of(today.plusDays(10), LocalTime.of(10, 0)))

    whereaboutsApiMockServer.stubGetEvents(prisonerId, fromDate = today.plusDays(minDays.toLong() + 1), toDate = today.plusDays(maxDays.toLong()), events = listOf(appointment1, appointment2, appointment3))

    // When
    val responseSpec = callGetVisitSessionsAndSchedule(webTestClient, prisonCode, prisonerId, min = null, username = null, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
    verify(visitSchedulerClientSpy, times(1)).getVisitSessions(prisonCode, prisonerId, null, null, null, STAFF)
    verify(whereAboutsApiClientSpy, times(0)).getEvents(prisonerId, LocalDate.now().plusDays(minDays.toLong() + 1), LocalDate.now().plusDays(maxDays.toLong()))
  }

  @Test
  fun `when visit sessions returns INTERNAL_SERVER_ERROR a INTERNAL_SERVER_ERROR error is returned`() {
    // Given
    visitSchedulerMockServer.stubGetVisitSessions(prisonCode, prisonerId, null, userType = STAFF, httpStatus = HttpStatus.INTERNAL_SERVER_ERROR)

    val appointment1 = createScheduledEvent(1L, today.plusDays(4), eventStartTime = LocalDateTime.of(today.plusDays(4), LocalTime.of(9, 0)), eventEndTime = LocalDateTime.of(today.plusDays(4), LocalTime.of(10, 0)))
    val appointment2 = createScheduledEvent(2L, today.plusDays(9), eventStartTime = LocalDateTime.of(today.plusDays(9), LocalTime.of(9, 0)), eventEndTime = LocalDateTime.of(today.plusDays(9), LocalTime.of(10, 0)))
    val appointment3 = createScheduledEvent(3L, today.plusDays(10), eventStartTime = LocalDateTime.of(today.plusDays(10), LocalTime.of(9, 0)), eventEndTime = LocalDateTime.of(today.plusDays(10), LocalTime.of(10, 0)))

    whereaboutsApiMockServer.stubGetEvents(prisonerId, fromDate = today.plusDays(minDays.toLong() + 1), toDate = today.plusDays(maxDays.toLong()), events = listOf(appointment1, appointment2, appointment3))

    // When
    val responseSpec = callGetVisitSessionsAndSchedule(webTestClient, prisonCode, prisonerId, min = null, username = null, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().is5xxServerError
    verify(visitSchedulerClientSpy, times(1)).getVisitSessions(prisonCode, prisonerId, null, null, null, STAFF)
    verify(whereAboutsApiClientSpy, times(0)).getEvents(prisonerId, LocalDate.now().plusDays(minDays.toLong() + 1), LocalDate.now().plusDays(maxDays.toLong()))
  }

  @Test
  fun `when visit whereabouts api returns NOT_FOUND sessions are returned but schedules are empty`() {
    // Given
    val visitSessionDto1 = createVisitSessionDto(prisonCode, "1", startTimestamp = LocalDateTime.of(today.plusDays(3), sessionStartTime), endTimestamp = LocalDateTime.of(today.plusDays(3), sessionEndTime))
    val visitSessionDto2 = createVisitSessionDto(prisonCode, "2", startTimestamp = LocalDateTime.of(today.plusDays(4), sessionStartTime), endTimestamp = LocalDateTime.of(today.plusDays(4), sessionEndTime))
    val visitSessionDto3 = createVisitSessionDto(prisonCode, "3", startTimestamp = LocalDateTime.of(today.plusDays(5), sessionStartTime), endTimestamp = LocalDateTime.of(today.plusDays(5), sessionEndTime))
    val visitSessionDto4 = createVisitSessionDto(prisonCode, "4", startTimestamp = LocalDateTime.of(today.plusDays(6), sessionStartTime), endTimestamp = LocalDateTime.of(today.plusDays(6), sessionEndTime))
    val visitSessionDto5 = createVisitSessionDto(prisonCode, "5", startTimestamp = LocalDateTime.of(today.plusDays(7), sessionStartTime), endTimestamp = LocalDateTime.of(today.plusDays(7), sessionEndTime))

    visitSchedulerMockServer.stubGetVisitSessions(prisonCode, prisonerId, mutableListOf(visitSessionDto1, visitSessionDto2, visitSessionDto3, visitSessionDto4, visitSessionDto5), userType = STAFF)

    // whereabouts returns a 404
    whereaboutsApiMockServer.stubGetEvents(prisonerId, fromDate = today.plusDays(minDays.toLong() + 1), toDate = today.plusDays(maxDays.toLong()), events = null, httpStatus = HttpStatus.NOT_FOUND)

    // When
    val responseSpec = callGetVisitSessionsAndSchedule(webTestClient, prisonCode, prisonerId, min = null, username = null, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val sessionsAndScheduleDto = getResults(returnResult)
    assertThat(sessionsAndScheduleDto.scheduledEventsAvailable).isTrue

    // no scheduled events are returned for any dates
    assertThat(sessionsAndScheduleDto.sessionsAndSchedule.map { it.scheduledEvents }.sumOf { it.count() }).isEqualTo(0)
    verify(visitSchedulerClientSpy, times(1)).getVisitSessions(prisonCode, prisonerId, null, null, null, STAFF)
    verify(whereAboutsApiClientSpy, times(1)).getEvents(prisonerId, LocalDate.now().plusDays(minDays.toLong() + 1), LocalDate.now().plusDays(maxDays.toLong()))
  }

  @Test
  fun `when visit whereabouts api returns INTERNAL_SERVER_ERROR sessions are returned but schedules are empty and scheduledEventsAvailable flag is false`() {
    // Given
    val visitSessionDto1 = createVisitSessionDto(prisonCode, "1", startTimestamp = LocalDateTime.of(today.plusDays(3), sessionStartTime), endTimestamp = LocalDateTime.of(today.plusDays(3), sessionEndTime))
    val visitSessionDto2 = createVisitSessionDto(prisonCode, "2", startTimestamp = LocalDateTime.of(today.plusDays(4), sessionStartTime), endTimestamp = LocalDateTime.of(today.plusDays(4), sessionEndTime))
    val visitSessionDto3 = createVisitSessionDto(prisonCode, "3", startTimestamp = LocalDateTime.of(today.plusDays(5), sessionStartTime), endTimestamp = LocalDateTime.of(today.plusDays(5), sessionEndTime))
    val visitSessionDto4 = createVisitSessionDto(prisonCode, "4", startTimestamp = LocalDateTime.of(today.plusDays(6), sessionStartTime), endTimestamp = LocalDateTime.of(today.plusDays(6), sessionEndTime))
    val visitSessionDto5 = createVisitSessionDto(prisonCode, "5", startTimestamp = LocalDateTime.of(today.plusDays(7), sessionStartTime), endTimestamp = LocalDateTime.of(today.plusDays(7), sessionEndTime))

    visitSchedulerMockServer.stubGetVisitSessions(prisonCode, prisonerId, mutableListOf(visitSessionDto1, visitSessionDto2, visitSessionDto3, visitSessionDto4, visitSessionDto5), userType = STAFF)

    // whereabouts returns a 404
    whereaboutsApiMockServer.stubGetEvents(prisonerId, fromDate = today.plusDays(minDays.toLong() + 1), toDate = today.plusDays(maxDays.toLong()), events = null, httpStatus = HttpStatus.INTERNAL_SERVER_ERROR)

    // When
    val responseSpec = callGetVisitSessionsAndSchedule(webTestClient, prisonCode, prisonerId, min = null, username = null, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val sessionsAndScheduleDto = getResults(returnResult)

    // scheduledEventsAvailable is set to false if whereabouts throws INTERNAL_SERVER_ERROR
    assertThat(sessionsAndScheduleDto.scheduledEventsAvailable).isFalse

    // no scheduled events are returned for any dates
    assertThat(sessionsAndScheduleDto.sessionsAndSchedule.map { it.scheduledEvents }.sumOf { it.count() }).isEqualTo(0)
    verify(visitSchedulerClientSpy, times(1)).getVisitSessions(prisonCode, prisonerId, null, null, null, STAFF)
    verify(whereAboutsApiClientSpy, times(1)).getEvents(prisonerId, LocalDate.now().plusDays(minDays.toLong() + 1), LocalDate.now().plusDays(maxDays.toLong()))
  }

  private fun assertSessionsAndScheduleCount(sessionsAndScheduleDto: VisitSessionsAndScheduleDto, dates: List<LocalDate>, sessions: Int, schedules: Int) {
    for (date in dates) {
      assertThat(sessionsAndScheduleDto.sessionsAndSchedule.first { it.date == date }.visitSessions.size).isEqualTo(sessions)
      assertThat(sessionsAndScheduleDto.sessionsAndSchedule.first { it.date == date }.scheduledEvents.size).isEqualTo(schedules)
    }
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): VisitSessionsAndScheduleDto = TestObjectMapper.mapper.readValue(returnResult.returnResult().responseBody, VisitSessionsAndScheduleDto::class.java)
}
