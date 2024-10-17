package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.sessions

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
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.prisons.ExcludeDateDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import java.time.LocalDate

@DisplayName("Get session template exclude dates tests")
class GetSessionTemplateExcludeDatesTest : IntegrationTestBase() {
  @SpyBean
  lateinit var manageUsersApiClientSpy: ManageUsersApiClient

  private final val sessionTemplateReference = "aaa-bbb-ccc"

  fun callGetSessionTemplateFutureExcludeDates(
    webTestClient: WebTestClient,
    sessionTemplateReference: String,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    return webTestClient.get().uri("/config/sessions/session/$sessionTemplateReference/exclude-date/future")
      .headers(authHttpHeaders)
      .exchange()
  }

  fun callGetSessionTemplatePastExcludeDates(
    webTestClient: WebTestClient,
    sessionTemplateReference: String,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    return webTestClient.get().uri("/config/sessions/session/$sessionTemplateReference/exclude-date/past")
      .headers(authHttpHeaders)
      .exchange()
  }

  @Test
  fun `when session template has past and future exclude dates only future ones are returned on call to get future exclude dates`() {
    // Given
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
      ExcludeDateDto(excludeDateCurrent, "user-14"),
      ExcludeDateDto(excludeDateFuture1, "user-15"),
      ExcludeDateDto(excludeDateFuture2, "user-16"),
      ExcludeDateDto(excludeDateFuture3, "user-16"),
    )

    visitSchedulerMockServer.stubGetSessionTemplateExcludeDates(sessionTemplateReference, excludeDates.sortedByDescending { it.excludeDate })
    // user-14 and user-16 exist on hmpps-auth but not user-15
    manageUsersApiMockServer.stubGetUserDetails("user-14", "User Fourteen")
    manageUsersApiMockServer.stubGetUserDetails("user-16", "User Sixteen")

    // When
    val responseSpec = callGetSessionTemplateFutureExcludeDates(webTestClient, sessionTemplateReference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val dates = getResults(returnResult)
    Assertions.assertThat(dates).hasSize(4)
    Assertions.assertThat(dates[0]).isEqualTo(ExcludeDateDto(excludeDateCurrent, "User Fourteen"))
    Assertions.assertThat(dates[1]).isEqualTo(ExcludeDateDto(excludeDateFuture1, "user-15"))
    Assertions.assertThat(dates[2]).isEqualTo(ExcludeDateDto(excludeDateFuture2, "User Sixteen"))
    Assertions.assertThat(dates[3]).isEqualTo(ExcludeDateDto(excludeDateFuture3, "User Sixteen"))
    verify(manageUsersApiClientSpy, times(3)).getUserDetails(any())
    verify(manageUsersApiClientSpy, times(1)).getUserDetails("user-14")
    verify(manageUsersApiClientSpy, times(1)).getUserDetails("user-15")
    verify(manageUsersApiClientSpy, times(1)).getUserDetails("user-16")
  }

