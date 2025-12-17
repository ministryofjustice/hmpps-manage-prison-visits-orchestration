package uk.gov.justice.digital.hmpps.prison.visits.orchestration.integration.booker

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.client.PrisonVisitBookerRegistryClient
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.controller.PUBLIC_BOOKER_APPROVE_VISITOR_REQUEST
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.booker.registry.ApproveVisitorRequestDto
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.booker.registry.PrisonVisitorRequestDto
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.booker.registry.enums.VisitorRequestsStatus.APPROVED
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.integration.IntegrationTestBase
import java.time.LocalDate

@DisplayName("PUT Approve visitor request tests - $PUBLIC_BOOKER_APPROVE_VISITOR_REQUEST")
class ApproveVisitorRequestTest : IntegrationTestBase() {

  private val requestReference = "abc-def-ghi"

  @MockitoSpyBean
  lateinit var prisonVisitBookerRegistryClientSpy: PrisonVisitBookerRegistryClient

  @Test
  fun `when call to approve visitor request on booker-registry is successful a successful response code is returned`() {
    // Given
    val approveVisitorRequestDto = ApproveVisitorRequestDto(visitorId = 123456L)
    val approveVisitorRequestResponse = PrisonVisitorRequestDto(
      requestReference,
      bookerReference = "abc-def-ghi",
      bookerEmail = "test@test.com",
      prisonerId = "AA123456",
      firstName = "John",
      lastName = "Smith",
      dateOfBirth = LocalDate.now().minusYears(21),
      requestedOn = LocalDate.now(),
      status = APPROVED,
    )

    prisonVisitBookerRegistryMockServer.stubApproveVisitorRequest(requestReference, approveVisitorRequestResponse, HttpStatus.OK)

    // When
    val responseSpec = callApproveVisitorRequest(webTestClient, requestReference, approveVisitorRequestDto, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().is2xxSuccessful

    verify(prisonVisitBookerRegistryClientSpy, times(1)).approveAndLinkVisitorRequest(any(), any())
  }

  @Test
  fun `when call to booker registry fails with a NOT_FOUND error then NOT_FOUND error code is returned`() {
    // Given
    val approveVisitorRequestDto = ApproveVisitorRequestDto(visitorId = 123456L)

    prisonVisitBookerRegistryMockServer.stubApproveVisitorRequest(requestReference, null, HttpStatus.NOT_FOUND)

    // When
    val responseSpec = callApproveVisitorRequest(webTestClient, requestReference, approveVisitorRequestDto, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
    verify(prisonVisitBookerRegistryClientSpy, times(1)).approveAndLinkVisitorRequest(any(), any())
  }

  @Test
  fun `when call to approve visitor request on booker-registry fails with an INTERNAL_SERVER_ERROR error, then INTERNAL_SERVER_ERROR error code is returned`() {
    // Given
    val approveVisitorRequestDto = ApproveVisitorRequestDto(visitorId = 123456L)
    PrisonVisitorRequestDto(
      requestReference,
      bookerReference = "abc-def-ghi",
      bookerEmail = "test@test.com",
      prisonerId = "AA123456",
      firstName = "John",
      lastName = "Smith",
      dateOfBirth = LocalDate.now().minusYears(21),
      requestedOn = LocalDate.now(),
      status = APPROVED,
    )

    prisonVisitBookerRegistryMockServer.stubApproveVisitorRequest(requestReference, null, HttpStatus.INTERNAL_SERVER_ERROR)

    // When
    val responseSpec = callApproveVisitorRequest(webTestClient, requestReference, approveVisitorRequestDto, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().is5xxServerError
    verify(prisonVisitBookerRegistryClientSpy, times(1)).approveAndLinkVisitorRequest(any(), any())
  }

  @Test
  fun `when call to approve visitor request is made without correct role then FORBIDDEN status is returned`() {
    // Given
    val approveVisitorRequestDto = ApproveVisitorRequestDto(visitorId = 123456L)

    // When
    val responseSpec = callApproveVisitorRequest(webTestClient, requestReference, approveVisitorRequestDto, authHttpHeaders = setAuthorisation(roles = listOf()))

    // Then
    responseSpec.expectStatus().isForbidden
    verify(prisonVisitBookerRegistryClientSpy, times(0)).approveAndLinkVisitorRequest(any(), any())
  }

  @Test
  fun `when call to approve visitor request is made without token then UNAUTHORIZED status is returned`() {
    // Given
    val url = PUBLIC_BOOKER_APPROVE_VISITOR_REQUEST.replace("{requestReference}", requestReference)

    // When
    val responseSpec = webTestClient.put().uri(url).exchange()

    // Then
    responseSpec.expectStatus().isUnauthorized
    verify(prisonVisitBookerRegistryClientSpy, times(0)).approveAndLinkVisitorRequest(any(), any())
  }

  private fun callApproveVisitorRequest(
    webTestClient: WebTestClient,
    requestReference: String,
    approveVisitorRequestDto: ApproveVisitorRequestDto,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec = webTestClient.put()
    .uri(PUBLIC_BOOKER_APPROVE_VISITOR_REQUEST.replace("{requestReference}", requestReference))
    .body(BodyInserters.fromValue(approveVisitorRequestDto))
    .headers(authHttpHeaders)
    .exchange()
}
