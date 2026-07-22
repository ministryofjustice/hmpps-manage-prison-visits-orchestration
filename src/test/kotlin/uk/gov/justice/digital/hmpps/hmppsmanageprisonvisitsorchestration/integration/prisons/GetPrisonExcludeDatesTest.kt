package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.prisons

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.manage.users.UserExtendedDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.prisons.ExcludeDateDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.TestObjectMapper
import java.time.LocalDate

@DisplayName("Get prison exclude dates tests")
class GetPrisonExcludeDatesTest : IntegrationTestBase() {
  final val prisonCode = "HEI"

  fun callGetPastExcludeDates(
    webTestClient: WebTestClient,
    prisonCode: String,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec = webTestClient.get().uri("/config/prisons/prison/$prisonCode/exclude-date/past")
    .headers(authHttpHeaders)
    .exchange()

  @Test
  fun `when prison has past and future exclude dates only past ones are returned on call to get past exclude dates`() {
    // Given
    val userIds = listOf("user-11", "user-12", "user-13")
    val userNamesMap = mapOf(
      "user-11" to UserExtendedDetailsDto("user-11", "User", "Eleven"),
      "user-13" to UserExtendedDetailsDto("user-11", "User", "Thirteen"),
    )

    val excludeDatePast1 = LocalDate.now().minusDays(1)
    val excludeDatePast2 = LocalDate.now().minusDays(2)
    val excludeDatePast3 = LocalDate.now().minusDays(3)

    val excludeDateCurrent = LocalDate.now()
    val excludeDateFuture1 = LocalDate.now().plusDays(1)
    val excludeDateFuture2 = LocalDate.now().plusDays(2)
    val excludeDateFuture3 = LocalDate.now().plusDays(3)
    val excludeDates = listOf(
      ExcludeDateDto(excludeDatePast1, "user-11"),
      ExcludeDateDto(excludeDatePast2, "user-12"),
      ExcludeDateDto(excludeDatePast3, "user-13"),
      ExcludeDateDto(excludeDateCurrent, "user-4"),
      ExcludeDateDto(excludeDateFuture1, "user-5"),
      ExcludeDateDto(excludeDateFuture2, "user-6"),
      ExcludeDateDto(excludeDateFuture3, "user-6"),
    )

    visitSchedulerMockServer.stubGetExcludeDates("HEI", excludeDates.sortedByDescending { it.excludeDate })
    // user-11 and user-13 exist on hmpps-auth but not user-12
    manageUsersApiMockServer.stubGetMultipleUserDetails(userIds, userDetails = userNamesMap)

    // When
    val responseSpec = callGetPastExcludeDates(webTestClient, "HEI", roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val fullDateExclusions = getResults(returnResult)
    Assertions.assertThat(fullDateExclusions).hasSize(3)
    Assertions.assertThat(fullDateExclusions[2]).isEqualTo(ExcludeDateDto(excludeDatePast3, "User Thirteen"))
    Assertions.assertThat(fullDateExclusions[1]).isEqualTo(ExcludeDateDto(excludeDatePast2, "user-12"))
    Assertions.assertThat(fullDateExclusions[0]).isEqualTo(ExcludeDateDto(excludeDatePast1, "User Eleven"))
    verify(manageUsersApiClientSpy, times(1)).getUsersByUsernames(any())
    verify(manageUsersApiClientSpy, times(1)).getUsersByUsernames(userIds.toSet())
  }

  @Test
  fun `when prison has only current and future exclude dates empty list is returned on call to get past exclude dates`() {
    // Given
    val excludeDateCurrent = LocalDate.now()
    val excludeDateFuture1 = LocalDate.now().plusDays(1)
    val excludeDateFuture2 = LocalDate.now().plusDays(2)
    val excludeDateFuture3 = LocalDate.now().plusDays(3)

    val excludeDates = listOf(
      ExcludeDateDto(excludeDateCurrent, "user-4"),
      ExcludeDateDto(excludeDateFuture1, "user-5"),
      ExcludeDateDto(excludeDateFuture2, "user-6"),
      ExcludeDateDto(excludeDateFuture3, "user-6"),
    )

    visitSchedulerMockServer.stubGetExcludeDates("HEI", excludeDates.sortedByDescending { it.excludeDate })

    // When
    val responseSpec = callGetPastExcludeDates(webTestClient, "HEI", roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val fullDateExclusions = getResults(returnResult)
    Assertions.assertThat(fullDateExclusions).isEmpty()
    verify(manageUsersApiClientSpy, times(0)).getUsersByUsernames(any())
  }

  @Test
  fun `when prison has no exclude dates empty list is returned on call to get past exclude dates`() {
    // Given
    visitSchedulerMockServer.stubGetExcludeDates("HEI", emptyList())

    // When
    val responseSpec = callGetPastExcludeDates(webTestClient, "HEI", roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val fullDateExclusions = getResults(returnResult)
    Assertions.assertThat(fullDateExclusions).isEmpty()
    verify(manageUsersApiClientSpy, times(0)).getUserDetails(any())
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): Array<ExcludeDateDto> = TestObjectMapper.mapper.readValue(returnResult.returnResult().responseBody, Array<ExcludeDateDto>::class.java)
}