  @Test
  fun `when session template has only today as exclude date it is returned on call to get future exclude dates`() {
    // Given
    val excludeDateCurrent = LocalDate.now()

    val excludeDates = listOf(
      ExcludeDateDto(excludeDateCurrent, "user-11"),
    )

    visitSchedulerMockServer.stubGetSessionTemplateExcludeDates(sessionTemplateReference, excludeDates.sortedByDescending { it.excludeDate })
    manageUsersApiMockServer.stubGetUserDetails("user-11", "User Eleven")

    // When
    val responseSpec = callGetSessionTemplateFutureExcludeDates(webTestClient, sessionTemplateReference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val dates = getResults(returnResult)
    Assertions.assertThat(dates).hasSize(1)
    Assertions.assertThat(dates[0]).isEqualTo(ExcludeDateDto(excludeDateCurrent, "User Eleven"))
    verify(manageUsersApiClientSpy, times(1)).getUserDetails("user-11")
  }

  @Test
  fun `when session template has only past exclude dates empty list is returned on call to get future exclude dates`() {
    // Given
    val excludeDatePast1 = LocalDate.now().minusDays(1)
    val excludeDatePast2 = LocalDate.now().minusDays(2)
    val excludeDatePast3 = LocalDate.now().minusDays(3)

    val excludeDates = listOf(
      ExcludeDateDto(excludeDatePast1, "user-11"),
      ExcludeDateDto(excludeDatePast2, "user-12"),
      ExcludeDateDto(excludeDatePast3, "user-13"),
    )

    visitSchedulerMockServer.stubGetSessionTemplateExcludeDates(sessionTemplateReference, excludeDates.sortedByDescending { it.excludeDate })

    // When
    val responseSpec = callGetSessionTemplateFutureExcludeDates(webTestClient, sessionTemplateReference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val dates = getResults(returnResult)
    Assertions.assertThat(dates).isEmpty()
    verify(manageUsersApiClientSpy, times(0)).getUserDetails(any())
  }

  @Test
  fun `when session template has no exclude dates empty list is returned on call to get future exclude dates`() {
    // Given
    visitSchedulerMockServer.stubGetSessionTemplateExcludeDates(sessionTemplateReference, emptyList())

    // When
    val responseSpec = callGetSessionTemplateFutureExcludeDates(webTestClient, sessionTemplateReference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val dates = getResults(returnResult)
    Assertions.assertThat(dates).isEmpty()
    verify(manageUsersApiClientSpy, times(0)).getUserDetails(any())
  }

  @Test
  fun `when session template has past and future exclude dates only past ones are returned on call to get past exclude dates`() {
    // Given
    val excludeDatePast1 = LocalDate.now().minusDays(1)
    val excludeDatePast2 = LocalDate.now().minusDays(2)
    val excludeDatePast3 = LocalDate.now().minusDays(3)

    val excludeDateCurrent = LocalDate.now()
    val excludeDateFuture1 = LocalDate.now().plusDays(1)
    val excludeDateFuture2 = LocalDate.now().plusDays(2)
    val excludeDateFuture3 = LocalDate.now().plusDays(3)
    val excludeDates = listOf(
      ExcludeDateDto(excludeDatePast1, "user-21"),
      ExcludeDateDto(excludeDatePast2, "user-22"),
      ExcludeDateDto(excludeDatePast3, "user-23"),
      ExcludeDateDto(excludeDateCurrent, "user-14"),
      ExcludeDateDto(excludeDateFuture1, "user-15"),
      ExcludeDateDto(excludeDateFuture2, "user-16"),
      ExcludeDateDto(excludeDateFuture3, "user-16"),
    )

    visitSchedulerMockServer.stubGetSessionTemplateExcludeDates(sessionTemplateReference, excludeDates.sortedByDescending { it.excludeDate })
    // user-21 and user-23 exist on hmpps-auth but not user-22
    manageUsersApiMockServer.stubGetUserDetails("user-21", "User TwentyOne")
    manageUsersApiMockServer.stubGetUserDetails("user-23", "User TwentyThree")

    // When
    val responseSpec = callGetSessionTemplatePastExcludeDates(webTestClient, sessionTemplateReference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val dates = getResults(returnResult)
    Assertions.assertThat(dates).hasSize(3)
    Assertions.assertThat(dates[2]).isEqualTo(ExcludeDateDto(excludeDatePast3, "User TwentyThree"))
    Assertions.assertThat(dates[1]).isEqualTo(ExcludeDateDto(excludeDatePast2, "user-22"))
    Assertions.assertThat(dates[0]).isEqualTo(ExcludeDateDto(excludeDatePast1, "User TwentyOne"))
    verify(manageUsersApiClientSpy, times(3)).getUserDetails(any())
    verify(manageUsersApiClientSpy, times(1)).getUserDetails("user-21")
    verify(manageUsersApiClientSpy, times(1)).getUserDetails("user-22")
    verify(manageUsersApiClientSpy, times(1)).getUserDetails("user-23")
  }

  @Test
  fun `when session template has only current and future exclude dates empty list is returned on call to get past exclude dates`() {
    // Given
    val excludeDateCurrent = LocalDate.now()
    val excludeDateFuture1 = LocalDate.now().plusDays(1)
    val excludeDateFuture2 = LocalDate.now().plusDays(2)
    val excludeDateFuture3 = LocalDate.now().plusDays(3)

    val excludeDates = listOf(
      ExcludeDateDto(excludeDateCurrent, "user-14"),
      ExcludeDateDto(excludeDateFuture1, "user-15"),
      ExcludeDateDto(excludeDateFuture2, "user-16"),
      ExcludeDateDto(excludeDateFuture3, "user-16"),
    )

    visitSchedulerMockServer.stubGetSessionTemplateExcludeDates(sessionTemplateReference, excludeDates.sortedByDescending { it.excludeDate })

    // When
    val responseSpec = callGetSessionTemplatePastExcludeDates(webTestClient, sessionTemplateReference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val dates = getResults(returnResult)
    Assertions.assertThat(dates).isEmpty()
    verify(manageUsersApiClientSpy, times(0)).getUserDetails(any())
  }

  @Test
  fun `when session template has no exclude dates empty list is returned on call to get past exclude dates`() {
    // Given
    visitSchedulerMockServer.stubGetSessionTemplateExcludeDates(sessionTemplateReference, emptyList())

    // When
    val responseSpec = callGetSessionTemplateFutureExcludeDates(webTestClient, sessionTemplateReference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val dates = getResults(returnResult)
    Assertions.assertThat(dates).isEmpty()
    verify(manageUsersApiClientSpy, times(0)).getUserDetails(any())
  }

  @Test
  fun `when NOT_FOUND is returned from visit scheduler then NOT_FOUND status is sent back`() {
    // Given
    visitSchedulerMockServer.stubGetSessionTemplateExcludeDates(sessionTemplateReference, null, HttpStatus.NOT_FOUND)

    // When
    val responseSpec = callGetSessionTemplateFutureExcludeDates(webTestClient, sessionTemplateReference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
  }

  @Test
  fun `when BAD_REQUEST is returned from visit scheduler then BAD_REQUEST status is sent back`() {
    // Given
    visitSchedulerMockServer.stubGetSessionTemplateExcludeDates(sessionTemplateReference, null, HttpStatus.BAD_REQUEST)

    // When
    val responseSpec = callGetSessionTemplateFutureExcludeDates(webTestClient, sessionTemplateReference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isBadRequest
    verify(manageUsersApiClientSpy, times(0)).getUserDetails(any())
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): Array<ExcludeDateDto> {
    return objectMapper.readValue(returnResult.returnResult().responseBody, Array<ExcludeDateDto>::class.java)
  }
}
