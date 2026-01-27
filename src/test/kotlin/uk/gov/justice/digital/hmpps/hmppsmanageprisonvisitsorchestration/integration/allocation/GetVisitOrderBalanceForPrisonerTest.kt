package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.allocation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitAllocationApiClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller.VISIT_ORDER_PRISONER_BALANCE_ENDPOINT
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation.PrisonerBalanceDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation.VisitOrderPrisonerBalanceDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.TestObjectMapper
import java.time.LocalDate

@DisplayName("GET $VISIT_ORDER_PRISONER_BALANCE_ENDPOINT")
class GetVisitOrderBalanceForPrisonerTest : IntegrationTestBase() {
  val prisonerId = "ABC123"
  val prisonId = "HEI"

  @MockitoSpyBean
  lateinit var visitAllocationApiClientSpy: VisitAllocationApiClient

  @MockitoSpyBean
  lateinit var prisonerSearchApiClientSpy: PrisonerSearchClient

  @Test
  fun `when visit allocation is called, with a valid request to get prisoners balance, then OK is returned`() {
    // Given
    val visitOrderPrisonerBalanceDto = VisitOrderPrisonerBalanceDto(
      prisonerId = prisonerId,
      voBalance = 5,
      pvoBalance = 2,
    )

    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, createPrisoner(prisonerId, "John", "Smith", LocalDate.now().minusYears(21), prisonId, convictedStatus = "Convicted"))
    visitAllocationApiMockServer.stubGetPrisonerVOBalance(prisonerId, visitOrderPrisonerBalanceDto)

    // When
    val responseSpec = callGetVisitOrderBalanceForPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, prisonerId, prisonId)
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerBalanceDto = getResults(returnResult)

    assertThat(prisonerBalanceDto.prisonerId).isEqualTo(prisonerId)
    assertThat(prisonerBalanceDto.voBalance).isEqualTo(5)
    assertThat(prisonerBalanceDto.pvoBalance).isEqualTo(2)
    assertThat(prisonerBalanceDto.firstName).isEqualTo("John")
    assertThat(prisonerBalanceDto.lastName).isEqualTo("Smith")

    verify(prisonerSearchApiClientSpy, times(1)).getPrisonerById(prisonerId)
    verify(visitAllocationApiClientSpy, times(1)).getPrisonerVOBalance(prisonerId)
  }

  @Test
  fun `when visit allocation call returns BAD_REQUEST then BAD_REQUEST is returned`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, createPrisoner(prisonerId, "John", "Smith", LocalDate.now().minusYears(21), prisonId, convictedStatus = "Convicted"))
    visitAllocationApiMockServer.stubGetPrisonerVOBalance(prisonerId, null, HttpStatus.BAD_REQUEST)

    // When
    val responseSpec = callGetVisitOrderBalanceForPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, prisonerId, prisonId)
    responseSpec.expectStatus().isBadRequest

    verify(prisonerSearchApiClientSpy, times(1)).getPrisonerById(prisonerId)
    verify(visitAllocationApiClientSpy, times(1)).getPrisonerVOBalance(prisonerId)
  }

  @Test
  fun `when visit order api is called and returns INTERNAL_SERVER_ERROR then INTERNAL_SERVER_ERROR is returned`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, createPrisoner(prisonerId, "John", "Smith", LocalDate.now().minusYears(21), prisonId, convictedStatus = "Convicted"))
    visitAllocationApiMockServer.stubGetPrisonerVOBalance(prisonerId, null, HttpStatus.INTERNAL_SERVER_ERROR)

    // When
    val responseSpec = callGetVisitOrderBalanceForPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, prisonerId, prisonId)
    responseSpec.expectStatus().is5xxServerError

    verify(prisonerSearchApiClientSpy, times(1)).getPrisonerById(prisonerId)
    verify(visitAllocationApiClientSpy, times(1)).getPrisonerVOBalance(prisonerId)
  }

  @Test
  fun `when get prisoner's visit order balance is called without correct role then FORBIDDEN status is returned`() {
    // When
    val invalidRoleHeaders = setAuthorisation(roles = listOf("ROLE_INVALID"))
    val responseSpec = callGetVisitOrderBalanceForPrisoner(webTestClient, invalidRoleHeaders, "test", "test")

    // Then
    responseSpec.expectStatus().isForbidden

    // And
    verify(prisonerSearchApiClientSpy, times(0)).getPrisonerById(prisonerId)
    verify(visitAllocationApiClientSpy, times(0)).getPrisonerVOBalance(prisonerId)
  }

  @Test
  fun `when get prisoner's visit order balance is called without correct headers then UNAUTHORIZED status is returned`() {
    // When
    val url = VISIT_ORDER_PRISONER_BALANCE_ENDPOINT.replace("{prisonerId}", prisonerId).replace("{prisonId}", prisonId)
    val responseSpec = webTestClient.put().uri(url).exchange()

    // Then
    responseSpec.expectStatus().isUnauthorized

    // And
    verify(visitAllocationApiClientSpy, times(0)).getPrisonerVOBalance(prisonerId)
  }

  @Test
  fun `when prisoners prisonId doesnt match the staff caseload prisonId, then 400 bad request is returned`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, createPrisoner(prisonerId, "John", "Smith", LocalDate.now().minusYears(21), "wrong_code", convictedStatus = "Convicted"))

    // When
    val responseSpec = callGetVisitOrderBalanceForPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, prisonerId, prisonId)
    responseSpec.expectStatus().isBadRequest

    verify(prisonerSearchApiClientSpy, times(1)).getPrisonerById(prisonerId)
    verify(visitAllocationApiClientSpy, times(0)).getPrisonerVOBalance(prisonerId)
  }

  @Test
  fun `when prisoner search returns BAD_REQUEST then BAD_REQUEST is returned`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, null, HttpStatus.BAD_REQUEST)

    // When
    val responseSpec = callGetVisitOrderBalanceForPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, prisonerId, prisonId)
    responseSpec.expectStatus().isBadRequest

    verify(prisonerSearchApiClientSpy, times(1)).getPrisonerById(prisonerId)
    verify(visitAllocationApiClientSpy, times(0)).getPrisonerVOBalance(prisonerId)
  }

  @Test
  fun `when prisoner search returns INTERNAL_SERVER_ERROR then INTERNAL_SERVER_ERROR is returned`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, null, HttpStatus.INTERNAL_SERVER_ERROR)

    // When
    val responseSpec = callGetVisitOrderBalanceForPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, prisonerId, prisonId)
    responseSpec.expectStatus().is5xxServerError

    verify(prisonerSearchApiClientSpy, times(1)).getPrisonerById(prisonerId)
    verify(visitAllocationApiClientSpy, times(0)).getPrisonerVOBalance(prisonerId)
  }

  fun callGetVisitOrderBalanceForPrisoner(
    webTestClient: WebTestClient,
    authHttpHeaders: (HttpHeaders) -> Unit,
    prisonerId: String,
    prisonId: String,
  ): WebTestClient.ResponseSpec {
    val uri = VISIT_ORDER_PRISONER_BALANCE_ENDPOINT.replace("{prisonerId}", prisonerId).replace("{prisonId}", prisonId)

    return webTestClient.get().uri(uri)
      .headers(authHttpHeaders)
      .exchange()
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): PrisonerBalanceDto = TestObjectMapper.mapper.readValue(returnResult.returnResult().responseBody, PrisonerBalanceDto::class.java)
}
