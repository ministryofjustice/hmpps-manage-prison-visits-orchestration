package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.prisons

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
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
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitSchedulerClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.prisons.ExcludeDateDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.prisons.IsExcludeDateDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import java.time.LocalDate

@DisplayName("Is date excluded for prison tests")
class IsDateExcludedForPrisonTest : IntegrationTestBase() {
  @SpyBean
  lateinit var manageUsersApiClientSpy: ManageUsersApiClient

  @SpyBean
  lateinit var visitSchedulerClientSpy: VisitSchedulerClient

  final val prisonCode = "HEI"

  fun callIsDateExcluded(
    webTestClient: WebTestClient,
    prisonCode: String,
    date: LocalDate,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    return webTestClient.get().uri("/config/prisons/prison/$prisonCode/exclude-date/$date/isExcluded")
      .headers(authHttpHeaders)
      .exchange()
  }

  @BeforeEach
  fun setupMocks() {
    // Given
    val excludeDatePast1 = LocalDate.now().minusDays(1)
    val excludeDatePast2 = LocalDate.now().minusDays(2)
    val excludeDatePast3 = LocalDate.now().minusDays(3)

    val excludeDateCurrent = LocalDate.now()
    val excludeDateFuture1 = LocalDate.now().plusDays(1)
    val excludeDateFuture2 = LocalDate.now().plusDays(2)
    val excludeDateFuture3 = LocalDate.now().plusDays(3)
    val excludeDates = listOf(
      ExcludeDateDto(excludeDatePast1, "user-1"),
      ExcludeDateDto(excludeDatePast2, "user-2"),
      ExcludeDateDto(excludeDatePast3, "user-3"),
      ExcludeDateDto(excludeDateCurrent, "user-4"),
      ExcludeDateDto(excludeDateFuture1, "user-5"),
      ExcludeDateDto(excludeDateFuture2, "user-6"),
      ExcludeDateDto(excludeDateFuture3, "user-6"),
    )

    visitSchedulerMockServer.stubGetExcludeDates("HEI", excludeDates.sortedByDescending { it.excludeDate })
  }

  @Test
  fun `when current date is excluded by prison then is date excluded returns true`() {
    // When
    val responseSpec = callIsDateExcluded(webTestClient, "HEI", LocalDate.now(), roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val isDateExcluded = getResults(returnResult)
    Assertions.assertThat(isDateExcluded.isExcluded).isTrue()
    verify(manageUsersApiClientSpy, times(0)).getUserDetails(any())
    verify(visitSchedulerClientSpy, times(1)).getPrisonExcludeDates(prisonCode)
  }

  @Test
  fun `when future date is excluded by prison then is date excluded returns true`() {
    // When
    val responseSpec = callIsDateExcluded(webTestClient, "HEI", LocalDate.now().plusDays(1), roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val isDateExcluded = getResults(returnResult)
    Assertions.assertThat(isDateExcluded.isExcluded).isTrue()
    verify(manageUsersApiClientSpy, times(0)).getUserDetails(any())
    verify(visitSchedulerClientSpy, times(1)).getPrisonExcludeDates(prisonCode)
  }

  @Test
  fun `when past date is excluded by prison then is date excluded returns true`() {
    // When
    val responseSpec = callIsDateExcluded(webTestClient, "HEI", LocalDate.now().minusDays(1), roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val isDateExcluded = getResults(returnResult)
    Assertions.assertThat(isDateExcluded.isExcluded).isTrue()
    verify(manageUsersApiClientSpy, times(0)).getUserDetails(any())
    verify(visitSchedulerClientSpy, times(1)).getPrisonExcludeDates(prisonCode)
  }

  @Test
  fun `when past date is not excluded by prison then is date excluded returns false`() {
    // When
    val responseSpec = callIsDateExcluded(webTestClient, "HEI", LocalDate.now().minusDays(12), roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val isDateExcluded = getResults(returnResult)
    Assertions.assertThat(isDateExcluded.isExcluded).isFalse()
    verify(manageUsersApiClientSpy, times(0)).getUserDetails(any())
    verify(visitSchedulerClientSpy, times(1)).getPrisonExcludeDates(prisonCode)
  }

  @Test
  fun `when future date is not excluded by prison then is date excluded returns false`() {
    // When
    val responseSpec = callIsDateExcluded(webTestClient, "HEI", LocalDate.now().plusDays(12), roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val isDateExcluded = getResults(returnResult)
    Assertions.assertThat(isDateExcluded.isExcluded).isFalse()
    verify(manageUsersApiClientSpy, times(0)).getUserDetails(any())
    verify(visitSchedulerClientSpy, times(1)).getPrisonExcludeDates(prisonCode)
  }

  @Test
  fun `when NOT_FOUND is returned from visit scheduler then NOT_FOUND status is sent back`() {
    // Given
    val prisonCode = "HEI"
    visitSchedulerMockServer.stubGetExcludeDates(prisonCode, null, HttpStatus.NOT_FOUND)

    // When
    val responseSpec = callIsDateExcluded(webTestClient, prisonCode, LocalDate.now(), roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
  }

  @Test
  fun `when BAD_REQUEST is returned from visit scheduler then BAD_REQUEST status is sent back`() {
    // Given
    visitSchedulerMockServer.stubGetExcludeDates("HEI", null, HttpStatus.BAD_REQUEST)

    // When
    val responseSpec = callIsDateExcluded(webTestClient, "HEI", LocalDate.now(), roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isBadRequest
    verify(manageUsersApiClientSpy, times(0)).getUserDetails(any())
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): IsExcludeDateDto {
    return objectMapper.readValue(returnResult.returnResult().responseBody, IsExcludeDateDto::class.java)
  }
}
