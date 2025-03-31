package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.booker

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config.BookerPrisonerRegistrationErrorResponse
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller.PUBLIC_BOOKER_REGISTER_PRISONER_CONTROLLER_PATH
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.RegisterPrisonerForBookerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.BookerPrisonerRegistrationErrorCodes
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import java.time.LocalDate

@DisplayName("$PUBLIC_BOOKER_REGISTER_PRISONER_CONTROLLER_PATH - Register a prisoner for a public booker")
class RegisterPermittedPrisonerForBookerTest : IntegrationTestBase() {
  @Test
  fun `when register prisoner for booker is successful HTTP status OK is returned`() {
    // Given
    val bookerReference = "booker-reference"
    val registerPrisonerForBookerDto = RegisterPrisonerForBookerDto(
      prisonerId = "AA12345",
      prisonerFirstName = "James",
      prisonerLastName = "Smith",
      prisonerDateOfBirth = LocalDate.now(),
      prisonCode = "HEI",
    )
    prisonVisitBookerRegistryMockServer.stubRegisterPrisonerForBooker(bookerReference = bookerReference, httpStatus = HttpStatus.OK, errorResponse = null)

    // When
    val responseSpec = callRegisterPrisoner(bookerReference, registerPrisonerForBookerDto, webTestClient, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
  }

  @Test
  fun `when register booker prisoner is unsuccessful an error response is returned`() {
    // Given
    val bookerReference = "booker-reference"
    val registerPrisonerForBookerDto = RegisterPrisonerForBookerDto(
      prisonerId = "AA12345",
      prisonerFirstName = "James",
      prisonerLastName = "Smith",
      prisonerDateOfBirth = LocalDate.now(),
      prisonCode = "HEI",
    )

    val bookerPrisonerRegistrationErrorResponse = BookerPrisonerRegistrationErrorResponse(status = HttpStatus.UNPROCESSABLE_ENTITY.value(), validationError = BookerPrisonerRegistrationErrorCodes.FAILED_REGISTRATION)

    prisonVisitBookerRegistryMockServer.stubRegisterPrisonerForBooker(bookerReference = bookerReference, httpStatus = HttpStatus.UNPROCESSABLE_ENTITY, errorResponse = bookerPrisonerRegistrationErrorResponse)

    // When
    val responseSpec = callRegisterPrisoner(bookerReference, registerPrisonerForBookerDto, webTestClient, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
    val errorResponse = getValidationErrorResponse(responseSpec)
    assertThat(errorResponse.validationError).isEqualTo(BookerPrisonerRegistrationErrorCodes.FAILED_REGISTRATION)
  }

  @Test
  fun `when register booker prisoner is called with no auth, request is rejected`() {
    // Given
    val invalidRoleHttpHeaders = setAuthorisation(roles = listOf("ROLE_INVALID"))
    val bookerReference = "booker-reference"
    val registerPrisonerForBookerDto = RegisterPrisonerForBookerDto(
      prisonerId = "AA12345",
      prisonerFirstName = "James",
      prisonerLastName = "Smith",
      prisonerDateOfBirth = LocalDate.now(),
      prisonCode = "HEI",
    )

    // When
    val responseSpec = callRegisterPrisoner(bookerReference, registerPrisonerForBookerDto, webTestClient, invalidRoleHttpHeaders)

    // Then
    responseSpec.expectStatus().isForbidden
  }

  fun getValidationErrorResponse(responseSpec: WebTestClient.ResponseSpec): BookerPrisonerRegistrationErrorResponse = objectMapper.readValue(responseSpec.expectBody().returnResult().responseBody, BookerPrisonerRegistrationErrorResponse::class.java)

  fun callRegisterPrisoner(
    bookerReference: String,
    registerPrisonerForBookerDto: RegisterPrisonerForBookerDto,
    webTestClient: WebTestClient,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec = webTestClient.post().uri(PUBLIC_BOOKER_REGISTER_PRISONER_CONTROLLER_PATH.replace("{bookerReference}", bookerReference))
    .headers(authHttpHeaders)
    .body(BodyInserters.fromValue(registerPrisonerForBookerDto))
    .exchange()
}
