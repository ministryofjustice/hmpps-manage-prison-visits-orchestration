package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.visit

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config.ApplicationValidationErrorResponse
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.BookingOrchestrationRequestDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.ApplicationMethodType.EMAIL
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.ApplicationValidationErrorCodes.APPLICATION_INVALID_NON_ASSOCIATION_VISITS
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.ApplicationValidationErrorCodes.APPLICATION_INVALID_NO_VO_BALANCE
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase

@DisplayName("Book Visit")
class BookVisitTest : IntegrationTestBase() {

  @Test
  fun `when book visit slot is successful then OK status is returned`() {
    // Given
    val applicationReference = "aaa-bbb-ccc-ddd"
    val reference = "aa-bb-cc-dd"
    val visitDto = createVisitDto(reference = reference, applicationReference = applicationReference)
    visitSchedulerMockServer.stubBookVisit(applicationReference, visitDto)
    val requestDto = BookingOrchestrationRequestDto(actionedBy = "booker", EMAIL)

    // When
    val responseSpec = callBookVisit(webTestClient, applicationReference, requestDto, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
      .expectBody()
      .jsonPath("$.reference").isEqualTo(reference)
  }

  @Test
  fun `when book visit slot is unsuccessful then NOT_FOUND status is returned`() {
    // Given
    val applicationReference = "aaa-bbb-ccc-ddd"
    val visitDto = null
    visitSchedulerMockServer.stubBookVisit(applicationReference, visitDto)

    val requestDto = BookingOrchestrationRequestDto(actionedBy = "booker", EMAIL)

    // When
    val responseSpec = callBookVisit(webTestClient, applicationReference, requestDto, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
  }

  @Test
  fun `when book visit slot fails application validation then UNPROCESSABLE_ENTITY status is returned`() {
    // Given
    val applicationReference = "aaa-bbb-ccc-ddd"
    visitSchedulerMockServer.stubBookVisitApplicationValidationFailure(
      applicationReference,
      ApplicationValidationErrorResponse(
        status = HttpStatus.UNPROCESSABLE_ENTITY.value(),
        validationErrors = listOf(APPLICATION_INVALID_NON_ASSOCIATION_VISITS, APPLICATION_INVALID_NO_VO_BALANCE),
      ),
    )

    val requestDto = BookingOrchestrationRequestDto(actionedBy = "booker", EMAIL)

    // When
    val responseSpec = callBookVisit(webTestClient, applicationReference, requestDto, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
    val errorResponse = getValidationErrorResponse(responseSpec)
    assertThat(errorResponse.validationErrors.size).isEqualTo(2)
    assertThat(errorResponse.validationErrors).contains(APPLICATION_INVALID_NON_ASSOCIATION_VISITS)
    assertThat(errorResponse.validationErrors).contains(APPLICATION_INVALID_NO_VO_BALANCE)
  }

  @Test
  fun `when book visit slot fails application validation parsing then INTERNAL_SERVER_ERROR status is returned`() {
    // Given
    val applicationReference = "aaa-bbb-ccc-ddd"
    visitSchedulerMockServer.stubBookVisitApplicationValidationFailureInvalid(
      applicationReference,
    )

    val requestDto = BookingOrchestrationRequestDto(actionedBy = "booker", EMAIL)

    // When
    val responseSpec = callBookVisit(webTestClient, applicationReference, requestDto, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
  }

  private fun callBookVisit(
    webTestClient: WebTestClient,
    applicationReference: String,
    requestDto: BookingOrchestrationRequestDto,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    return webTestClient.put().uri("/visits/$applicationReference/book")
      .body(BodyInserters.fromValue(requestDto))
      .headers(authHttpHeaders)
      .exchange()
  }

  fun getValidationErrorResponse(responseSpec: WebTestClient.ResponseSpec): ApplicationValidationErrorResponse =
    objectMapper.readValue(responseSpec.expectBody().returnResult().responseBody, ApplicationValidationErrorResponse::class.java)
}
