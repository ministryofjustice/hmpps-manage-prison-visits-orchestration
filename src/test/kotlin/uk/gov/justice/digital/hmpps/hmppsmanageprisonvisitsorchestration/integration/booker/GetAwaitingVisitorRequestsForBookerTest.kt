package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.booker

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonVisitBookerRegistryClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller.PUBLIC_BOOKER_GET_VISITOR_REQUESTS_PATH
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.BookerPrisonerVisitorRequestDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import java.time.LocalDate

@DisplayName("Get awaiting visitor requests for booker")
class GetAwaitingVisitorRequestsForBookerTest : IntegrationTestBase() {

  @MockitoSpyBean
  lateinit var prisonVisitBookerRegistryClientSpy: PrisonVisitBookerRegistryClient

  @Test
  fun `when booker has awaiting visitor requests then all awaiting visitor requests are returned`() {
    // Given
    val bookerReference = "booker-ref"
    val request1 = BookerPrisonerVisitorRequestDto(reference = "1", prisonerId = "A11", firstName = "VisitorOne", lastName = "First", dateOfBirth = LocalDate.of(1980, 1, 1))
    val request2 = BookerPrisonerVisitorRequestDto(reference = "2", prisonerId = "A11", firstName = "VisitorTwo", lastName = "Second", dateOfBirth = LocalDate.of(1990, 2, 2))
    val request3 = BookerPrisonerVisitorRequestDto(reference = "3", prisonerId = "A12", firstName = "VisitorThree", lastName = "Third", dateOfBirth = LocalDate.of(2000, 3, 3))
    val request4 = BookerPrisonerVisitorRequestDto(reference = "4", prisonerId = "A12", firstName = "VisitorFour", lastName = "Fourth", dateOfBirth = LocalDate.of(2010, 4, 4))
    prisonVisitBookerRegistryMockServer.stubGetAwaitingVisitorRequestsForBooker(bookerReference, listOf(request1, request2, request3, request4))

    // When
    val responseSpec = callGetAwaitingVisitorRequestsForBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, bookerReference)
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val awaitingResultsList = getResults(returnResult)

    assertThat(awaitingResultsList.size).isEqualTo(4)
    assertVisitorRequestDetails(awaitingResultsList[0], request1)
    assertVisitorRequestDetails(awaitingResultsList[1], request2)
    assertVisitorRequestDetails(awaitingResultsList[2], request3)
    assertVisitorRequestDetails(awaitingResultsList[3], request4)
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getAwaitingVisitorRequests(bookerReference)
  }

  @Test
  fun `when booker has no awaiting visitor requests then an empty list is returned`() {
    // Given
    val bookerReference = "booker-ref"
    prisonVisitBookerRegistryMockServer.stubGetAwaitingVisitorRequestsForBooker(bookerReference, emptyList())

    // When
    val responseSpec = callGetAwaitingVisitorRequestsForBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, bookerReference)
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val awaitingResultsList = getResults(returnResult)

    assertThat(awaitingResultsList.size).isEqualTo(0)
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getAwaitingVisitorRequests(bookerReference)
  }

  @Test
  fun `when booker registry call returns NOT_FOUND then an empty list is returned`() {
    // Given
    val bookerReference = "booker-ref"
    prisonVisitBookerRegistryMockServer.stubGetAwaitingVisitorRequestsForBooker(bookerReference, null, HttpStatus.NOT_FOUND)

    // When
    val responseSpec = callGetAwaitingVisitorRequestsForBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, bookerReference)
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val awaitingResultsList = getResults(returnResult)

    assertThat(awaitingResultsList.size).isEqualTo(0)
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getAwaitingVisitorRequests(bookerReference)
  }

  @Test
  fun `when booker registry call returns INTERNAL_SERVER_ERROR then INTERNAL_SERVER_ERROR is returned`() {
    // Given
    val bookerReference = "booker-ref"
    prisonVisitBookerRegistryMockServer.stubGetAwaitingVisitorRequestsForBooker(bookerReference, null, HttpStatus.INTERNAL_SERVER_ERROR)

    // When
    val responseSpec = callGetAwaitingVisitorRequestsForBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, bookerReference)
    responseSpec.expectStatus().is5xxServerError
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getAwaitingVisitorRequests(bookerReference)
  }

  private fun assertVisitorRequestDetails(bookerPrisonerVisitorRequestDto: BookerPrisonerVisitorRequestDto, requestedVisitorRequestDto: BookerPrisonerVisitorRequestDto) {
    Assertions.assertThat(bookerPrisonerVisitorRequestDto.prisonerId).isEqualTo(requestedVisitorRequestDto.prisonerId)
    Assertions.assertThat(bookerPrisonerVisitorRequestDto.firstName).isEqualTo(requestedVisitorRequestDto.firstName)
    Assertions.assertThat(bookerPrisonerVisitorRequestDto.lastName).isEqualTo(requestedVisitorRequestDto.lastName)
    Assertions.assertThat(bookerPrisonerVisitorRequestDto.dateOfBirth).isEqualTo(requestedVisitorRequestDto.dateOfBirth)
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): List<BookerPrisonerVisitorRequestDto> = objectMapper.readValue(returnResult.returnResult().responseBody, Array<BookerPrisonerVisitorRequestDto>::class.java).toList()

  fun callGetAwaitingVisitorRequestsForBooker(
    webTestClient: WebTestClient,
    authHttpHeaders: (HttpHeaders) -> Unit,
    bookerReference: String,
  ): WebTestClient.ResponseSpec = webTestClient.get().uri(PUBLIC_BOOKER_GET_VISITOR_REQUESTS_PATH.replace("{bookerReference}", bookerReference))
    .headers(authHttpHeaders)
    .exchange()
}
