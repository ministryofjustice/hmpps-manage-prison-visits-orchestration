package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.booker

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config.PrisonerValidationErrorResponse
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller.PUBLIC_BOOKER_VALIDATE_PRISONER_CONTROLLER_PATH
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.BookerPrisonerValidationErrorCodes.PRISONER_RELEASED
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase

@DisplayName("Validate prisoner for a public booker")
class BookerPrisonerValidateTest : IntegrationTestBase() {
  fun callPrisonerValidate(
    bookerReference: String,
    prisonerId: String,
    webTestClient: WebTestClient,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    val uri = PUBLIC_BOOKER_VALIDATE_PRISONER_CONTROLLER_PATH
      .replace("{bookerReference}", bookerReference)
      .replace("{prisonerId}", prisonerId)
    return webTestClient.get().uri(uri)
      .headers(authHttpHeaders)
      .exchange()
  }

  @Test
  fun `when validate booker prisoner is successful HTTP status OK is returned`() {
    // Given
    val bookerReference = "booker-reference"
    val prisonerId = "prisoner-id"
    prisonVisitBookerRegistryMockServer.stubValidateBookerPrisoner(bookerReference, prisonerId, HttpStatus.OK)

    // When
    val responseSpec = callPrisonerValidate(bookerReference, prisonerId, webTestClient, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
  }

  @Test
  fun `when validate booker prisoner is unsuccesful an error response is returned`() {
    // Given
    val bookerReference = "booker-reference"
    val prisonerId = "prisoner-id"
    val prisonerValidationErrorResponse = PrisonerValidationErrorResponse(status = HttpStatus.UNPROCESSABLE_ENTITY.value(), validationError = PRISONER_RELEASED)
    prisonVisitBookerRegistryMockServer.stubPrisonerValidationFailure(bookerReference, prisonerId, prisonerValidationErrorResponse)

    // When
    val responseSpec = callPrisonerValidate(bookerReference, prisonerId, webTestClient, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
    val errorResponse = getValidationErrorResponse(responseSpec)
    assertThat(errorResponse.validationError).isEqualTo(PRISONER_RELEASED)
  }

  fun getValidationErrorResponse(responseSpec: WebTestClient.ResponseSpec): PrisonerValidationErrorResponse =
    objectMapper.readValue(responseSpec.expectBody().returnResult().responseBody, PrisonerValidationErrorResponse::class.java)
}
