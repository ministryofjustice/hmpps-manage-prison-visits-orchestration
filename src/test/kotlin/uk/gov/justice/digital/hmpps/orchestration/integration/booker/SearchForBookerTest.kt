package uk.gov.justice.digital.hmpps.orchestration.integration.booker

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.orchestration.client.PrisonVisitBookerRegistryClient
import uk.gov.justice.digital.hmpps.orchestration.client.SEARCH_FOR_BOOKER
import uk.gov.justice.digital.hmpps.orchestration.controller.PUBLIC_BOOKER_SEARCH
import uk.gov.justice.digital.hmpps.orchestration.dto.booker.registry.admin.BookerSearchResultsDto
import uk.gov.justice.digital.hmpps.orchestration.dto.booker.registry.admin.SearchBookerDto
import uk.gov.justice.digital.hmpps.orchestration.integration.IntegrationTestBase
import java.time.LocalDateTime

@DisplayName("Get booker via search criteria")
class SearchForBookerTest : IntegrationTestBase() {
  @MockitoSpyBean
  lateinit var prisonVisitBookerRegistryClientSpy: PrisonVisitBookerRegistryClient

  @Test
  fun `when booker exists with same email, then booker is returned`() {
    // Given
    val searchDto = SearchBookerDto(email = "test_email@test.com")
    val booker = BookerSearchResultsDto(reference = "abc-def-ghi", email = "test_email@test.com", LocalDateTime.now().minusMonths(1))
    prisonVisitBookerRegistryMockServer.stubSearchBooker(searchDto, listOf(booker))

    // When
    val responseSpec = callSearchBooker(searchDto, webTestClient, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val foundBookers = getResults(returnResult)

    Assertions.assertThat(foundBookers.size).isEqualTo(1)
    Assertions.assertThat(foundBookers[0]).isEqualTo(booker)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).searchForBooker(searchDto)
  }

  @Test
  fun `when multiple booker exists with same email, then all bookers are returned`() {
    // Given
    val searchDto = SearchBookerDto(email = "test_email@test.com")
    val booker = BookerSearchResultsDto(reference = "abc-def-ghi", email = "test_email@test.com", LocalDateTime.now().minusMonths(1))
    val otherBooker = BookerSearchResultsDto(reference = "xyz-abc-ghf", email = "test_email@test.com", LocalDateTime.now().minusMonths(2))

    prisonVisitBookerRegistryMockServer.stubSearchBooker(searchDto, listOf(booker, otherBooker))

    // When
    val responseSpec = callSearchBooker(searchDto, webTestClient, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val foundBookers = getResults(returnResult)

    Assertions.assertThat(foundBookers.size).isEqualTo(2)
    Assertions.assertThat(foundBookers[0]).isEqualTo(booker)
    Assertions.assertThat(foundBookers[1]).isEqualTo(otherBooker)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).searchForBooker(searchDto)
  }

  @Test
  fun `when booker registry returns a 404, then 404 is thrown upwards to caller`() {
    // Given
    val searchDto = SearchBookerDto(email = "test_email@test.com")
    prisonVisitBookerRegistryMockServer.stubSearchBooker(searchDto, null, HttpStatus.NOT_FOUND)

    // When
    val responseSpec = callSearchBooker(searchDto, webTestClient, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound

    verify(prisonVisitBookerRegistryClientSpy, times(1)).searchForBooker(searchDto)
  }

  @Test
  fun `when booker registry returns a internal server error, then internal server error is thrown upwards to caller`() {
    // Given
    val searchDto = SearchBookerDto(email = "test_email@test.com")
    prisonVisitBookerRegistryMockServer.stubSearchBooker(searchDto, null, HttpStatus.INTERNAL_SERVER_ERROR)

    // When
    val responseSpec = callSearchBooker(searchDto, webTestClient, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().is5xxServerError

    verify(prisonVisitBookerRegistryClientSpy, times(1)).searchForBooker(searchDto)
  }

  @Test
  fun `when booker registry is called without token then UNAUTHORIZED status is returned`() {
    // Given
    val searchDto = SearchBookerDto(email = "test_email@test.com")

    // When
    val responseSpec = webTestClient.post().uri(SEARCH_FOR_BOOKER)
      .body(BodyInserters.fromValue(searchDto))
      .exchange()

    // Then
    responseSpec.expectStatus().isUnauthorized
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): List<BookerSearchResultsDto> = objectMapper.readValue(returnResult.returnResult().responseBody, Array<BookerSearchResultsDto>::class.java).toList()

  fun callSearchBooker(
    searchBookerDto: SearchBookerDto,
    webTestClient: WebTestClient,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec = webTestClient.post().uri(PUBLIC_BOOKER_SEARCH)
    .headers(authHttpHeaders)
    .body(BodyInserters.fromValue(searchBookerDto))
    .exchange()
}
