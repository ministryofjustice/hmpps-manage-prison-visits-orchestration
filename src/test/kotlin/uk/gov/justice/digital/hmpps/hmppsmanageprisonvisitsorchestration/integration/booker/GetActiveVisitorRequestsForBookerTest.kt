package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.booker

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
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

@DisplayName("Get active visitor requests for booker")
class GetActiveVisitorRequestsForBookerTest : IntegrationTestBase() {

  @MockitoSpyBean
  lateinit var prisonVisitBookerRegistryClientSpy: PrisonVisitBookerRegistryClient

  @Test
  fun `when booker has active visitor requests then all active visitor requests are returned`() {
    // Given
    val bookerReference = "booker-ref"
    val request1 = BookerPrisonerVisitorRequestDto(reference = "1", prisonerId = "A11", firstName = "VisitorOne", lastName = "First", dateOfBirth = LocalDate.of(1980, 1, 1))
    val request2 = BookerPrisonerVisitorRequestDto(reference = "2", prisonerId = "A11", firstName = "VisitorTwo", lastName = "Second", dateOfBirth = LocalDate.of(1990, 2, 2))
    val request3 = BookerPrisonerVisitorRequestDto(reference = "3", prisonerId = "A12", firstName = "VisitorThree", lastName = "Third", dateOfBirth = LocalDate.of(2000, 3, 3))
    val request4 = BookerPrisonerVisitorRequestDto(reference = "4", prisonerId = "A12", firstName = "VisitorFour", lastName = "Fourth", dateOfBirth = LocalDate.of(2010, 4, 4))
    prisonVisitBookerRegistryMockServer.stubGetActiveVisitorRequestsForBooker(bookerReference, listOf(request1, request2, request3, request4))

    // When
    val responseSpec = callGetActiveVisitorRequestsForBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, bookerReference)
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val activeResultsList = getResults(returnResult)

    assertThat(activeResultsList.size).isEqualTo(4)
    assertVisitorRequestDetails(activeResultsList[0], request1)
    assertVisitorRequestDetails(activeResultsList[1], request2)
    assertVisitorRequestDetails(activeResultsList[2], request3)
    assertVisitorRequestDetails(activeResultsList[3], request4)
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getActiveVisitorRequestsForBooker(bookerReference)
  }

  @Test
  fun `when booker has no active visitor requests then an empty list is returned`() {
    // Given
    val bookerReference = "booker-ref"
    prisonVisitBookerRegistryMockServer.stubGetActiveVisitorRequestsForBooker(bookerReference, emptyList())

    // When
    val responseSpec = callGetActiveVisitorRequestsForBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, bookerReference)
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val activeResultsList = getResults(returnResult)

    assertThat(activeResultsList.size).isEqualTo(0)
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getActiveVisitorRequestsForBooker(bookerReference)
  }

  @Test
  fun `when booker registry call returns NOT_FOUND then an empty list is returned`() {
    // Given
    val bookerReference = "booker-ref"
    prisonVisitBookerRegistryMockServer.stubGetActiveVisitorRequestsForBooker(bookerReference, null, HttpStatus.NOT_FOUND)

    // When
    val responseSpec = callGetActiveVisitorRequestsForBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, bookerReference)
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val activeResultsList = getResults(returnResult)

    assertThat(activeResultsList.size).isEqualTo(0)
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getActiveVisitorRequestsForBooker(bookerReference)
  }

  @Test
  fun `when booker registry call returns INTERNAL_SERVER_ERROR then INTERNAL_SERVER_ERROR is returned`() {
    // Given
    val bookerReference = "booker-ref"
    prisonVisitBookerRegistryMockServer.stubGetActiveVisitorRequestsForBooker(bookerReference, null, HttpStatus.INTERNAL_SERVER_ERROR)

    // When
    val responseSpec = callGetActiveVisitorRequestsForBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, bookerReference)
    responseSpec.expectStatus().is5xxServerError
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getActiveVisitorRequestsForBooker(bookerReference)
  }

  @Test
  fun `when get active visitor requests is called without correct role then FORBIDDEN status is returned`() {
    // When
    val invalidRoleHeaders = setAuthorisation(roles = listOf("ROLE_INVALID"))
    val responseSpec = callGetActiveVisitorRequestsForBooker(webTestClient, invalidRoleHeaders, "test")

    // Then
    responseSpec.expectStatus().isForbidden

    // And
    verify(prisonVisitBookerRegistryClientSpy, times(0)).getActiveVisitorRequestsForBooker(any())
  }

  @Test
  fun `when get active visitor requests is called without correct role then UNAUTHORIZED status is returned`() {
    // When
    val url = PUBLIC_BOOKER_GET_VISITOR_REQUESTS_PATH.replace("{bookerReference}", "bookerReference")
    val responseSpec = webTestClient.put().uri(url).exchange()

    // Then
    responseSpec.expectStatus().isUnauthorized

    // And
    verify(prisonVisitBookerRegistryClientSpy, times(0)).getActiveVisitorRequestsForBooker(any())
  }

  private fun assertVisitorRequestDetails(bookerPrisonerVisitorRequestDto: BookerPrisonerVisitorRequestDto, requestedVisitorRequestDto: BookerPrisonerVisitorRequestDto) {
    Assertions.assertThat(bookerPrisonerVisitorRequestDto.prisonerId).isEqualTo(requestedVisitorRequestDto.prisonerId)
    Assertions.assertThat(bookerPrisonerVisitorRequestDto.firstName).isEqualTo(requestedVisitorRequestDto.firstName)
    Assertions.assertThat(bookerPrisonerVisitorRequestDto.lastName).isEqualTo(requestedVisitorRequestDto.lastName)
    Assertions.assertThat(bookerPrisonerVisitorRequestDto.dateOfBirth).isEqualTo(requestedVisitorRequestDto.dateOfBirth)
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): List<BookerPrisonerVisitorRequestDto> = objectMapper.readValue(returnResult.returnResult().responseBody, Array<BookerPrisonerVisitorRequestDto>::class.java).toList()

  fun callGetActiveVisitorRequestsForBooker(
    webTestClient: WebTestClient,
    authHttpHeaders: (HttpHeaders) -> Unit,
    bookerReference: String,
  ): WebTestClient.ResponseSpec = webTestClient.get().uri(PUBLIC_BOOKER_GET_VISITOR_REQUESTS_PATH.replace("{bookerReference}", bookerReference))
    .headers(authHttpHeaders)
    .exchange()
}
