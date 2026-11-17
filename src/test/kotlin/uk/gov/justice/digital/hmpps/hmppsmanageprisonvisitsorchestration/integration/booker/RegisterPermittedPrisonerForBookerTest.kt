package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.booker

import io.swagger.v3.core.converter.ModelConverters
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config.BookerPrisonerRegistrationErrorResponse
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller.PUBLIC_BOOKER_REGISTER_PRISONER_CONTROLLER_PATH
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.RegisterPrisonerForBookerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import java.time.LocalDate

@DisplayName("$PUBLIC_BOOKER_REGISTER_PRISONER_CONTROLLER_PATH - Register a prisoner for a public booker")
class RegisterPermittedPrisonerForBookerTest : IntegrationTestBase() {
  private val sensitiveValidationErrors = arrayOf("PRISONER_NOT_FOUND", "LAST_NAME_INCORRECT", "DOB_INCORRECT", "PRISON_CODE_INCORRECT")

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
  @CsvSource(
    delimiter = '|',
    value = [
      "PRISONER_ALREADY_REGISTERED_AGAINST_BOOKER|PRISONER_ALREADY_REGISTERED_AGAINST_BOOKER",
      "PRISONER_NOT_FOUND|FAILED_REGISTRATION",
      "LAST_NAME_INCORRECT|FAILED_REGISTRATION",
      "DOB_INCORRECT|FAILED_REGISTRATION",
      "PRISON_CODE_INCORRECT|FAILED_REGISTRATION",
      "LAST_NAME_INCORRECT, DOB_INCORRECT, PRISON_CODE_INCORRECT|FAILED_REGISTRATION",
      "PRISONER_NOT_FOUND, LAST_NAME_INCORRECT, DOB_INCORRECT, PRISON_CODE_INCORRECT|FAILED_REGISTRATION",
      "PRISONER_ALREADY_REGISTERED_AGAINST_BOOKER, LAST_NAME_INCORRECT|PRISONER_ALREADY_REGISTERED_AGAINST_BOOKER",
      "DOB_INCORRECT, PRISONER_ALREADY_REGISTERED_AGAINST_BOOKER|PRISONER_ALREADY_REGISTERED_AGAINST_BOOKER",
    ],
  )
  fun `when register booker prisoner fails validation only privacy safe codes are returned`(errors: String, publicErrorCode: String) {
    // Given
    val bookerReference = "booker-reference"
    val registerPrisonerForBookerDto = RegisterPrisonerForBookerDto(
      prisonerId = "AA12345",
      prisonerFirstName = "James",
      prisonerLastName = "Smith",
      prisonerDateOfBirth = LocalDate.now(),
      prisonCode = "HEI",
    )

    val registryErrorResponse = """
      {
        "status": 422,
        "errorCode": null,
        "userMessage": "Prisoner registration validation failed",
        "developerMessage": "Prisoner registration validation failed with the following errors - ${sensitiveValidationErrors.joinToString(", ")}",
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
      .jsonPath("$.validationError").isEqualTo(publicErrorCode)
      .jsonPath("$.validationErrors").doesNotExist()
      .consumeWith { assertThat(it.responseBody?.decodeToString()).doesNotContain(*sensitiveValidationErrors) }
  }

  @ParameterizedTest
  @ValueSource(
    strings = [
      """{"status":422,"validationErrors":[]}""",
      """{"status":422}""",
      """{"status":422,"validationErrors":["UNKNOWN"]}""",
      """{"status":422,"validationErrors":["PRISONER_NOT_FOUND","UNKNOWN"],"developerMessage":"LAST_NAME_INCORRECT DOB_INCORRECT PRISON_CODE_INCORRECT"}""",
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
      .consumeWith { assertThat(it.responseBody?.decodeToString()).doesNotContain(*sensitiveValidationErrors) }
  }

  @Test
  fun `public registration validation schema documents one privacy safe code`() {
    val schemas = ModelConverters.getInstance().readAll(BookerPrisonerRegistrationErrorResponse::class.java)
    val responseSchema = schemas.getValue("BookerPrisonerRegistrationErrorResponse")
    val validationErrorSchema = responseSchema.properties.getValue("validationError")

    assertThat(validationErrorSchema.type).isEqualTo("string")
    assertThat(validationErrorSchema.enum).containsExactly("PRISONER_ALREADY_REGISTERED_AGAINST_BOOKER", "FAILED_REGISTRATION")
    assertThat(responseSchema.properties).doesNotContainKey("validationErrors")
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
