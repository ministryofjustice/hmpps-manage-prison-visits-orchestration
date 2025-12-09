package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.allocation

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.ManageUsersApiClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitAllocationApiClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller.VISIT_ORDER_HISTORY_FOR_PRISONER
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation.VisitOrderHistoryDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("Get visit order history for a prisoner")
class GetVisitOrderHistoryForPrisonerTest : IntegrationTestBase() {

  @MockitoSpyBean
  lateinit var visitAllocationApiClientSpy: VisitAllocationApiClient

  @MockitoSpyBean
  lateinit var manageUsersApiClientSpy: ManageUsersApiClient

  @Test
  fun `when prisoner has multiple visit order history then all results are returned with balance set`() {
    // Given
    val prisonerId = "ABC123"
    val fromDate = LocalDate.now().minusDays(10)
    val visitOrderHistory1 = VisitOrderHistoryDto(prisonerId, "MIGRATION", LocalDateTime.now().minusDays(10), 10, null, 3, null, userName = "user1")
    val visitOrderHistory2 = VisitOrderHistoryDto(prisonerId, "PRISONER_BALANCE_RESET", LocalDateTime.now().minusDays(9), 0, null, 0, null, userName = "user2")
    val visitOrderHistory3 = VisitOrderHistoryDto(prisonerId, "VISIT_BOOKED", LocalDateTime.now().minusDays(8), -1, null, 0, null, userName = "SYSTEM")

    // this entry needs to be ignored as balance does not change
    val visitOrderHistory4 = VisitOrderHistoryDto(prisonerId, "SYNC_FROM_NOMIS", LocalDateTime.now().minusDays(7), -1, null, 0, null, userName = "SYSTEM")
    val visitOrderHistory5 = VisitOrderHistoryDto(prisonerId, "VO_AND_PVO_ALLOCATION", LocalDateTime.now().minusDays(6), 4, null, 2, null, userName = "user3")
    visitAllocationApiMockServer.stubGetVisitOrderHistory(prisonerId, fromDate, listOf(visitOrderHistory1, visitOrderHistory2, visitOrderHistory3, visitOrderHistory4, visitOrderHistory5))
    manageUsersApiMockServer.stubGetUserDetails("user1", "John Smith")
    manageUsersApiMockServer.stubGetUserDetails("user2", "Sarah Jones")
    manageUsersApiMockServer.stubGetUserDetailsFailure("user3")

    // When
    val responseSpec = callVisitOrderHistoryForPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, prisonerId, LocalDate.now().minusDays(10))
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val activeResultsList = getResults(returnResult)

    assertThat(activeResultsList.size).isEqualTo(4)
    assertVisitOrderHistory(activeResultsList[0], visitOrderHistory1, null, null, "John Smith")
    assertVisitOrderHistory(activeResultsList[1], visitOrderHistory2, -10, -3, "Sarah Jones")
    assertVisitOrderHistory(activeResultsList[2], visitOrderHistory3, -1, 0, "SYSTEM")

