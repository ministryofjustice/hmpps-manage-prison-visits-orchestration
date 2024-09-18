package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.prisons

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.ManageUsersApiClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.prisons.PrisonExcludeDateDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import java.time.LocalDate

@DisplayName("Get prison exclude dates tests")
class GetPrisonExcludeDatesTest : IntegrationTestBase() {
  @SpyBean
  lateinit var manageUsersApiClientSpy: ManageUsersApiClient

  final val prisonCode = "HEI"

  fun callGetFutureExcludeDates(
    webTestClient: WebTestClient,
    prisonCode: String,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    return webTestClient.get().uri("/config/prisons/prison/$prisonCode/exclude-date/future")
      .headers(authHttpHeaders)
      .exchange()
  }

  fun callGetPastExcludeDates(
    webTestClient: WebTestClient,
    prisonCode: String,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    return webTestClient.get().uri("/config/prisons/prison/$prisonCode/exclude-date/past")
      .headers(authHttpHeaders)
      .exchange()
  }

  @Test
  fun `when prison has past and future exclude dates only future ones are returned on call to get future exclude dates`() {
    // Given
    val excludeDatePast1 = LocalDate.now().minusDays(1)
    val excludeDatePast2 = LocalDate.now().minusDays(2)
    val excludeDatePast3 = LocalDate.now().minusDays(3)

    val excludeDateCurrent = LocalDate.now()
    val excludeDateFuture1 = LocalDate.now().plusDays(1)
    val excludeDateFuture2 = LocalDate.now().plusDays(2)
    val excludeDateFuture3 = LocalDate.now().plusDays(3)
    val excludeDates = listOf(
      PrisonExcludeDateDto(excludeDatePast1, "user-1"),
      PrisonExcludeDateDto(excludeDatePast2, "user-2"),
      PrisonExcludeDateDto(excludeDatePast3, "user-3"),
      PrisonExcludeDateDto(excludeDateCurrent, "user-4"),
      PrisonExcludeDateDto(excludeDateFuture1, "user-5"),
      PrisonExcludeDateDto(excludeDateFuture2, "user-6"),
      PrisonExcludeDateDto(excludeDateFuture3, "user-6"),
    )

    visitSchedulerMockServer.stubGetExcludeDates("HEI", excludeDates.sortedByDescending { it.excludeDate })
    // user-4 and user-6 exist on hmpps-auth but not user-5
    manageUsersApiMockServer.stubGetUserDetails("user-4", "User Four")
    manageUsersApiMockServer.stubGetUserDetails("user-6", "User Six")

    // When
    val responseSpec = callGetFutureExcludeDates(webTestClient, "HEI", roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val dates = getResults(returnResult)
    Assertions.assertThat(dates).hasSize(4)
    Assertions.assertThat(dates[0]).isEqualTo(PrisonExcludeDateDto(excludeDateCurrent, "User Four"))
    Assertions.assertThat(dates[1]).isEqualTo(PrisonExcludeDateDto(excludeDateFuture1, "user-5"))
    Assertions.assertThat(dates[2]).isEqualTo(PrisonExcludeDateDto(excludeDateFuture2, "User Six"))
    Assertions.assertThat(dates[3]).isEqualTo(PrisonExcludeDateDto(excludeDateFuture3, "User Six"))
    verify(manageUsersApiClientSpy, times(3)).getUserDetails(any())
    verify(manageUsersApiClientSpy, times(1)).getUserDetails("user-4")
    verify(manageUsersApiClientSpy, times(1)).getUserDetails("user-5")
    verify(manageUsersApiClientSpy, times(1)).getUserDetails("user-6")
  }

  @Test
  fun `when prison has only today as exclude date it is returned on call to get future exclude dates`() {
    // Given
    val excludeDateCurrent = LocalDate.now()

    val excludeDates = listOf(
      PrisonExcludeDateDto(excludeDateCurrent, "user-1"),
    )

    visitSchedulerMockServer.stubGetExcludeDates("HEI", excludeDates.sortedByDescending { it.excludeDate })
    manageUsersApiMockServer.stubGetUserDetails("user-1", "User One")

    // When
    val responseSpec = callGetFutureExcludeDates(webTestClient, "HEI", roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val dates = getResults(returnResult)
    Assertions.assertThat(dates).hasSize(1)
    Assertions.assertThat(dates[0]).isEqualTo(PrisonExcludeDateDto(excludeDateCurrent, "User One"))
    verify(manageUsersApiClientSpy, times(1)).getUserDetails("user-1")
  }

  @Test
  fun `when prison has only past exclude dates empty list is returned on call to get future exclude dates`() {
    // Given
    val excludeDatePast1 = LocalDate.now().minusDays(1)
    val excludeDatePast2 = LocalDate.now().minusDays(2)
    val excludeDatePast3 = LocalDate.now().minusDays(3)

    val excludeDates = listOf(
      PrisonExcludeDateDto(excludeDatePast1, "user-1"),
      PrisonExcludeDateDto(excludeDatePast2, "user-2"),
      PrisonExcludeDateDto(excludeDatePast3, "user-3"),
    )

    visitSchedulerMockServer.stubGetExcludeDates("HEI", excludeDates.sortedByDescending { it.excludeDate })

    // When
    val responseSpec = callGetFutureExcludeDates(webTestClient, "HEI", roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val dates = getResults(returnResult)
    Assertions.assertThat(dates).isEmpty()
    verify(manageUsersApiClientSpy, times(0)).getUserDetails(any())
  }

  @Test
  fun `when prison has no exclude dates empty list is returned on call to get future exclude dates`() {
    // Given
    visitSchedulerMockServer.stubGetExcludeDates("HEI", emptyList())

    // When
    val responseSpec = callGetFutureExcludeDates(webTestClient, "HEI", roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val dates = getResults(returnResult)
    Assertions.assertThat(dates).isEmpty()
    verify(manageUsersApiClientSpy, times(0)).getUserDetails(any())
  }

  @Test
  fun `when prison has past and future exclude dates only past ones are returned on call to get past exclude dates`() {
    // Given
    val excludeDatePast1 = LocalDate.now().minusDays(1)
    val excludeDatePast2 = LocalDate.now().minusDays(2)
    val excludeDatePast3 = LocalDate.now().minusDays(3)

    val excludeDateCurrent = LocalDate.now()
    val excludeDateFuture1 = LocalDate.now().plusDays(1)
    val excludeDateFuture2 = LocalDate.now().plusDays(2)
    val excludeDateFuture3 = LocalDate.now().plusDays(3)
    val excludeDates = listOf(
      PrisonExcludeDateDto(excludeDatePast1, "user-11"),
      PrisonExcludeDateDto(excludeDatePast2, "user-12"),
      PrisonExcludeDateDto(excludeDatePast3, "user-13"),
      PrisonExcludeDateDto(excludeDateCurrent, "user-4"),
      PrisonExcludeDateDto(excludeDateFuture1, "user-5"),
      PrisonExcludeDateDto(excludeDateFuture2, "user-6"),
      PrisonExcludeDateDto(excludeDateFuture3, "user-6"),
    )

    visitSchedulerMockServer.stubGetExcludeDates("HEI", excludeDates.sortedByDescending { it.excludeDate })
    // user-11 and user-13 exist on hmpps-auth but not user-12
    manageUsersApiMockServer.stubGetUserDetails("user-11", "User Eleven")
    manageUsersApiMockServer.stubGetUserDetails("user-13", "User Thirteen")

    // When
    val responseSpec = callGetPastExcludeDates(webTestClient, "HEI", roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val dates = getResults(returnResult)
    Assertions.assertThat(dates).hasSize(3)
    Assertions.assertThat(dates[2]).isEqualTo(PrisonExcludeDateDto(excludeDatePast3, "User Thirteen"))
    Assertions.assertThat(dates[1]).isEqualTo(PrisonExcludeDateDto(excludeDatePast2, "user-12"))
    Assertions.assertThat(dates[0]).isEqualTo(PrisonExcludeDateDto(excludeDatePast1, "User Eleven"))
    verify(manageUsersApiClientSpy, times(3)).getUserDetails(any())
    verify(manageUsersApiClientSpy, times(1)).getUserDetails("user-11")
    verify(manageUsersApiClientSpy, times(1)).getUserDetails("user-12")
    verify(manageUsersApiClientSpy, times(1)).getUserDetails("user-13")
  }

  @Test
  fun `when prison has only current and future exclude dates empty list is returned on call to get past exclude dates`() {
    // Given
    val excludeDateCurrent = LocalDate.now()
    val excludeDateFuture1 = LocalDate.now().plusDays(1)
    val excludeDateFuture2 = LocalDate.now().plusDays(2)
    val excludeDateFuture3 = LocalDate.now().plusDays(3)

    val excludeDates = listOf(
      PrisonExcludeDateDto(excludeDateCurrent, "user-4"),
      PrisonExcludeDateDto(excludeDateFuture1, "user-5"),
      PrisonExcludeDateDto(excludeDateFuture2, "user-6"),
      PrisonExcludeDateDto(excludeDateFuture3, "user-6"),
    )

    visitSchedulerMockServer.stubGetExcludeDates("HEI", excludeDates.sortedByDescending { it.excludeDate })

    // When
    val responseSpec = callGetPastExcludeDates(webTestClient, "HEI", roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val dates = getResults(returnResult)
    Assertions.assertThat(dates).isEmpty()
    verify(manageUsersApiClientSpy, times(0)).getUserDetails(any())
  }

  @Test
  fun `when prison has no exclude dates empty list is returned on call to get past exclude dates`() {
    // Given
    visitSchedulerMockServer.stubGetExcludeDates("HEI", emptyList())

    // When
    val responseSpec = callGetPastExcludeDates(webTestClient, "HEI", roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val dates = getResults(returnResult)
    Assertions.assertThat(dates).isEmpty()
    verify(manageUsersApiClientSpy, times(0)).getUserDetails(any())
  }

  @Test
  fun `when NOT_FOUND is returned from visit scheduler then NOT_FOUND status is sent back`() {
    // Given
    val prisonCode = "HEI"
    visitSchedulerMockServer.stubGetExcludeDates(prisonCode, null, HttpStatus.NOT_FOUND)

    // When
    val responseSpec = callGetFutureExcludeDates(webTestClient, prisonCode, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
  }

  @Test
  fun `when BAD_REQUEST is returned from visit scheduler then BAD_REQUEST status is sent back`() {
    // Given
    visitSchedulerMockServer.stubGetExcludeDates("HEI", null, HttpStatus.BAD_REQUEST)

    // When
    val responseSpec = callGetFutureExcludeDates(webTestClient, "HEI", roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isBadRequest
    verify(manageUsersApiClientSpy, times(0)).getUserDetails(any())
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): Array<PrisonExcludeDateDto> {
    return objectMapper.readValue(returnResult.returnResult().responseBody, Array<PrisonExcludeDateDto>::class.java)
  }
}
