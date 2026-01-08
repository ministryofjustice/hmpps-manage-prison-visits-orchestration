package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.allocation

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
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitAllocationApiClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller.VISIT_ORDER_PRISONER_BALANCE_ENDPOINT
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation.PrisonerBalanceAdjustmentDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation.enums.AdjustmentReasonType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase

@DisplayName("PUT $VISIT_ORDER_PRISONER_BALANCE_ENDPOINT - Manual Adjustment tests")
class AdjustVisitOrderBalanceForPrisonerTest : IntegrationTestBase() {
  val prisonerId = "ABC123"

  @MockitoSpyBean
  lateinit var visitAllocationApiClientSpy: VisitAllocationApiClient

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

    visitAllocationApiMockServer.stubAdjustPrisonersVisitOrderBalance(prisonerId, prisonerBalanceAdjustmentDto)

    // When
    val responseSpec = callAdjustPrisonersVisitOrderBalanceForPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, prisonerId, prisonerBalanceAdjustmentDto)
    responseSpec.expectStatus().isOk

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

    visitAllocationApiMockServer.stubAdjustPrisonersVisitOrderBalance(prisonerId, null, HttpStatus.NOT_FOUND)

    // When
    val responseSpec = callAdjustPrisonersVisitOrderBalanceForPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, prisonerId, prisonerBalanceAdjustmentDto)
    responseSpec.expectStatus().isNotFound

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

    visitAllocationApiMockServer.stubAdjustPrisonersVisitOrderBalance(prisonerId, null, HttpStatus.BAD_REQUEST)

    // When
    val responseSpec = callAdjustPrisonersVisitOrderBalanceForPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, prisonerId, prisonerBalanceAdjustmentDto)
    responseSpec.expectStatus().isBadRequest

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

    visitAllocationApiMockServer.stubAdjustPrisonersVisitOrderBalance(prisonerId, null, HttpStatus.INTERNAL_SERVER_ERROR)

    // When
    val responseSpec = callAdjustPrisonersVisitOrderBalanceForPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, prisonerId, prisonerBalanceAdjustmentDto)
    responseSpec.expectStatus().is5xxServerError

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
    val responseSpec = callAdjustPrisonersVisitOrderBalanceForPrisoner(webTestClient, invalidRoleHeaders, "test", prisonerBalanceAdjustmentDto)

    // Then
    responseSpec.expectStatus().isForbidden

    // And
    verify(visitAllocationApiClientSpy, times(0)).adjustPrisonersVisitOrderBalanceAsMono(any(), any())
  }

  @Test
  fun `when manual adjustment of prisoner's visit order balance is called without correct headers then UNAUTHORIZED status is returned`() {
    // When
    val url = VISIT_ORDER_PRISONER_BALANCE_ENDPOINT.replace("{prisonerId}", prisonerId)
    val responseSpec = webTestClient.put().uri(url).exchange()

    // Then
    responseSpec.expectStatus().isUnauthorized

    // And
    verify(visitAllocationApiClientSpy, times(0)).adjustPrisonersVisitOrderBalanceAsMono(any(), any())
  }

  fun callAdjustPrisonersVisitOrderBalanceForPrisoner(
    webTestClient: WebTestClient,
    authHttpHeaders: (HttpHeaders) -> Unit,
    prisonerId: String,
    prisonerBalanceAdjustmentDto: PrisonerBalanceAdjustmentDto,
  ): WebTestClient.ResponseSpec = webTestClient.put().uri(VISIT_ORDER_PRISONER_BALANCE_ENDPOINT.replace("{prisonerId}", prisonerId))
    .body(BodyInserters.fromValue(prisonerBalanceAdjustmentDto))
    .headers(authHttpHeaders)
    .exchange()
}