    // visitOrderHistory4 not returned as no balance changed
    assertVisitOrderHistory(activeResultsList[3], visitOrderHistory5, 5, 2, "user3")
    verify(visitAllocationApiClientSpy, times(1)).getPrisonerVisitOrderHistory(prisonerId, fromDate)
    verify(manageUsersApiClientSpy, times(1)).getUserDetails("user1")
    verify(manageUsersApiClientSpy, times(1)).getUserDetails("user2")
    verify(manageUsersApiClientSpy, times(1)).getUserDetails("user3")
    verify(manageUsersApiClientSpy, times(3)).getUserDetails(any())
  }

  @Test
  fun `when prisoner has no visit order history then an empty list is returned`() {
    // Given
    val prisonerId = "ABC123"
    val fromDate = LocalDate.now().minusDays(10)
    visitAllocationApiMockServer.stubGetVisitOrderHistory(prisonerId, fromDate, emptyList())

    // When
    val responseSpec = callVisitOrderHistoryForPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, prisonerId, LocalDate.now().minusDays(10))
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val activeResultsList = getResults(returnResult)

    assertThat(activeResultsList.size).isEqualTo(0)
    verify(visitAllocationApiClientSpy, times(1)).getPrisonerVisitOrderHistory(prisonerId, fromDate)
    verify(manageUsersApiClientSpy, times(0)).getUserDetails(any())
  }

  @Test
  fun `when visit allocation call returns NOT_FOUND then NOT_FOUND is returned`() {
    // Given
    val prisonerId = "ABC123"
    val fromDate = LocalDate.now().minusDays(10)
    visitAllocationApiMockServer.stubGetVisitOrderHistory(prisonerId, fromDate, null, HttpStatus.NOT_FOUND)

    // When
    val responseSpec = callVisitOrderHistoryForPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, prisonerId, LocalDate.now().minusDays(10))
    responseSpec.expectStatus().isNotFound
    verify(visitAllocationApiClientSpy, times(1)).getPrisonerVisitOrderHistory(prisonerId, fromDate)
    verify(manageUsersApiClientSpy, times(0)).getUserDetails(any())
  }

  @Test
  fun `when visit allocation call returns INTERNAL_SERVER_ERROR then INTERNAL_SERVER_ERROR is returned`() {
    // Given
    val prisonerId = "ABC123"
    val fromDate = LocalDate.now().minusDays(10)
    visitAllocationApiMockServer.stubGetVisitOrderHistory(prisonerId, fromDate, null, HttpStatus.INTERNAL_SERVER_ERROR)

    // When
    val responseSpec = callVisitOrderHistoryForPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, prisonerId, LocalDate.now().minusDays(10))
    responseSpec.expectStatus().is5xxServerError

    verify(visitAllocationApiClientSpy, times(1)).getPrisonerVisitOrderHistory(prisonerId, fromDate)
    verify(manageUsersApiClientSpy, times(0)).getUserDetails(any())
  }

  @Test
  fun `when get visit order history is called without correct role then FORBIDDEN status is returned`() {
    // When
    val invalidRoleHeaders = setAuthorisation(roles = listOf("ROLE_INVALID"))
    val responseSpec = callVisitOrderHistoryForPrisoner(webTestClient, invalidRoleHeaders, "test", LocalDate.now().minusDays(10))

    // Then
    responseSpec.expectStatus().isForbidden

    // And
    verify(visitAllocationApiClientSpy, times(0)).getPrisonerVisitOrderHistory(any(), any())
    verify(manageUsersApiClientSpy, times(0)).getUserDetails(any())
  }

  @Test
  fun `when get visit order history is called without correct headers then UNAUTHORIZED status is returned`() {
    // When
    val url = VISIT_ORDER_HISTORY_FOR_PRISONER.replace("{prisonerId}", "prisonerId")
    val responseSpec = webTestClient.put().uri(url).exchange()

    // Then
    responseSpec.expectStatus().isUnauthorized

    // And
    verify(visitAllocationApiClientSpy, times(0)).getPrisonerVisitOrderHistory(any(), any())
    verify(manageUsersApiClientSpy, times(0)).getUserDetails(any())
  }

  private fun assertVisitOrderHistory(visitOrderHistoryDto: VisitOrderHistoryDto, expectedVisitOrderHistoryDto: VisitOrderHistoryDto, expectedVoBalanceChange: Int?, expectedPVoBalanceChange: Int?, expectedUserName: String) {
    Assertions.assertThat(visitOrderHistoryDto.prisonerId).isEqualTo(expectedVisitOrderHistoryDto.prisonerId)
    Assertions.assertThat(visitOrderHistoryDto.visitOrderHistoryType).isEqualTo(expectedVisitOrderHistoryDto.visitOrderHistoryType)
    Assertions.assertThat(visitOrderHistoryDto.voBalance).isEqualTo(expectedVisitOrderHistoryDto.voBalance)
    Assertions.assertThat(visitOrderHistoryDto.pvoBalance).isEqualTo(expectedVisitOrderHistoryDto.pvoBalance)
    Assertions.assertThat(visitOrderHistoryDto.userName).isEqualTo(expectedUserName)
    Assertions.assertThat(visitOrderHistoryDto.createdTimeStamp).isEqualTo(expectedVisitOrderHistoryDto.createdTimeStamp)
    Assertions.assertThat(visitOrderHistoryDto.comment).isEqualTo(expectedVisitOrderHistoryDto.comment)
    Assertions.assertThat(visitOrderHistoryDto.attributes).isEqualTo(expectedVisitOrderHistoryDto.attributes)
    Assertions.assertThat(visitOrderHistoryDto.voBalanceChange).isEqualTo(expectedVoBalanceChange)
    Assertions.assertThat(visitOrderHistoryDto.pvoBalanceChange).isEqualTo(expectedPVoBalanceChange)
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): List<VisitOrderHistoryDto> = objectMapper.readValue(returnResult.returnResult().responseBody, Array<VisitOrderHistoryDto>::class.java).toList()

  fun callVisitOrderHistoryForPrisoner(
    webTestClient: WebTestClient,
    authHttpHeaders: (HttpHeaders) -> Unit,
    prisonerId: String,
    fromDate: LocalDate,
  ): WebTestClient.ResponseSpec = webTestClient.get().uri("$VISIT_ORDER_HISTORY_FOR_PRISONER?fromDate=$fromDate".replace("{prisonerId}", prisonerId))
    .headers(authHttpHeaders)
    .exchange()
}
