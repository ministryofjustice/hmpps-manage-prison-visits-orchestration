package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.allocation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config.PrisonerBalanceAdjustmentValidationErrorResponse
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller.VISIT_ORDER_PRISONER_BALANCE_ENDPOINT
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation.PrisonerBalanceAdjustmentDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation.VisitOrderPrisonerBalanceDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation.enums.AdjustmentReasonType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation.enums.PrisonerBalanceAdjustmentValidationErrorCodes
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.TestObjectMapper
import java.time.LocalDate

@DisplayName("PUT $VISIT_ORDER_PRISONER_BALANCE_ENDPOINT - Manual Adjustment tests")
class AdjustVisitOrderBalanceForPrisonerTest : IntegrationTestBase() {
  val prisonerId = "ABC123"
  val prisonId = "HEI"

  @Test
  fun `when visit allocation is called, with a valid request to adjust balance for a prisoner, then OK is returned`() {
    // Given
    val prisonerBalanceAdjustmentDto = PrisonerBalanceAdjustmentDto(
      voAmount = 5,
      pvoAmount = null,
      adjustmentReasonType = AdjustmentReasonType.GOVERNOR_ADJUSTMENT,
      adjustmentReasonText = null,
      userName = "A_USER",
    )

    val response = VisitOrderPrisonerBalanceDto(
      prisonerId,
      voBalance = 5,
      pvoBalance = 2,
    )

    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, createPrisoner(prisonerId, "John", "Smith", LocalDate.now().minusYears(21), prisonId, convictedStatus = "Convicted"))
    visitAllocationApiMockServer.stubAdjustPrisonersVisitOrderBalance(prisonerId, response)

    // When
    val responseSpec = callAdjustPrisonersVisitOrderBalance(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, prisonerId, prisonId, prisonerBalanceAdjustmentDto)
    val result = responseSpec.expectStatus().isOk.expectBody()
    val prisonerBalanceDto = getResults(result)
    assertThat(prisonerBalanceDto.prisonerId).isEqualTo(prisonerId)

