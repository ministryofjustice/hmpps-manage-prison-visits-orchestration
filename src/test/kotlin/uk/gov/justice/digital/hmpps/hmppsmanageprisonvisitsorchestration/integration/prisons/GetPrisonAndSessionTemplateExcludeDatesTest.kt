package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.prisons

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_CLASS
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller.ORCHESTRATION_PRISONS_EXCLUDE_DATE_GET_FUTURE_CONTROLLER_V2_PATH
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.manage.users.UserExtendedDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionScheduleDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionScheduleWithDateExclusionsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionTemplateVisitOrderRestrictionType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.prisons.ExcludeDateDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.prisons.PrisonAndSessionsExcludeDatesDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.TestObjectMapper
import java.time.LocalDate
import java.time.LocalTime

@DisplayName("GET $ORCHESTRATION_PRISONS_EXCLUDE_DATE_GET_FUTURE_CONTROLLER_V2_PATH with includeSessions as true tests")
@DirtiesContext(classMode = BEFORE_CLASS)
class GetPrisonAndSessionTemplateExcludeDatesTest : IntegrationTestBase() {
  final val prisonCode = "HEI"
  final val userIds = listOf("user-13", "user-14", "user-15", "user-16")
  final val userNamesMap = mapOf(
    "user-13" to UserExtendedDetailsDto("user-13", "User", "Thirteen"),
    "user-14" to UserExtendedDetailsDto("user-14", "User", "Fourteen"),
    "user-16" to UserExtendedDetailsDto("user-16", "User", "Sixteen"),
  )

