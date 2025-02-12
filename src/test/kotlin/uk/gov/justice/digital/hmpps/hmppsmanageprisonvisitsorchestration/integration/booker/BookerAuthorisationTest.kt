package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.booker

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonVisitBookerRegistryClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.AuthDetailDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.BookerReference
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase

class BookerAuthorisationTest : IntegrationTestBase() {

  val authDetail = AuthDetailDto("one-login-sub", "test@example.com")
  val bookerReference = BookerReference("reference")

  @MockitoSpyBean
  lateinit var prisonVisitBookerRegistryClientSpy: PrisonVisitBookerRegistryClient

  fun callBookerAuthorisation(
    webTestClient: WebTestClient,
    authDetails: AuthDetailDto,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec = webTestClient.put().uri("/public/booker/register/auth")
    .body(BodyInserters.fromValue(authDetails))
    .headers(authHttpHeaders)
    .exchange()

  @Test
  fun `when auth is successful a booker reference is returned`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubBookerAuthorisation(bookerReference)

    // When
    val responseSpec = callBookerAuthorisation(webTestClient, authDetail, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val bookerReference = getResults(returnResult)

    Assertions.assertThat(bookerReference.value).isEqualTo("reference")

    verify(prisonVisitBookerRegistryClientSpy, times(1)).bookerAuthorisation(any())
  }

  @Test
  fun `when auth is unsuccessful a booker reference is not returned`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubBookerAuthorisation(bookerReference, HttpStatus.NOT_FOUND)

    // When
    val responseSpec = callBookerAuthorisation(webTestClient, authDetail, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
    verify(prisonVisitBookerRegistryClientSpy, times(1)).bookerAuthorisation(any())
  }

  @Test
  fun `when auth is unsuccessful and a 401 is returned from booker registry a 401 is returned to caller`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubBookerAuthorisation(bookerReference, HttpStatus.UNAUTHORIZED)

    // When
    val responseSpec = callBookerAuthorisation(webTestClient, authDetail, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isUnauthorized
    verify(prisonVisitBookerRegistryClientSpy, times(1)).bookerAuthorisation(any())
  }

  @Test
  fun `when booker authorisation is called without correct role then FORBIDDEN status is returned`() {
    // When
    val responseSpec = callBookerAuthorisation(webTestClient, authDetail, authHttpHeaders = setAuthorisation(roles = listOf()))

    // Then
    responseSpec.expectStatus().isForbidden

    // And
    verify(prisonVisitBookerRegistryClientSpy, times(0)).bookerAuthorisation(any())
  }

  @Test
  fun `when  booker authorisation is called without token then UNAUTHORIZED status is returned`() {
    // When
    val responseSpec = webTestClient.put().uri("/public/booker/register/auth")
      .exchange()

    // Then
    responseSpec.expectStatus().isUnauthorized

    // And
    verify(prisonVisitBookerRegistryClientSpy, times(0)).bookerAuthorisation(any())
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): BookerReference = objectMapper.readValue(returnResult.returnResult().responseBody, BookerReference::class.java)
}
