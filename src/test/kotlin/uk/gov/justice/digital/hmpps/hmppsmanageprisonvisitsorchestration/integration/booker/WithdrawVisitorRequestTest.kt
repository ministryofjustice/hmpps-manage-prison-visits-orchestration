package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.booker

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller.PUBLIC_BOOKER_WITHDRAW_VISITOR_REQUEST
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.PrisonVisitorRequestDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.WithdrawVisitorRequestDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.enums.LanguagePreference
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.enums.VisitorRequestsStatus.WITHDRAWN
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import java.time.LocalDate

@DisplayName("PUT Withdraw visitor request tests - $PUBLIC_BOOKER_WITHDRAW_VISITOR_REQUEST")
class WithdrawVisitorRequestTest : IntegrationTestBase() {

  private val requestReference = "abc-def-ghi"
  private val bookerReference = "jkl-mno-pqr"

  @Test
  fun `when call to withdraw visitor request on booker-registry is successful a successful response code is returned`() {
    // Given
    val withdrawVisitorRequestDto = WithdrawVisitorRequestDto(bookerReference)

    val withdrawVisitorRequestResponse = PrisonVisitorRequestDto(
      requestReference,
      bookerReference,
      bookerEmail = "test@test.com",
      prisonerId = "AA123456",
      firstName = "John",
      lastName = "Smith",
      dateOfBirth = LocalDate.now().minusYears(21),
      requestedOn = LocalDate.now(),
      status = WITHDRAWN,
      languagePreference = LanguagePreference.EN,
    )

    prisonVisitBookerRegistryMockServer.stubWithdrawVisitorRequest(requestReference, withdrawVisitorRequestResponse, HttpStatus.OK)

    // When
    val responseSpec = callWithdrawVisitorRequest(webTestClient, requestReference, withdrawVisitorRequestDto, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().is2xxSuccessful

    verify(prisonVisitBookerRegistryClientSpy, times(1)).withdrawVisitorRequest(requestReference, withdrawVisitorRequestDto)
  }

  @Test
  fun `when call to booker registry fails with a NOT_FOUND error then NOT_FOUND error code is returned`() {
    // Given
    val withdrawVisitorRequestDto = WithdrawVisitorRequestDto(bookerReference)

    prisonVisitBookerRegistryMockServer.stubWithdrawVisitorRequest(requestReference, null, HttpStatus.NOT_FOUND)

    // When
    val responseSpec = callWithdrawVisitorRequest(webTestClient, requestReference, withdrawVisitorRequestDto, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
    verify(prisonVisitBookerRegistryClientSpy, times(1)).withdrawVisitorRequest(requestReference, withdrawVisitorRequestDto)
  }

  @Test
  fun `when call to withdraw visitor request on booker-registry fails with an INTERNAL_SERVER_ERROR error, then INTERNAL_SERVER_ERROR error code is returned`() {
    // Given
    val withdrawVisitorRequestDto = WithdrawVisitorRequestDto(bookerReference)

    prisonVisitBookerRegistryMockServer.stubWithdrawVisitorRequest(requestReference, null, HttpStatus.INTERNAL_SERVER_ERROR)

    // When
    val responseSpec = callWithdrawVisitorRequest(webTestClient, requestReference, withdrawVisitorRequestDto, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().is5xxServerError
    verify(prisonVisitBookerRegistryClientSpy, times(1)).withdrawVisitorRequest(requestReference, withdrawVisitorRequestDto)
  }

  @Test
  fun `when call to withdraw visitor request is made without correct role then FORBIDDEN status is returned`() {
    // Given
    val withdrawVisitorRequestDto = WithdrawVisitorRequestDto(bookerReference)

    // When
    val responseSpec = callWithdrawVisitorRequest(webTestClient, requestReference, withdrawVisitorRequestDto, authHttpHeaders = setAuthorisation(roles = listOf()))

    // Then
    responseSpec.expectStatus().isForbidden
    verify(prisonVisitBookerRegistryClientSpy, times(0)).withdrawVisitorRequest(any(), any())
  }

  @Test
  fun `when call to withdraw visitor request is made without token then UNAUTHORIZED status is returned`() {
    // Given
    val url = PUBLIC_BOOKER_WITHDRAW_VISITOR_REQUEST.replace("{requestReference}", requestReference)

    // When
    val responseSpec = webTestClient.put().uri(url).exchange()

    // Then
    responseSpec.expectStatus().isUnauthorized
    verify(prisonVisitBookerRegistryClientSpy, times(0)).withdrawVisitorRequest(any(), any())
  }

  private fun callWithdrawVisitorRequest(
    webTestClient: WebTestClient,
    requestReference: String,
    withdrawVisitorRequestDto: WithdrawVisitorRequestDto,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec = webTestClient.put()
    .uri(PUBLIC_BOOKER_WITHDRAW_VISITOR_REQUEST.replace("{requestReference}", requestReference))
    .headers(authHttpHeaders)
    .body(BodyInserters.fromValue(withdrawVisitorRequestDto))
    .exchange()
}
