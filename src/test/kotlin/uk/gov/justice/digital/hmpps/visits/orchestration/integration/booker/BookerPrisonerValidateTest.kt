package uk.gov.justice.digital.hmpps.visits.orchestration.integration.booker

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.visits.orchestration.config.BookerPrisonerValidationErrorResponse
import uk.gov.justice.digital.hmpps.visits.orchestration.controller.PUBLIC_BOOKER_VALIDATE_PRISONER_CONTROLLER_PATH
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.booker.registry.enums.BookerPrisonerValidationErrorCodes.PRISONER_RELEASED
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.booker.registry.enums.BookerPrisonerValidationErrorCodes.REGISTERED_PRISON_NOT_SUPPORTED
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.enums.UserType.PUBLIC
import uk.gov.justice.digital.hmpps.visits.orchestration.integration.IntegrationTestBase
import java.time.LocalDate

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
    val prisonId = "MDI"
    val prisoner1Dto = createPrisoner(
      prisonerId = prisonerId,
      firstName = "FirstName",
      lastName = "LastName",
      dateOfBirth = LocalDate.of(2000, 1, 31),
      prisonId = prisonId,
      convictedStatus = "Convicted",
    )
    prisonVisitBookerRegistryMockServer.stubValidateBookerPrisoner(bookerReference, prisonerId, HttpStatus.OK)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisoner1Dto)
    visitSchedulerMockServer.stubGetSupportedPrisons(PUBLIC, listOf(prisonId))

    // When
    val responseSpec = callPrisonerValidate(bookerReference, prisonerId, webTestClient, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
  }

  @Test
  fun `when validate booker prisoner is unsuccessful an error response is returned`() {
    // Given
    val bookerReference = "booker-reference"
    val prisonerId = "prisoner-id"
    val prisonId = "MDI"

    val bookerPrisonerValidationErrorResponse = BookerPrisonerValidationErrorResponse(status = HttpStatus.UNPROCESSABLE_ENTITY.value(), validationError = PRISONER_RELEASED)
    val prisoner1Dto = createPrisoner(
      prisonerId = prisonerId,
      firstName = "FirstName",
      lastName = "LastName",
      dateOfBirth = LocalDate.of(2000, 1, 31),
      prisonId = prisonId,
      convictedStatus = "Convicted",
    )

    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisoner1Dto)
    visitSchedulerMockServer.stubGetSupportedPrisons(PUBLIC, listOf("ABC", prisonId))
    prisonVisitBookerRegistryMockServer.stubPrisonerValidationFailure(bookerReference, prisonerId, bookerPrisonerValidationErrorResponse)

    // When
    val responseSpec = callPrisonerValidate(bookerReference, prisonerId, webTestClient, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
    val errorResponse = getValidationErrorResponse(responseSpec)
    assertThat(errorResponse.validationError).isEqualTo(PRISONER_RELEASED)
  }

  @Test
  fun `when prisoner's prison is not supported on visit scheduler a REGISTERED_PRISON_NOT_SUPPORTED error is returned`() {
    // Given
    val bookerReference = "booker-reference"
    val prisonerId = "prisoner-id"
    val prisonId = "MDI"
    val prisoner1Dto = createPrisoner(
      prisonerId = prisonerId,
      firstName = "FirstName",
      lastName = "LastName",
      dateOfBirth = LocalDate.of(2000, 1, 31),
      prisonId = prisonId,
      convictedStatus = "Remand",
    )

    prisonVisitBookerRegistryMockServer.stubValidateBookerPrisoner(bookerReference, prisonerId, HttpStatus.OK)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisoner1Dto)

    // prison code MDI is not supported on visit-scheduler
    visitSchedulerMockServer.stubGetSupportedPrisons(PUBLIC, listOf("ABC", "XYZ"))

    // When
    val responseSpec = callPrisonerValidate(bookerReference, prisonerId, webTestClient, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
    val errorResponse = getValidationErrorResponse(responseSpec)
    assertThat(errorResponse.validationError).isEqualTo(REGISTERED_PRISON_NOT_SUPPORTED)
  }

  @Test
  fun `when prisoner offender search call returns NOT_FOUND a NOT_FOUND response is returned`() {
    // Given
    val bookerReference = "booker-reference"
    val prisonerId = "prisoner-id"
    val prisonId = "MDI"
    prisonVisitBookerRegistryMockServer.stubValidateBookerPrisoner(bookerReference, prisonerId, HttpStatus.OK)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, null, HttpStatus.NOT_FOUND)
    visitSchedulerMockServer.stubGetSupportedPrisons(PUBLIC, listOf(prisonId))

    // When
    val responseSpec = callPrisonerValidate(bookerReference, prisonerId, webTestClient, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
  }

  @Test
  fun `when prisoner offender search call returns INTERNAL_SERVER_ERROR a INTERNAL_SERVER_ERROR response is returned`() {
    // Given
    val bookerReference = "booker-reference"
    val prisonerId = "prisoner-id"
    val prisonId = "MDI"
    prisonVisitBookerRegistryMockServer.stubValidateBookerPrisoner(bookerReference, prisonerId, HttpStatus.OK)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, null, HttpStatus.INTERNAL_SERVER_ERROR)
    visitSchedulerMockServer.stubGetSupportedPrisons(PUBLIC, listOf(prisonId))

    // When
    val responseSpec = callPrisonerValidate(bookerReference, prisonerId, webTestClient, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().is5xxServerError
  }

  @Test
  fun `when visit scheduler call returns NOT_FOUND a NOT_FOUND response is returned`() {
    // Given
    val bookerReference = "booker-reference"
    val prisonerId = "prisoner-id"
    val prisonId = "MDI"
    val prisoner1Dto = createPrisoner(
      prisonerId = prisonerId,
      firstName = "FirstName",
      lastName = "LastName",
      dateOfBirth = LocalDate.of(2000, 1, 31),
      prisonId = prisonId,
      convictedStatus = "Remand",
    )
    prisonVisitBookerRegistryMockServer.stubValidateBookerPrisoner(bookerReference, prisonerId, HttpStatus.OK)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisoner1Dto)
    visitSchedulerMockServer.stubGetSupportedPrisons(PUBLIC, null, HttpStatus.NOT_FOUND)

    // When
    val responseSpec = callPrisonerValidate(bookerReference, prisonerId, webTestClient, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
  }

  @Test
  fun `when visit scheduler call returns INTERNAL_SERVER_ERROR a INTERNAL_SERVER_ERROR response is returned`() {
    // Given
    val bookerReference = "booker-reference"
    val prisonerId = "prisoner-id"
    val prisonId = "MDI"
    val prisoner1Dto = createPrisoner(
      prisonerId = prisonerId,
      firstName = "FirstName",
      lastName = "LastName",
      dateOfBirth = LocalDate.of(2000, 1, 31),
      prisonId = prisonId,
      convictedStatus = "Remand",
    )
    prisonVisitBookerRegistryMockServer.stubValidateBookerPrisoner(bookerReference, prisonerId, HttpStatus.OK)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisoner1Dto)
    visitSchedulerMockServer.stubGetSupportedPrisons(PUBLIC, null, HttpStatus.INTERNAL_SERVER_ERROR)

    // When
    val responseSpec = callPrisonerValidate(bookerReference, prisonerId, webTestClient, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().is5xxServerError
  }

  fun getValidationErrorResponse(responseSpec: WebTestClient.ResponseSpec): BookerPrisonerValidationErrorResponse = objectMapper.readValue(responseSpec.expectBody().returnResult().responseBody, BookerPrisonerValidationErrorResponse::class.java)
}
