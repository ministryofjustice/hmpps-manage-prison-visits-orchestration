package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.allocation

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.ManageUsersApiClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitAllocationApiClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller.VISIT_ORDER_HISTORY_FOR_PRISONER
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation.VisitOrderHistoryAttributesDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation.VisitOrderHistoryDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation.VisitOrderHistoryDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation.enums.VisitOrderHistoryAttributeType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation.enums.VisitOrderHistoryType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("Get visit order history for a prisoner")
class GetVisitOrderHistoryForPrisonerTest : IntegrationTestBase() {
  val prisonerId = "ABC123"

  private final val prisonerDto = createPrisoner(
    prisonerId = prisonerId,
    firstName = "First",
    lastName = "Last",
    dateOfBirth = LocalDate.of(1980, 1, 1),
    currentIncentive = createCurrentIncentive(),
    convictedStatus = "Convicted",
  )

  private val inmateDetails = createInmateDetails(prisonerId, "Category - C")

  @MockitoSpyBean
  lateinit var visitAllocationApiClientSpy: VisitAllocationApiClient

  @MockitoSpyBean
  lateinit var manageUsersApiClientSpy: ManageUsersApiClient

  @Test
  fun `when prisoner has multiple visit order history then all results are returned with balance set`() {
    // Given
    val fromDate = LocalDate.now().minusDays(10)
    val visitOrderHistory1 = VisitOrderHistoryDto(prisonerId, VisitOrderHistoryType.MIGRATION, LocalDateTime.now().minusDays(10), 10, null, 3, null, userName = "user1", attributes = emptyList())
    val visitOrderHistory2 = VisitOrderHistoryDto(prisonerId, VisitOrderHistoryType.PRISONER_BALANCE_RESET, LocalDateTime.now().minusDays(9), 0, null, 0, null, userName = "user2", attributes = emptyList())
    val visitOrderHistory3 = VisitOrderHistoryDto(prisonerId, VisitOrderHistoryType.ALLOCATION_USED_BY_VISIT, LocalDateTime.now().minusDays(8), -1, null, 0, null, userName = "SYSTEM", attributes = listOf(VisitOrderHistoryAttributesDto(VisitOrderHistoryAttributeType.VISIT_REFERENCE, "aa-bb-cc-dd")))

    // this entry needs to be ignored as balance does not change
    val visitOrderHistory4 = VisitOrderHistoryDto(prisonerId, VisitOrderHistoryType.SYNC_FROM_NOMIS, LocalDateTime.now().minusDays(7), -1, null, 0, null, userName = "SYSTEM", attributes = emptyList())
    val visitOrderHistory5 = VisitOrderHistoryDto(prisonerId, VisitOrderHistoryType.VO_AND_PVO_ALLOCATION, LocalDateTime.now().minusDays(6), 4, null, 2, null, userName = "user3", attributes = emptyList())
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(prisonerId, inmateDetails)
    visitAllocationApiMockServer.stubGetVisitOrderHistory(prisonerId, fromDate, listOf(visitOrderHistory1, visitOrderHistory2, visitOrderHistory3, visitOrderHistory4, visitOrderHistory5))
    manageUsersApiMockServer.stubGetUserDetails("user1", "John Smith")
    manageUsersApiMockServer.stubGetUserDetails("user2", "Sarah Jones")
    manageUsersApiMockServer.stubGetUserDetailsFailure("user3")

    // When
    val responseSpec = callVisitOrderHistoryForPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, prisonerId, LocalDate.now().minusDays(10))
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val visitOrderHistoryDetailsDto = getResults(returnResult)
    val visitOrderHistory = visitOrderHistoryDetailsDto.visitOrderHistory

    assertThat(visitOrderHistory.size).isEqualTo(4)
    assertVisitOrderHistory(visitOrderHistory[0], visitOrderHistory1, null, null, "John Smith")
    assertVisitOrderHistory(visitOrderHistory[1], visitOrderHistory2, -10, -3, "Sarah Jones")
    assertVisitOrderHistory(visitOrderHistory[2], visitOrderHistory3, -1, 0, "SYSTEM")

    // visitOrderHistory4 not returned as no balance changed
    assertVisitOrderHistory(visitOrderHistory[3], visitOrderHistory5, 5, 2, "user3")
    verify(visitAllocationApiClientSpy, times(1)).getVisitOrderHistoryDetails(prisonerId, fromDate)
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
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(prisonerId, inmateDetails)
    visitAllocationApiMockServer.stubGetVisitOrderHistory(prisonerId, fromDate, emptyList())

