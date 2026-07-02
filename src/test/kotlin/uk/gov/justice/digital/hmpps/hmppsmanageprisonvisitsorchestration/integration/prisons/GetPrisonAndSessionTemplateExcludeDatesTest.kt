package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.prisons

import org.assertj.core.api.Assertions
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
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionTemplateDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.prisons.ExcludeDateDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.prisons.PrisonAndSessionsExcludeDatesDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.TestObjectMapper
import java.time.LocalDate

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

    // 3 future session templates
    val sessionTemplate1 = SessionTemplateDto("ref-1")
    val sessionTemplate2 = SessionTemplateDto("ref-2")
    val sessionTemplate3 = SessionTemplateDto("ref-3")
    visitSchedulerMockServer.stubGetCurrentOrFutureSessionTemplates(prisonCode, listOf(sessionTemplate1, sessionTemplate2, sessionTemplate3))

    val sessionTemplate1ExcludeDatePast1 = ExcludeDateDto(today.minusDays(1), "user-15")
    val sessionTemplate1ExcludeDatePast2 = ExcludeDateDto(today.minusDays(1), "user-15")
    val sessionTemplate1ExcludeDateCurrent = ExcludeDateDto(today, "user-14")
    val sessionTemplate1ExcludeDateFuture = ExcludeDateDto(today.plusDays(3), "user-13")
    val sessionTemplate3ExcludeDateFuture = ExcludeDateDto(today.plusDays(21), "user-16")
    visitSchedulerMockServer.stubGetSessionTemplateExcludeDates(sessionTemplate1.reference, listOf(sessionTemplate1ExcludeDatePast1, sessionTemplate1ExcludeDatePast2, sessionTemplate1ExcludeDateCurrent, sessionTemplate1ExcludeDateFuture))
    visitSchedulerMockServer.stubGetSessionTemplateExcludeDates(sessionTemplate2.reference, emptyList())
    visitSchedulerMockServer.stubGetSessionTemplateExcludeDates(sessionTemplate3.reference, listOf(sessionTemplate3ExcludeDateFuture))

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
    Assertions.assertThat(prisonAndSessionExcludeDate.sessionExclusions).hasSize(3)

    val sessionTemplate1Exclusions = prisonAndSessionExcludeDate.sessionExclusions[sessionTemplate1.reference]!!
    Assertions.assertThat(sessionTemplate1Exclusions[0]).isEqualTo(ExcludeDateDto(sessionTemplate1ExcludeDateCurrent.excludeDate, "User Fourteen"))
    Assertions.assertThat(sessionTemplate1Exclusions[1]).isEqualTo(ExcludeDateDto(sessionTemplate1ExcludeDateFuture.excludeDate, "User Thirteen"))

    val sessionTemplate2Exclusions = prisonAndSessionExcludeDate.sessionExclusions[sessionTemplate2.reference]!!
    Assertions.assertThat(sessionTemplate2Exclusions).isEmpty()

    val sessionTemplate3Exclusions = prisonAndSessionExcludeDate.sessionExclusions[sessionTemplate3.reference]!!
    Assertions.assertThat(sessionTemplate3Exclusions[0]).isEqualTo(ExcludeDateDto(sessionTemplate3ExcludeDateFuture.excludeDate, "User Sixteen"))

    verify(manageUsersApiClientSpy, times(2)).getUsersByUsernames(any())
  }

  @Test
  fun `when a prison does not have exclude dates and its sessions have exclude dates then prison exclude dates are empty and session exclude dates are returned`() {
    // Given
    val today = LocalDate.now()
    visitSchedulerMockServer.stubGetExcludeDates(prisonCode, emptyList())

    // 1 future session template
    val sessionTemplate1 = SessionTemplateDto("ref-1")
    visitSchedulerMockServer.stubGetCurrentOrFutureSessionTemplates(prisonCode, listOf(sessionTemplate1))

    val sessionTemplate1ExcludeDatePast1 = ExcludeDateDto(today.minusDays(1), "user-15")
    val sessionTemplate1ExcludeDatePast2 = ExcludeDateDto(today.minusDays(1), "user-15")
    val sessionTemplate1ExcludeDateCurrent = ExcludeDateDto(today, "user-14")
    val sessionTemplate1ExcludeDateFuture = ExcludeDateDto(today.plusDays(3), "user-13")
    visitSchedulerMockServer.stubGetSessionTemplateExcludeDates(sessionTemplate1.reference, listOf(sessionTemplate1ExcludeDatePast1, sessionTemplate1ExcludeDatePast2, sessionTemplate1ExcludeDateCurrent, sessionTemplate1ExcludeDateFuture))

    // user-14 and user-16 exist on hmpps-auth but not user-15
    manageUsersApiMockServer.stubGetMultipleUserDetails(userIds, userDetails = userNamesMap)

    // When
    val responseSpec = callGetPrisonAndSessionTemplateFutureExcludeDates(webTestClient, prisonCode, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonAndSessionExcludeDate = getResults(returnResult)
    Assertions.assertThat(prisonAndSessionExcludeDate.fullDateExclusions).hasSize(0)

    Assertions.assertThat(prisonAndSessionExcludeDate.sessionExclusions).hasSize(1)
    val sessionTemplate1Exclusions = prisonAndSessionExcludeDate.sessionExclusions[sessionTemplate1.reference]!!
    Assertions.assertThat(sessionTemplate1Exclusions[0]).isEqualTo(ExcludeDateDto(sessionTemplate1ExcludeDateCurrent.excludeDate, "User Fourteen"))
    Assertions.assertThat(sessionTemplate1Exclusions[1]).isEqualTo(ExcludeDateDto(sessionTemplate1ExcludeDateFuture.excludeDate, "User Thirteen"))

    verify(manageUsersApiClientSpy, times(1)).getUsersByUsernames(any())
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

    // 3 future session templates
    val sessionTemplate1 = SessionTemplateDto("ref-1")
    val sessionTemplate2 = SessionTemplateDto("ref-2")
    visitSchedulerMockServer.stubGetCurrentOrFutureSessionTemplates(prisonCode, listOf(sessionTemplate1, sessionTemplate2))

    visitSchedulerMockServer.stubGetSessionTemplateExcludeDates(sessionTemplate1.reference, emptyList())
    visitSchedulerMockServer.stubGetSessionTemplateExcludeDates(sessionTemplate2.reference, emptyList())

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

    Assertions.assertThat(prisonAndSessionExcludeDate.sessionExclusions).hasSize(2)
    val sessionTemplate1Exclusions = prisonAndSessionExcludeDate.sessionExclusions[sessionTemplate1.reference]!!
    Assertions.assertThat(sessionTemplate1Exclusions).isEmpty()
    val sessionTemplate2Exclusions = prisonAndSessionExcludeDate.sessionExclusions[sessionTemplate2.reference]!!
    Assertions.assertThat(sessionTemplate2Exclusions).isEmpty()

    verify(manageUsersApiClientSpy, times(1)).getUsersByUsernames(any())
  }

  @Test
  fun `when NOT_FOUND is returned from visit scheduler get prison exclude dates then NOT_FOUND status is sent back`() {
    // Given
    visitSchedulerMockServer.stubGetExcludeDates(prisonCode, null, HttpStatus.NOT_FOUND)

    // 1 future session template
    val sessionTemplate1 = SessionTemplateDto("ref-1")
    visitSchedulerMockServer.stubGetCurrentOrFutureSessionTemplates(prisonCode, listOf(sessionTemplate1))
    visitSchedulerMockServer.stubGetSessionTemplateExcludeDates(sessionTemplate1.reference, emptyList())

    // When
    val responseSpec = callGetPrisonAndSessionTemplateFutureExcludeDates(webTestClient, prisonCode, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
    verify(visitSchedulerClientSpy, times(1)).getPrisonExcludeDates(any())
    verify(visitSchedulerClientSpy, times(0)).getCurrentOrFutureSessionTemplates(any())
    verify(visitSchedulerClientSpy, times(0)).getSessionTemplateExcludeDates(any())
    verify(manageUsersApiClientSpy, times(0)).getUserDetails(any())
  }

  @Test
  fun `when BAD_REQUEST is returned from visit scheduler get prison exclude dates then BAD_REQUEST status is sent back`() {
    // Given
    visitSchedulerMockServer.stubGetExcludeDates(prisonCode, null, HttpStatus.BAD_REQUEST)

    // 1 future session template
    val sessionTemplate1 = SessionTemplateDto("ref-1")
    visitSchedulerMockServer.stubGetCurrentOrFutureSessionTemplates(prisonCode, listOf(sessionTemplate1))
    visitSchedulerMockServer.stubGetSessionTemplateExcludeDates(sessionTemplate1.reference, emptyList())

    // When
    val responseSpec = callGetPrisonAndSessionTemplateFutureExcludeDates(webTestClient, prisonCode, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isBadRequest
    verify(visitSchedulerClientSpy, times(1)).getPrisonExcludeDates(any())
    verify(visitSchedulerClientSpy, times(0)).getCurrentOrFutureSessionTemplates(any())
    verify(visitSchedulerClientSpy, times(0)).getSessionTemplateExcludeDates(any())
    verify(manageUsersApiClientSpy, times(0)).getUserDetails(any())
  }

  @Test
  fun `when NOT_FOUND is returned from visit scheduler get session templates then NOT_FOUND status is sent back`() {
    // Given
    visitSchedulerMockServer.stubGetExcludeDates(prisonCode, emptyList())

    visitSchedulerMockServer.stubGetCurrentOrFutureSessionTemplates(prisonCode, null, HttpStatus.NOT_FOUND)

    // When
    val responseSpec = callGetPrisonAndSessionTemplateFutureExcludeDates(webTestClient, prisonCode, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
    verify(visitSchedulerClientSpy, times(1)).getPrisonExcludeDates(any())
    verify(visitSchedulerClientSpy, times(1)).getCurrentOrFutureSessionTemplates(any())
    verify(visitSchedulerClientSpy, times(0)).getSessionTemplateExcludeDates(any())
    verify(manageUsersApiClientSpy, times(0)).getUserDetails(any())
  }

  @Test
  fun `when BAD_REQUEST is returned from visit scheduler get session templates then BAD_REQUEST status is sent back`() {
    // Given
    visitSchedulerMockServer.stubGetExcludeDates(prisonCode, emptyList())
    visitSchedulerMockServer.stubGetCurrentOrFutureSessionTemplates(prisonCode, null, HttpStatus.BAD_REQUEST)

    // When
    val responseSpec = callGetPrisonAndSessionTemplateFutureExcludeDates(webTestClient, prisonCode, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isBadRequest
    verify(visitSchedulerClientSpy, times(1)).getPrisonExcludeDates(any())
    verify(visitSchedulerClientSpy, times(1)).getCurrentOrFutureSessionTemplates(any())
    verify(visitSchedulerClientSpy, times(0)).getSessionTemplateExcludeDates(any())
    verify(manageUsersApiClientSpy, times(0)).getUserDetails(any())
  }

  @Test
  fun `when NOT_FOUND is returned from visit scheduler get session exclude dates then NOT_FOUND status is sent back`() {
    // Given
    visitSchedulerMockServer.stubGetExcludeDates(prisonCode, emptyList())

    // 1 future session template
    val sessionTemplate1 = SessionTemplateDto("ref-1")
    visitSchedulerMockServer.stubGetCurrentOrFutureSessionTemplates(prisonCode, listOf(sessionTemplate1))
    visitSchedulerMockServer.stubGetSessionTemplateExcludeDates(sessionTemplate1.reference, null, HttpStatus.NOT_FOUND)

    // When
    val responseSpec = callGetPrisonAndSessionTemplateFutureExcludeDates(webTestClient, prisonCode, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
    verify(visitSchedulerClientSpy, times(1)).getPrisonExcludeDates(any())
    verify(visitSchedulerClientSpy, times(1)).getCurrentOrFutureSessionTemplates(any())
    verify(visitSchedulerClientSpy, times(1)).getSessionTemplateExcludeDates(any())
    verify(manageUsersApiClientSpy, times(0)).getUserDetails(any())
  }

  @Test
  fun `when BAD_REQUEST is returned from visit scheduler get session exclude dates then BAD_REQUEST status is sent back`() {
    // Given
    visitSchedulerMockServer.stubGetExcludeDates(prisonCode, emptyList())

    // 1 future session template
    val sessionTemplate1 = SessionTemplateDto("ref-1")
    visitSchedulerMockServer.stubGetCurrentOrFutureSessionTemplates(prisonCode, listOf(sessionTemplate1))
    visitSchedulerMockServer.stubGetSessionTemplateExcludeDates(sessionTemplate1.reference, null, HttpStatus.BAD_REQUEST)

    // When
    val responseSpec = callGetPrisonAndSessionTemplateFutureExcludeDates(webTestClient, prisonCode, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isBadRequest
    verify(visitSchedulerClientSpy, times(1)).getPrisonExcludeDates(any())
    verify(visitSchedulerClientSpy, times(1)).getCurrentOrFutureSessionTemplates(any())
    verify(visitSchedulerClientSpy, times(1)).getSessionTemplateExcludeDates(any())
    verify(manageUsersApiClientSpy, times(0)).getUserDetails(any())
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): PrisonAndSessionsExcludeDatesDto = TestObjectMapper.mapper.readValue(returnResult.returnResult().responseBody, PrisonAndSessionsExcludeDatesDto::class.java)
}
