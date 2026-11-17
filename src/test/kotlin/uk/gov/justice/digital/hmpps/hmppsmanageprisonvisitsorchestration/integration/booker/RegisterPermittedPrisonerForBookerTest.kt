package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.booker

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller.PUBLIC_BOOKER_REGISTER_PRISONER_CONTROLLER_PATH
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.RegisterPrisonerForBookerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.enums.BookerPrisonerRegistrationErrorCodes
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

  @ParameterizedTest
  @ValueSource(strings = ["PRISONER_ALREADY_REGISTERED_AGAINST_BOOKER", "LAST_NAME_INCORRECT, DOB_INCORRECT, PRISON_CODE_INCORRECT", "PRISONER_NOT_FOUND"])
  fun `when register booker prisoner fails validation all registry validation codes are returned`(errors: String) {
    // Given
    val bookerReference = "booker-reference"
    val registerPrisonerForBookerDto = RegisterPrisonerForBookerDto(
      prisonerId = "AA12345",
      prisonerFirstName = "James",
      prisonerLastName = "Smith",
      prisonerDateOfBirth = LocalDate.now(),
      prisonCode = "HEI",
    )

    val validationErrors = errors.split(", ").map(BookerPrisonerRegistrationErrorCodes::valueOf)
    val registryErrorResponse = """
      {
        "status": 422,
        "errorCode": null,
        "userMessage": "Prisoner registration validation failed",
        "developerMessage": "Prisoner registration validation failed with the following errors - PRISONER_NOT_FOUND",
        "moreInfo": null,
        "validationErrors": [${errors.split(", ").joinToString(", ") { "\"$it\"" }}]
      }
    """.trimIndent()

    prisonVisitBookerRegistryMockServer.stubRegisterPrisonerForBooker(bookerReference = bookerReference, httpStatus = HttpStatus.UNPROCESSABLE_CONTENT, errorResponse = registryErrorResponse)

    // When
    val responseSpec = callRegisterPrisoner(bookerReference, registerPrisonerForBookerDto, webTestClient, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT)
      .expectBody()
      .jsonPath("$.status").isEqualTo(422)
      .jsonPath("$.validationErrors").isEqualTo(validationErrors.map { it.name })
      .jsonPath("$.validationError").doesNotExist()
  }

  @ParameterizedTest
  @ValueSource(
    strings = [
      """{"status":422,"validationErrors":[]}""",
      """{"status":422}""",
      """{"status":422,"validationErrors":["UNKNOWN"]}""",
      "not-json",
    ],
  )
  fun `when registry validation response is invalid an explicit server error is returned`(registryErrorResponse: String) {
    val bookerReference = "booker-reference"
    val registerPrisonerForBookerDto = RegisterPrisonerForBookerDto(
      prisonerId = "AA12345",
      prisonerFirstName = "James",
      prisonerLastName = "Smith",
      prisonerDateOfBirth = LocalDate.now(),
      prisonCode = "HEI",
    )
    prisonVisitBookerRegistryMockServer.stubRegisterPrisonerForBooker(bookerReference, HttpStatus.UNPROCESSABLE_CONTENT, registryErrorResponse)

    val responseSpec = callRegisterPrisoner(bookerReference, registerPrisonerForBookerDto, webTestClient, roleVSIPOrchestrationServiceHttpHeaders)

    responseSpec.expectStatus().is5xxServerError
      .expectBody()
      .jsonPath("$.validationErrors").doesNotExist()
      .jsonPath("$.validationError").doesNotExist()
  }

  @ParameterizedTest
  @ValueSource(ints = [400, 404, 500])
  fun `when register booker prisoner fails with another status the registry response is forwarded unchanged`(status: Int) {
    val bookerReference = "booker-reference"
    val registerPrisonerForBookerDto = RegisterPrisonerForBookerDto(
      prisonerId = "AA12345",
      prisonerFirstName = "James",
      prisonerLastName = "Smith",
      prisonerDateOfBirth = LocalDate.now(),
      prisonCode = "HEI",
    )
    val registryErrorResponse = """{"status":$status,"userMessage":"Registration failed"}"""
    prisonVisitBookerRegistryMockServer.stubRegisterPrisonerForBooker(bookerReference, HttpStatus.valueOf(status), registryErrorResponse)

    val responseSpec = callRegisterPrisoner(bookerReference, registerPrisonerForBookerDto, webTestClient, roleVSIPOrchestrationServiceHttpHeaders)

    responseSpec.expectStatus().isEqualTo(status)
      .expectBody()
      .consumeWith { assertThat(it.responseBody).isEqualTo(registryErrorResponse.toByteArray()) }
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