  private val today = LocalDate.now()

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
      validFromDate = today.minusWeeks(1),
      validToDate = today.plusWeeks(2),
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
      validFromDate = today.minusWeeks(2),
      areLocationGroupsInclusive = false,
      areCategoryGroupsInclusive = false,
      areIncentiveGroupsInclusive = false,
      visitRoom = "Visit Room 2",
      visitOrderRestriction = SessionTemplateVisitOrderRestrictionType.NONE,
      isSessionExcluded = true,
    )
  }

  fun callGetPrisonAndSessionTemplateFutureExcludeDates(
    webTestClient: WebTestClient,
    prisonCode: String,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec = webTestClient.get().uri(
    buildString {
      append(ORCHESTRATION_PRISONS_EXCLUDE_DATE_GET_FUTURE_CONTROLLER_V2_PATH.replace("{prisonCode}", prisonCode))
      append("?includeSessions=true")
    },
  )
    .headers(authHttpHeaders)
    .exchange()

  @Test
  fun `when a prison and its sessions have exclude dates then all prison and session exclude dates are returned`() {
    // Given
    val today = LocalDate.now()
    // 3 past exclude dates, 1 current and 3 future exclude dates
    val prisonExcludeDatePast1 = ExcludeDateDto(today.minusDays(1), "user-14")
    val prisonExcludeDatePast2 = ExcludeDateDto(today.minusDays(2), "user-15")
    val prisonExcludeDatePast3 = ExcludeDateDto(today.minusDays(3), "user-13")

    val prisonExcludeDateCurrent = ExcludeDateDto(today, "user-14")
    val prisonExcludeDateFuture1 = ExcludeDateDto(today.plusDays(1), "user-13")
    val prisonExcludeDateFuture2 = ExcludeDateDto(today.plusDays(2), "user-16")
    val prisonExcludeDateFuture3 = ExcludeDateDto(today.plusDays(3), "user-15")

    val excludeDates = listOf(prisonExcludeDatePast1, prisonExcludeDatePast2, prisonExcludeDatePast3, prisonExcludeDateCurrent, prisonExcludeDateFuture1, prisonExcludeDateFuture2, prisonExcludeDateFuture3)
    visitSchedulerMockServer.stubGetExcludeDates(prisonCode, excludeDates.sortedByDescending { it.excludeDate })

    val sessionTemplate1ExcludeDateCurrent = ExcludeDateDto(today, "user-14")
    val sessionTemplate1ExcludeDateFuture1 = ExcludeDateDto(today.plusDays(23), "user-13")
    val sessionTemplate1ExcludeDateFuture2 = ExcludeDateDto(today.plusDays(21), "user-16")

    val sessionTemplate2ExcludeDateCurrent = ExcludeDateDto(today, "user-15")
    val sessionTemplate2ExcludeDateFuture1 = ExcludeDateDto(today.plusDays(3), "user-13")
    val sessionTemplate2ExcludeDateFuture2 = ExcludeDateDto(today.plusDays(2), "user-16")

    val sessionScheduleWithDateExclusions1 = SessionScheduleWithDateExclusionsDto(sessionScheduleDto1, listOf(sessionTemplate1ExcludeDateCurrent, sessionTemplate1ExcludeDateFuture1, sessionTemplate1ExcludeDateFuture2))
    val sessionScheduleWithDateExclusions2 = SessionScheduleWithDateExclusionsDto(sessionScheduleDto2, listOf(sessionTemplate2ExcludeDateCurrent, sessionTemplate2ExcludeDateFuture1, sessionTemplate2ExcludeDateFuture2))

    visitSchedulerMockServer.stubGetSessionSchedulesWithDateExclusions(prisonCode, listOf(sessionScheduleWithDateExclusions1, sessionScheduleWithDateExclusions2))

    // user-14 and user-16 exist on hmpps-auth but not user-15
    manageUsersApiMockServer.stubGetMultipleUserDetails(userIds, userDetails = userNamesMap)

    // When
    val responseSpec = callGetPrisonAndSessionTemplateFutureExcludeDates(webTestClient, prisonCode, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonAndSessionExcludeDate = getResults(returnResult)
    Assertions.assertThat(prisonAndSessionExcludeDate.fullDateExclusions).hasSize(4)
    Assertions.assertThat(prisonAndSessionExcludeDate.fullDateExclusions[0]).isEqualTo(ExcludeDateDto(prisonExcludeDateCurrent.excludeDate, "User Fourteen"))
    Assertions.assertThat(prisonAndSessionExcludeDate.fullDateExclusions[1]).isEqualTo(ExcludeDateDto(prisonExcludeDateFuture1.excludeDate, "User Thirteen"))
    Assertions.assertThat(prisonAndSessionExcludeDate.fullDateExclusions[2]).isEqualTo(ExcludeDateDto(prisonExcludeDateFuture2.excludeDate, "User Sixteen"))
    Assertions.assertThat(prisonAndSessionExcludeDate.fullDateExclusions[3]).isEqualTo(ExcludeDateDto(prisonExcludeDateFuture3.excludeDate, "user-15"))
    Assertions.assertThat(prisonAndSessionExcludeDate.sessionExclusions).hasSize(6)

    val sessionTemplateExclusions = prisonAndSessionExcludeDate.sessionExclusions
    Assertions.assertThat(sessionTemplateExclusions[0].sessionSchedule.sessionTemplateReference).isEqualTo(sessionScheduleDto1.sessionTemplateReference)
    Assertions.assertThat(sessionTemplateExclusions[0].excludeDate).isEqualTo(ExcludeDateDto(sessionTemplate1ExcludeDateCurrent.excludeDate, "User Fourteen"))

    Assertions.assertThat(sessionTemplateExclusions[1].sessionSchedule.sessionTemplateReference).isEqualTo(sessionScheduleDto2.sessionTemplateReference)
    Assertions.assertThat(sessionTemplateExclusions[1].excludeDate).isEqualTo(ExcludeDateDto(sessionTemplate2ExcludeDateCurrent.excludeDate, "user-15"))

    Assertions.assertThat(sessionTemplateExclusions[2].sessionSchedule.sessionTemplateReference).isEqualTo(sessionScheduleDto2.sessionTemplateReference)
    Assertions.assertThat(sessionTemplateExclusions[2].excludeDate).isEqualTo(ExcludeDateDto(sessionTemplate2ExcludeDateFuture2.excludeDate, "User Sixteen"))

    Assertions.assertThat(sessionTemplateExclusions[3].sessionSchedule.sessionTemplateReference).isEqualTo(sessionScheduleDto2.sessionTemplateReference)
    Assertions.assertThat(sessionTemplateExclusions[3].excludeDate).isEqualTo(ExcludeDateDto(sessionTemplate2ExcludeDateFuture1.excludeDate, "User Thirteen"))

    Assertions.assertThat(sessionTemplateExclusions[4].sessionSchedule.sessionTemplateReference).isEqualTo(sessionScheduleDto1.sessionTemplateReference)
    Assertions.assertThat(sessionTemplateExclusions[4].excludeDate).isEqualTo(ExcludeDateDto(sessionTemplate1ExcludeDateFuture2.excludeDate, "User Sixteen"))

    Assertions.assertThat(sessionTemplateExclusions[5].sessionSchedule.sessionTemplateReference).isEqualTo(sessionScheduleDto1.sessionTemplateReference)
    Assertions.assertThat(sessionTemplateExclusions[5].excludeDate).isEqualTo(ExcludeDateDto(sessionTemplate1ExcludeDateFuture1.excludeDate, "User Thirteen"))

    verify(manageUsersApiClientSpy, times(2)).getUsersByUsernames(any())
    verify(visitSchedulerClientSpy, times(1)).getPrisonExcludeDates(any())
    verify(visitSchedulerClientSpy, times(1)).getFutureSessionTemplateExclusions(any())
  }

  @Test
  fun `when a prison does not have exclude dates and its sessions have exclude dates then prison exclude dates are empty and session exclude dates are returned`() {
    // Given
    val today = LocalDate.now()
    visitSchedulerMockServer.stubGetExcludeDates(prisonCode, emptyList())

    val sessionTemplate1ExcludeDateCurrent = ExcludeDateDto(today, "user-14")
    val sessionTemplate2ExcludeDateCurrent = ExcludeDateDto(today, "user-15")
    val sessionScheduleWithDateExclusions1 = SessionScheduleWithDateExclusionsDto(sessionScheduleDto1, listOf(sessionTemplate1ExcludeDateCurrent))
    val sessionScheduleWithDateExclusions2 = SessionScheduleWithDateExclusionsDto(sessionScheduleDto2, listOf(sessionTemplate2ExcludeDateCurrent))

    visitSchedulerMockServer.stubGetSessionSchedulesWithDateExclusions(prisonCode, listOf(sessionScheduleWithDateExclusions2, sessionScheduleWithDateExclusions1))

    // user-14 and user-16 exist on hmpps-auth but not user-15
    manageUsersApiMockServer.stubGetMultipleUserDetails(userIds, userDetails = userNamesMap)

    // When
    val responseSpec = callGetPrisonAndSessionTemplateFutureExcludeDates(webTestClient, prisonCode, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonAndSessionExcludeDate = getResults(returnResult)
    Assertions.assertThat(prisonAndSessionExcludeDate.fullDateExclusions).hasSize(0)

    Assertions.assertThat(prisonAndSessionExcludeDate.sessionExclusions).hasSize(2)
    val sessionTemplateExclusions = prisonAndSessionExcludeDate.sessionExclusions
    Assertions.assertThat(sessionTemplateExclusions[0].excludeDate).isEqualTo(ExcludeDateDto(sessionTemplate1ExcludeDateCurrent.excludeDate, "User Fourteen"))
    Assertions.assertThat(sessionTemplateExclusions[1].excludeDate).isEqualTo(ExcludeDateDto(sessionTemplate2ExcludeDateCurrent.excludeDate, "user-15"))

    verify(manageUsersApiClientSpy, times(1)).getUsersByUsernames(any())
    verify(visitSchedulerClientSpy, times(1)).getPrisonExcludeDates(any())
    verify(visitSchedulerClientSpy, times(1)).getFutureSessionTemplateExclusions(any())
  }

  @Test
  fun `when a prison does not have exclude dates and its sessions do not have exclude dates then prison exclude dates are returned and session exclude dates are empty`() {
    // Given
    val today = LocalDate.now()
    // 3 past exclude dates, 1 current and 1 future exclude dates
    val prisonExcludeDatePast1 = ExcludeDateDto(today.minusDays(1), "user-14")
    val prisonExcludeDatePast2 = ExcludeDateDto(today.minusDays(2), "user-15")
    val prisonExcludeDatePast3 = ExcludeDateDto(today.minusDays(3), "user-13")

    val prisonExcludeDateCurrent = ExcludeDateDto(today, "user-14")
    val prisonExcludeDateFuture1 = ExcludeDateDto(today.plusDays(1), "user-13")

    val excludeDates = listOf(prisonExcludeDatePast1, prisonExcludeDatePast2, prisonExcludeDatePast3, prisonExcludeDateCurrent, prisonExcludeDateFuture1)
    visitSchedulerMockServer.stubGetExcludeDates(prisonCode, excludeDates.sortedByDescending { it.excludeDate })
    visitSchedulerMockServer.stubGetSessionSchedulesWithDateExclusions(prisonCode, emptyList())

    // user-14 and user-16 exist on hmpps-auth but not user-15
    manageUsersApiMockServer.stubGetMultipleUserDetails(userIds, userDetails = userNamesMap)

    // When
    val responseSpec = callGetPrisonAndSessionTemplateFutureExcludeDates(webTestClient, prisonCode, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonAndSessionExcludeDate = getResults(returnResult)
    Assertions.assertThat(prisonAndSessionExcludeDate.fullDateExclusions).hasSize(2)
    Assertions.assertThat(prisonAndSessionExcludeDate.fullDateExclusions[0]).isEqualTo(ExcludeDateDto(prisonExcludeDateCurrent.excludeDate, "User Fourteen"))
    Assertions.assertThat(prisonAndSessionExcludeDate.fullDateExclusions[1]).isEqualTo(ExcludeDateDto(prisonExcludeDateFuture1.excludeDate, "User Thirteen"))

    Assertions.assertThat(prisonAndSessionExcludeDate.sessionExclusions).isEmpty()

    verify(manageUsersApiClientSpy, times(1)).getUsersByUsernames(any())
    verify(visitSchedulerClientSpy, times(1)).getPrisonExcludeDates(any())
    verify(visitSchedulerClientSpy, times(1)).getFutureSessionTemplateExclusions(any())
  }

  @Test
  fun `when NOT_FOUND is returned from visit scheduler get prison exclude dates then NOT_FOUND status is sent back`() {
    // Given
    visitSchedulerMockServer.stubGetExcludeDates(prisonCode, null, HttpStatus.NOT_FOUND)
    visitSchedulerMockServer.stubGetSessionSchedulesWithDateExclusions(prisonCode, emptyList())

    // When
    val responseSpec = callGetPrisonAndSessionTemplateFutureExcludeDates(webTestClient, prisonCode, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
    verify(visitSchedulerClientSpy, times(1)).getPrisonExcludeDates(any())
    verify(visitSchedulerClientSpy, times(0)).getFutureSessionTemplateExclusions(any())
    verify(manageUsersApiClientSpy, times(0)).getUserDetails(any())
  }

  @Test
  fun `when BAD_REQUEST is returned from visit scheduler get prison exclude dates then BAD_REQUEST status is sent back`() {
    // Given
    visitSchedulerMockServer.stubGetExcludeDates(prisonCode, null, HttpStatus.BAD_REQUEST)
    visitSchedulerMockServer.stubGetSessionSchedulesWithDateExclusions(prisonCode, emptyList())

    // When
    val responseSpec = callGetPrisonAndSessionTemplateFutureExcludeDates(webTestClient, prisonCode, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isBadRequest
    verify(visitSchedulerClientSpy, times(1)).getPrisonExcludeDates(any())
    verify(visitSchedulerClientSpy, times(0)).getFutureSessionTemplateExclusions(any())
    verify(manageUsersApiClientSpy, times(0)).getUserDetails(any())
  }

  @Test
  fun `when NOT_FOUND is returned from visit scheduler get session date exclusions then NOT_FOUND status is sent back`() {
    // Given
    visitSchedulerMockServer.stubGetExcludeDates(prisonCode, emptyList())
    visitSchedulerMockServer.stubGetSessionSchedulesWithDateExclusions(prisonCode, null, HttpStatus.NOT_FOUND)

    // When
    val responseSpec = callGetPrisonAndSessionTemplateFutureExcludeDates(webTestClient, prisonCode, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
    verify(visitSchedulerClientSpy, times(1)).getPrisonExcludeDates(any())
    verify(visitSchedulerClientSpy, times(1)).getFutureSessionTemplateExclusions(any())
    verify(manageUsersApiClientSpy, times(0)).getUserDetails(any())
  }

  @Test
  fun `when BAD_REQUEST is returned from visit scheduler get session date exclusions then BAD_REQUEST status is sent back`() {
    // Given
    visitSchedulerMockServer.stubGetExcludeDates(prisonCode, emptyList())
    visitSchedulerMockServer.stubGetSessionSchedulesWithDateExclusions(prisonCode, null, HttpStatus.BAD_REQUEST)

    // When
    val responseSpec = callGetPrisonAndSessionTemplateFutureExcludeDates(webTestClient, prisonCode, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isBadRequest
    verify(visitSchedulerClientSpy, times(1)).getPrisonExcludeDates(any())
    verify(visitSchedulerClientSpy, times(1)).getFutureSessionTemplateExclusions(any())
    verify(manageUsersApiClientSpy, times(0)).getUserDetails(any())
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): PrisonAndSessionsExcludeDatesDto = TestObjectMapper.mapper.readValue(returnResult.returnResult().responseBody, PrisonAndSessionsExcludeDatesDto::class.java)
}