    // When
    val responseSpec = callVisitOrderHistoryForPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, prisonerId, LocalDate.now().minusDays(10))
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val visitOrderHistoryDetailsDto = getResults(returnResult)
    val visitOrderHistory = visitOrderHistoryDetailsDto.visitOrderHistory

    assertThat(visitOrderHistory.size).isEqualTo(0)
    verify(visitAllocationApiClientSpy, times(1)).getVisitOrderHistoryDetails(prisonerId, fromDate)
    verify(manageUsersApiClientSpy, times(0)).getUserDetails(any())
  }

  @Test
  fun `when prisoner search call returns NOT_FOUND then NOT_FOUND is returned`() {
    // Given
    val prisonerId = "ABC123"
    val fromDate = LocalDate.now().minusDays(10)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, null, NOT_FOUND)
    prisonApiMockServer.stubGetInmateDetails(prisonerId, inmateDetails)
    visitAllocationApiMockServer.stubGetVisitOrderHistory(prisonerId, fromDate, emptyList())

    // When
    val responseSpec = callVisitOrderHistoryForPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, prisonerId, LocalDate.now().minusDays(10))
    responseSpec.expectStatus().isNotFound
    verify(visitAllocationApiClientSpy, times(1)).getVisitOrderHistoryDetails(prisonerId, fromDate)
    verify(manageUsersApiClientSpy, times(0)).getUserDetails(any())
  }

  @Test
  fun `when prisoner search call returns INTERNAL_SERVER_ERROR then INTERNAL_SERVER_ERROR is returned`() {
    // Given
    val prisonerId = "ABC123"
    val fromDate = LocalDate.now().minusDays(10)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, null, INTERNAL_SERVER_ERROR)
    prisonApiMockServer.stubGetInmateDetails(prisonerId, inmateDetails)
    visitAllocationApiMockServer.stubGetVisitOrderHistory(prisonerId, fromDate, emptyList())

    // When
    val responseSpec = callVisitOrderHistoryForPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, prisonerId, LocalDate.now().minusDays(10))
    responseSpec.expectStatus().is5xxServerError

    verify(visitAllocationApiClientSpy, times(1)).getVisitOrderHistoryDetails(prisonerId, fromDate)
    verify(manageUsersApiClientSpy, times(0)).getUserDetails(any())
  }

  @Test
  fun `when prison API call returns NOT_FOUND then NOT_FOUND is returned`() {
    // Given
    val prisonerId = "ABC123"
    val fromDate = LocalDate.now().minusDays(10)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(prisonerId, null, NOT_FOUND)
    visitAllocationApiMockServer.stubGetVisitOrderHistory(prisonerId, fromDate, emptyList())

    // When
    val responseSpec = callVisitOrderHistoryForPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, prisonerId, LocalDate.now().minusDays(10))
    responseSpec.expectStatus().isNotFound
    verify(visitAllocationApiClientSpy, times(1)).getVisitOrderHistoryDetails(prisonerId, fromDate)
    verify(manageUsersApiClientSpy, times(0)).getUserDetails(any())
  }

  @Test
  fun `when prison API call returns INTERNAL_SERVER_ERROR then INTERNAL_SERVER_ERROR is returned`() {
    // Given
    val prisonerId = "ABC123"
    val fromDate = LocalDate.now().minusDays(10)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(prisonerId, null, INTERNAL_SERVER_ERROR)
    visitAllocationApiMockServer.stubGetVisitOrderHistory(prisonerId, fromDate, emptyList())

    // When
    val responseSpec = callVisitOrderHistoryForPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, prisonerId, LocalDate.now().minusDays(10))
    responseSpec.expectStatus().is5xxServerError

    verify(visitAllocationApiClientSpy, times(1)).getVisitOrderHistoryDetails(prisonerId, fromDate)
    verify(manageUsersApiClientSpy, times(0)).getUserDetails(any())
  }

  @Test
  fun `when visit allocation call returns NOT_FOUND then NOT_FOUND is returned`() {
    // Given
    val prisonerId = "ABC123"
    val fromDate = LocalDate.now().minusDays(10)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(prisonerId, inmateDetails)
    visitAllocationApiMockServer.stubGetVisitOrderHistory(prisonerId, fromDate, null, NOT_FOUND)

    // When
    val responseSpec = callVisitOrderHistoryForPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, prisonerId, LocalDate.now().minusDays(10))
    responseSpec.expectStatus().isNotFound
    verify(visitAllocationApiClientSpy, times(1)).getVisitOrderHistoryDetails(prisonerId, fromDate)
    verify(manageUsersApiClientSpy, times(0)).getUserDetails(any())
  }

  @Test
  fun `when visit allocation call returns INTERNAL_SERVER_ERROR then INTERNAL_SERVER_ERROR is returned`() {
    // Given
    val prisonerId = "ABC123"
    val fromDate = LocalDate.now().minusDays(10)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(prisonerId, inmateDetails)
    visitAllocationApiMockServer.stubGetVisitOrderHistory(prisonerId, fromDate, null, INTERNAL_SERVER_ERROR)

    // When
    val responseSpec = callVisitOrderHistoryForPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, prisonerId, LocalDate.now().minusDays(10))
    responseSpec.expectStatus().is5xxServerError

    verify(visitAllocationApiClientSpy, times(1)).getVisitOrderHistoryDetails(prisonerId, fromDate)
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
    verify(visitAllocationApiClientSpy, times(0)).getVisitOrderHistoryDetails(any(), any())
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
    verify(visitAllocationApiClientSpy, times(0)).getVisitOrderHistoryDetails(any(), any())
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

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): VisitOrderHistoryDetailsDto = objectMapper.readValue(returnResult.returnResult().responseBody, VisitOrderHistoryDetailsDto::class.java)

  fun callVisitOrderHistoryForPrisoner(
    webTestClient: WebTestClient,
    authHttpHeaders: (HttpHeaders) -> Unit,
    prisonerId: String,
    fromDate: LocalDate,
  ): WebTestClient.ResponseSpec = webTestClient.get().uri("$VISIT_ORDER_HISTORY_FOR_PRISONER?fromDate=$fromDate".replace("{prisonerId}", prisonerId))
    .headers(authHttpHeaders)
    .exchange()
}
