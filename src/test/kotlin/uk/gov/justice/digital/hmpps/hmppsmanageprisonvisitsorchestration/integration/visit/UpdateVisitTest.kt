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
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase

@DisplayName("Update Visit")
class UpdateVisitTest : IntegrationTestBase() {
  @Test
  fun `when update visit slot is successful then OK status is returned`() {
    // Given
    val applicationReference = "aaa-bbb-ccc-ddd"
    val reference = "aa-bb-cc-dd"
    val visitDto = createVisitDto(reference = reference, applicationReference = applicationReference)
    visitSchedulerMockServer.stubGetBookedVisitByApplicationReference(applicationReference, visitDto)
    visitSchedulerMockServer.stubUpdateVisit(applicationReference, visitDto)
    val requestDto = BookingOrchestrationRequestDto(actionedBy = "booker", EMAIL, false, UserType.STAFF)

    // When
    val responseSpec = callUpdateVisit(webTestClient, applicationReference, requestDto, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
      .expectBody()
      .jsonPath("$.reference").isEqualTo(reference)
  }

  @Test
  fun `when update visit slot is unsuccessful then NOT_FOUND status is returned`() {
    // Given
    val applicationReference = "aaa-bbb-ccc-ddd"
    val reference = "aa-bb-cc-dd"
    val visitDto = createVisitDto(reference = reference, applicationReference = applicationReference)
    visitSchedulerMockServer.stubGetBookedVisitByApplicationReference(applicationReference, visitDto)
    visitSchedulerMockServer.stubUpdateVisit(applicationReference, null)

    val requestDto = BookingOrchestrationRequestDto(actionedBy = "booker", EMAIL, false, UserType.STAFF)

    // When
    val responseSpec = callUpdateVisit(webTestClient, applicationReference, requestDto, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
  }

  @Test
  fun `when update visit slot fails application validation then UNPROCESSABLE_ENTITY status is returned`() {
    // Given
    val applicationReference = "aaa-bbb-ccc-ddd"
    val reference = "aa-bb-cc-dd"
    val visitDto = createVisitDto(reference = reference, applicationReference = applicationReference)
    visitSchedulerMockServer.stubGetBookedVisitByApplicationReference(applicationReference, visitDto)

    visitSchedulerMockServer.stubUpdateVisitApplicationValidationFailure(
      applicationReference,
      ApplicationValidationErrorResponse(
        status = HttpStatus.UNPROCESSABLE_ENTITY.value(),
        validationErrors = listOf(APPLICATION_INVALID_NON_ASSOCIATION_VISITS, APPLICATION_INVALID_NO_VO_BALANCE),
      ),
    )

    val requestDto = BookingOrchestrationRequestDto(actionedBy = "booker", EMAIL, false, UserType.STAFF)

    // When
    val responseSpec = callUpdateVisit(webTestClient, applicationReference, requestDto, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
    val errorResponse = getValidationErrorResponse(responseSpec)
    assertThat(errorResponse.validationErrors.size).isEqualTo(2)
    assertThat(errorResponse.validationErrors).contains(APPLICATION_INVALID_NON_ASSOCIATION_VISITS)
    assertThat(errorResponse.validationErrors).contains(APPLICATION_INVALID_NO_VO_BALANCE)
  }

  @Test
  fun `when update visit slot fails application validation parsing then INTERNAL_SERVER_ERROR status is returned`() {
    // Given
    val applicationReference = "aaa-bbb-ccc-ddd"
    val reference = "aa-bb-cc-dd"
    val visitDto = createVisitDto(reference = reference, applicationReference = applicationReference)
    visitSchedulerMockServer.stubGetBookedVisitByApplicationReference(applicationReference, visitDto)
    visitSchedulerMockServer.stubUpdateVisitApplicationValidationFailureInvalid(applicationReference)

    val requestDto = BookingOrchestrationRequestDto(actionedBy = "booker", EMAIL, false, UserType.STAFF)

    // When
    val responseSpec = callUpdateVisit(webTestClient, applicationReference, requestDto, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
  }

  private fun callUpdateVisit(
    webTestClient: WebTestClient,
    applicationReference: String,
    requestDto: BookingOrchestrationRequestDto,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec = webTestClient.put().uri("/visits/$applicationReference/update")
    .body(BodyInserters.fromValue(requestDto))
    .headers(authHttpHeaders)
    .exchange()

  fun getValidationErrorResponse(responseSpec: WebTestClient.ResponseSpec): ApplicationValidationErrorResponse = objectMapper.readValue(responseSpec.expectBody().returnResult().responseBody, ApplicationValidationErrorResponse::class.java)
}
