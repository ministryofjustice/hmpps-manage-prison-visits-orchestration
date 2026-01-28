package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.booker

import org.assertj.core.api.Assertions.assertThat
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
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config.BookerVisitorRequestValidationErrorResponse
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller.PUBLIC_BOOKER_VISITOR_REQUESTS_PATH
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.AddVisitorToBookerPrisonerRequestDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.CreateVisitorRequestResponseDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.enums.VisitorRequestValidationErrorCodes
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.enums.VisitorRequestsStatus
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.TestObjectMapper
import java.time.LocalDate

class BookerAddVisitorRequestTest : IntegrationTestBase() {

  private val bookerReference = "booker-reference"
  private val prisonerId = "A12345"

  @MockitoSpyBean
  lateinit var prisonVisitBookerRegistryClientSpy: PrisonVisitBookerRegistryClient

  fun callAddVisitorRequest(
    webTestClient: WebTestClient,
    bookerReference: String,
    prisonerId: String,
    addVisitorToBookerPrisonerRequestDto: AddVisitorToBookerPrisonerRequestDto,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec = webTestClient.post().uri(PUBLIC_BOOKER_VISITOR_REQUESTS_PATH.replace("{bookerReference}", bookerReference).replace("{prisonerId}", prisonerId))
    .body(BodyInserters.fromValue(addVisitorToBookerPrisonerRequestDto))
    .headers(authHttpHeaders)
    .exchange()

  @Test
  fun `when call to booker registry is successful a CREATED response code is returned`() {
    // Given
    val addVisitorRequest = AddVisitorToBookerPrisonerRequestDto("Test", "User", LocalDate.of(2000, 1, 1))
    val response = CreateVisitorRequestResponseDto(reference = "random-ref", status = VisitorRequestsStatus.REQUESTED, bookerReference = bookerReference, prisonerId = prisonerId)
    prisonVisitBookerRegistryMockServer.stubAddVisitorRequest(bookerReference, prisonerId, addVisitorRequest, response, HttpStatus.CREATED)

    // When
    val responseSpec = callAddVisitorRequest(webTestClient, bookerReference, prisonerId, addVisitorRequest, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isCreated
    verify(prisonVisitBookerRegistryClientSpy, times(1)).createAddVisitorRequest(bookerReference, prisonerId, addVisitorRequest)
  }

  @Test
  fun `when call to booker registry throws a validation error then validation error is returned`() {
    // Given
    val addVisitorRequest = AddVisitorToBookerPrisonerRequestDto("Test", "User", LocalDate.of(2000, 1, 1))
    val bookerVisitorRequestValidationErrorResponse = BookerVisitorRequestValidationErrorResponse(status = HttpStatus.UNPROCESSABLE_CONTENT.value(), validationError = VisitorRequestValidationErrorCodes.VISITOR_ALREADY_EXISTS)
    prisonVisitBookerRegistryMockServer.stubAddVisitorRequestValidationFailure(bookerReference, prisonerId, bookerVisitorRequestValidationErrorResponse)

    // When
    val responseSpec = callAddVisitorRequest(webTestClient, bookerReference, prisonerId, addVisitorRequest, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT)
    val errorResponse = getValidationErrorResponse(responseSpec)
    assertThat(errorResponse.validationError).isEqualTo(VisitorRequestValidationErrorCodes.VISITOR_ALREADY_EXISTS)
    verify(prisonVisitBookerRegistryClientSpy, times(1)).createAddVisitorRequest(bookerReference, prisonerId, addVisitorRequest)
  }

  @Test
  fun `when call to booker registry fails with a NOT_FOUND error then NOT_FOUND error code is returned`() {
    // Given
    val addVisitorRequest = AddVisitorToBookerPrisonerRequestDto("Test", "User", LocalDate.of(2000, 1, 1))
    val response = CreateVisitorRequestResponseDto(reference = "random-ref", status = VisitorRequestsStatus.REQUESTED, bookerReference = bookerReference, prisonerId = prisonerId)
    prisonVisitBookerRegistryMockServer.stubAddVisitorRequest(bookerReference, prisonerId, addVisitorRequest, response, HttpStatus.NOT_FOUND)

    // When
    val responseSpec = callAddVisitorRequest(webTestClient, bookerReference, prisonerId, addVisitorRequest, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
    verify(prisonVisitBookerRegistryClientSpy, times(1)).createAddVisitorRequest(bookerReference, prisonerId, addVisitorRequest)
  }

  @Test
  fun `when call to booker registry fails with an INTERNAL_SERVER_ERROR error then INTERNAL_SERVER_ERROR error code is returned`() {
    // Given
    val addVisitorRequest = AddVisitorToBookerPrisonerRequestDto("Test", "User", LocalDate.of(2000, 1, 1))
    val response = CreateVisitorRequestResponseDto(reference = "random-ref", status = VisitorRequestsStatus.REQUESTED, bookerReference = bookerReference, prisonerId = prisonerId)

    prisonVisitBookerRegistryMockServer.stubAddVisitorRequest(bookerReference, prisonerId, addVisitorRequest, response, HttpStatus.INTERNAL_SERVER_ERROR)

    // When
    val responseSpec = callAddVisitorRequest(webTestClient, bookerReference, prisonerId, addVisitorRequest, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().is5xxServerError
    verify(prisonVisitBookerRegistryClientSpy, times(1)).createAddVisitorRequest(bookerReference, prisonerId, addVisitorRequest)
  }

  @Test
  fun `when booker authorisation is called without correct role then FORBIDDEN status is returned`() {
    // When
    val addVisitorRequest = AddVisitorToBookerPrisonerRequestDto("Test", "User", LocalDate.of(2000, 1, 1))
    val response = CreateVisitorRequestResponseDto(reference = "random-ref", status = VisitorRequestsStatus.REQUESTED, bookerReference = bookerReference, prisonerId = prisonerId)

    prisonVisitBookerRegistryMockServer.stubAddVisitorRequest(bookerReference, prisonerId, addVisitorRequest, response, HttpStatus.CREATED)

    // When
    val responseSpec = callAddVisitorRequest(webTestClient, bookerReference, prisonerId, addVisitorRequest, authHttpHeaders = setAuthorisation(roles = listOf()))

    // Then
    responseSpec.expectStatus().isForbidden

    // And
    verify(prisonVisitBookerRegistryClientSpy, times(0)).createAddVisitorRequest(any(), any(), any())
  }

  @Test
  fun `when  booker authorisation is called without token then UNAUTHORIZED status is returned`() {
    // When
    val url = PUBLIC_BOOKER_VISITOR_REQUESTS_PATH.replace("{bookerReference}", bookerReference).replace("{prisonerId}", prisonerId)

    val responseSpec = webTestClient.put().uri(url)
      .exchange()

    // Then
    responseSpec.expectStatus().isUnauthorized

    // And
    verify(prisonVisitBookerRegistryClientSpy, times(0)).createAddVisitorRequest(any(), any(), any())
  }

  fun getValidationErrorResponse(responseSpec: WebTestClient.ResponseSpec): BookerVisitorRequestValidationErrorResponse = TestObjectMapper.mapper.readValue(responseSpec.expectBody().returnResult().responseBody, BookerVisitorRequestValidationErrorResponse::class.java)
}