    verify(prisonerSearchClientSpy, times(1)).getPrisonerById(prisonerId)
    verify(visitAllocationApiClientSpy, times(1)).adjustPrisonersVisitOrderBalanceAsMono(prisonerId, prisonerBalanceAdjustmentDto)
  }

  @Test
  fun `when visit allocation is called, and validation errors are returned then UNPROCESSABLE_CONTENT is returned`() {
    // Given
    val prisonerBalanceAdjustmentDto = PrisonerBalanceAdjustmentDto(
      voAmount = -5,
      pvoAmount = -5,
      adjustmentReasonType = AdjustmentReasonType.GOVERNOR_ADJUSTMENT,
      adjustmentReasonText = null,
      userName = "A_USER",
    )

    val errorResponse = PrisonerBalanceAdjustmentValidationErrorResponse(status = HttpStatus.UNPROCESSABLE_CONTENT.value(), validationErrors = listOf(PrisonerBalanceAdjustmentValidationErrorCodes.VO_TOTAL_POST_ADJUSTMENT_BELOW_ZERO, PrisonerBalanceAdjustmentValidationErrorCodes.PVO_TOTAL_POST_ADJUSTMENT_BELOW_ZERO))

    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, createPrisoner(prisonerId, "John", "Smith", LocalDate.now().minusYears(21), prisonId, convictedStatus = "Convicted"))
    visitAllocationApiMockServer.stubAdjustPrisonersVisitOrderBalanceValidationFailure(prisonerId, errorResponse)

    // When
    val responseSpec = callAdjustPrisonersVisitOrderBalance(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, prisonerId, prisonId, prisonerBalanceAdjustmentDto)
    responseSpec.expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT)

    val errorResponseSpec = getValidationErrorResponse(responseSpec)
    assertThat(errorResponseSpec.validationErrors.size).isEqualTo(2)

    verify(prisonerSearchClientSpy, times(1)).getPrisonerById(prisonerId)
    verify(visitAllocationApiClientSpy, times(1)).adjustPrisonersVisitOrderBalanceAsMono(prisonerId, prisonerBalanceAdjustmentDto)
  }

  @Test
  fun `when visit allocation call returns NOT_FOUND then NOT_FOUND is returned`() {
    // Given
    val prisonerBalanceAdjustmentDto = PrisonerBalanceAdjustmentDto(
      voAmount = 5,
      pvoAmount = null,
      adjustmentReasonType = AdjustmentReasonType.GOVERNOR_ADJUSTMENT,
      adjustmentReasonText = null,
      userName = "A_USER",
    )

    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, createPrisoner(prisonerId, "John", "Smith", LocalDate.now().minusYears(21), prisonId, convictedStatus = "Convicted"))
    visitAllocationApiMockServer.stubAdjustPrisonersVisitOrderBalance(prisonerId, null, HttpStatus.NOT_FOUND)

    // When
    val responseSpec = callAdjustPrisonersVisitOrderBalance(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, prisonerId, prisonId, prisonerBalanceAdjustmentDto)
    responseSpec.expectStatus().isNotFound

    verify(prisonerSearchClientSpy, times(1)).getPrisonerById(prisonerId)
    verify(visitAllocationApiClientSpy, times(1)).adjustPrisonersVisitOrderBalanceAsMono(prisonerId, prisonerBalanceAdjustmentDto)
  }

  @Test
  fun `when visit allocation call returns BAD_REQUEST then BAD_REQUEST is returned`() {
    // Given
    val prisonerBalanceAdjustmentDto = PrisonerBalanceAdjustmentDto(
      voAmount = 5,
      pvoAmount = null,
      adjustmentReasonType = AdjustmentReasonType.GOVERNOR_ADJUSTMENT,
      adjustmentReasonText = null,
      userName = "A_USER",
    )

    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, createPrisoner(prisonerId, "John", "Smith", LocalDate.now().minusYears(21), prisonId, convictedStatus = "Convicted"))
    visitAllocationApiMockServer.stubAdjustPrisonersVisitOrderBalance(prisonerId, null, HttpStatus.BAD_REQUEST)

    // When
    val responseSpec = callAdjustPrisonersVisitOrderBalance(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, prisonerId, prisonId, prisonerBalanceAdjustmentDto)
    responseSpec.expectStatus().isBadRequest

    verify(prisonerSearchClientSpy, times(1)).getPrisonerById(prisonerId)
    verify(visitAllocationApiClientSpy, times(1)).adjustPrisonersVisitOrderBalanceAsMono(prisonerId, prisonerBalanceAdjustmentDto)
  }

  @Test
  fun `when manual adjustment of prisoner's visit order balance is called and returns INTERNAL_SERVER_ERROR then INTERNAL_SERVER_ERROR is returned`() {
    // Given
    val prisonerBalanceAdjustmentDto = PrisonerBalanceAdjustmentDto(
      voAmount = 5,
      pvoAmount = null,
      adjustmentReasonType = AdjustmentReasonType.GOVERNOR_ADJUSTMENT,
      adjustmentReasonText = null,
      userName = "A_USER",
    )

    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, createPrisoner(prisonerId, "John", "Smith", LocalDate.now().minusYears(21), prisonId, convictedStatus = "Convicted"))
    visitAllocationApiMockServer.stubAdjustPrisonersVisitOrderBalance(prisonerId, null, HttpStatus.INTERNAL_SERVER_ERROR)

    // When
    val responseSpec = callAdjustPrisonersVisitOrderBalance(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, prisonerId, prisonId, prisonerBalanceAdjustmentDto)
    responseSpec.expectStatus().is5xxServerError

    verify(prisonerSearchClientSpy, times(1)).getPrisonerById(prisonerId)
    verify(visitAllocationApiClientSpy, times(1)).adjustPrisonersVisitOrderBalanceAsMono(prisonerId, prisonerBalanceAdjustmentDto)
  }

  @Test
  fun `when manual adjustment of prisoner's visit order balance is called without correct role then FORBIDDEN status is returned`() {
    // When
    val prisonerBalanceAdjustmentDto = PrisonerBalanceAdjustmentDto(
      voAmount = 5,
      pvoAmount = null,
      adjustmentReasonType = AdjustmentReasonType.GOVERNOR_ADJUSTMENT,
      adjustmentReasonText = null,
      userName = "A_USER",
    )

    val invalidRoleHeaders = setAuthorisation(roles = listOf("ROLE_INVALID"))
    val responseSpec = callAdjustPrisonersVisitOrderBalance(webTestClient, invalidRoleHeaders, "test", "test", prisonerBalanceAdjustmentDto)

    // Then
    responseSpec.expectStatus().isForbidden

    // And
    verify(prisonerSearchClientSpy, times(0)).getPrisonerById(prisonerId)
    verify(visitAllocationApiClientSpy, times(0)).adjustPrisonersVisitOrderBalanceAsMono(any(), any())
  }

  @Test
  fun `when manual adjustment of prisoner's visit order balance is called without correct headers then UNAUTHORIZED status is returned`() {
    // When
    val url = VISIT_ORDER_PRISONER_BALANCE_ENDPOINT.replace("{prisonerId}", prisonerId).replace("{prisonId}", prisonId)
    val responseSpec = webTestClient.put().uri(url).exchange()

    // Then
    responseSpec.expectStatus().isUnauthorized

    // And
    verify(visitAllocationApiClientSpy, times(0)).adjustPrisonersVisitOrderBalanceAsMono(any(), any())
  }

  @Test
  fun `when prisoners prisonId doesnt match the staff caseload prisonId, then 400 bad request is returned`() {
    // Given
    val prisonerBalanceAdjustmentDto = PrisonerBalanceAdjustmentDto(
      voAmount = 5,
      pvoAmount = null,
      adjustmentReasonType = AdjustmentReasonType.GOVERNOR_ADJUSTMENT,
      adjustmentReasonText = null,
      userName = "A_USER",
    )

    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, createPrisoner(prisonerId, "John", "Smith", LocalDate.now().minusYears(21), "wrong_code", convictedStatus = "Convicted"))

    // When
    val responseSpec = callAdjustPrisonersVisitOrderBalance(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, prisonerId, prisonId, prisonerBalanceAdjustmentDto)
    responseSpec.expectStatus().isBadRequest

    verify(prisonerSearchClientSpy, times(1)).getPrisonerById(prisonerId)
    verify(visitAllocationApiClientSpy, times(0)).adjustPrisonersVisitOrderBalanceAsMono(prisonerId, prisonerBalanceAdjustmentDto)
  }

  @Test
  fun `when prisoner search returns BAD_REQUEST then BAD_REQUEST is returned`() {
    // Given
    val prisonerBalanceAdjustmentDto = PrisonerBalanceAdjustmentDto(
      voAmount = 5,
      pvoAmount = null,
      adjustmentReasonType = AdjustmentReasonType.GOVERNOR_ADJUSTMENT,
      adjustmentReasonText = null,
      userName = "A_USER",
    )

    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, null, HttpStatus.BAD_REQUEST)

    // When
    val responseSpec = callAdjustPrisonersVisitOrderBalance(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, prisonerId, prisonId, prisonerBalanceAdjustmentDto)
    responseSpec.expectStatus().isBadRequest

    verify(prisonerSearchClientSpy, times(1)).getPrisonerById(prisonerId)
    verify(visitAllocationApiClientSpy, times(0)).adjustPrisonersVisitOrderBalanceAsMono(prisonerId, prisonerBalanceAdjustmentDto)
  }

  @Test
  fun `when prisoner search returns INTERNAL_SERVER_ERROR then INTERNAL_SERVER_ERROR is returned`() {
    // Given
    val prisonerBalanceAdjustmentDto = PrisonerBalanceAdjustmentDto(
      voAmount = 5,
      pvoAmount = null,
      adjustmentReasonType = AdjustmentReasonType.GOVERNOR_ADJUSTMENT,
      adjustmentReasonText = null,
      userName = "A_USER",
    )

    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, null, HttpStatus.INTERNAL_SERVER_ERROR)

    // When
    val responseSpec = callAdjustPrisonersVisitOrderBalance(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, prisonerId, prisonId, prisonerBalanceAdjustmentDto)
    responseSpec.expectStatus().is5xxServerError

    verify(prisonerSearchClientSpy, times(1)).getPrisonerById(prisonerId)
    verify(visitAllocationApiClientSpy, times(0)).adjustPrisonersVisitOrderBalanceAsMono(prisonerId, prisonerBalanceAdjustmentDto)
  }

  fun callAdjustPrisonersVisitOrderBalance(
    webTestClient: WebTestClient,
    authHttpHeaders: (HttpHeaders) -> Unit,
    prisonerId: String,
    prisonId: String,
    prisonerBalanceAdjustmentDto: PrisonerBalanceAdjustmentDto,
  ): WebTestClient.ResponseSpec {
    val uri = VISIT_ORDER_PRISONER_BALANCE_ENDPOINT.replace("{prisonerId}", prisonerId).replace("{prisonId}", prisonId)

    return webTestClient.put().uri(uri)
      .body(BodyInserters.fromValue(prisonerBalanceAdjustmentDto))
      .headers(authHttpHeaders)
      .exchange()
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): VisitOrderPrisonerBalanceDto = TestObjectMapper.mapper.readValue(returnResult.returnResult().responseBody, VisitOrderPrisonerBalanceDto::class.java)

  private fun getValidationErrorResponse(responseSpec: WebTestClient.ResponseSpec): PrisonerBalanceAdjustmentValidationErrorResponse = TestObjectMapper.mapper.readValue(responseSpec.expectBody().returnResult().responseBody, PrisonerBalanceAdjustmentValidationErrorResponse::class.java)
}
