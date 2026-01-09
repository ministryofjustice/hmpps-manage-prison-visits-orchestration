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
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.IncentivesApiClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.ManageUsersApiClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitAllocationApiClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller.VISIT_ORDER_HISTORY_FOR_PRISONER
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.incentives.IncentiveLevelDto
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

  @MockitoSpyBean
  lateinit var incentivesApiClientSpy: IncentivesApiClient

  @Test
  fun `when prisoner has multiple visit order history then all results are returned with balance change populated`() {
    // Given
    val fromDate = LocalDate.now().minusDays(10)
    val incentiveLevels = listOf(IncentiveLevelDto("STD", "Standard"), IncentiveLevelDto("ENH", "Enhanced"))
    val visitOrderHistory1 = VisitOrderHistoryDto(VisitOrderHistoryType.MIGRATION, LocalDateTime.now().minusDays(10), 10, null, 3, null, userName = "user1", attributes = emptyList())
    val visitOrderHistory2 = VisitOrderHistoryDto(VisitOrderHistoryType.PRISONER_BALANCE_RESET, LocalDateTime.now().minusDays(9), 0, null, 0, null, userName = "user2", attributes = emptyList())
    val visitOrderHistory3 = VisitOrderHistoryDto(VisitOrderHistoryType.ALLOCATION_USED_BY_VISIT, LocalDateTime.now().minusDays(8), -1, null, 0, null, userName = "SYSTEM", attributes = listOf(VisitOrderHistoryAttributesDto(VisitOrderHistoryAttributeType.VISIT_REFERENCE, "aa-bb-cc-dd")))

    // this entry needs to be ignored as balance does not change
    val visitOrderHistory4 = VisitOrderHistoryDto(VisitOrderHistoryType.SYNC_FROM_NOMIS, LocalDateTime.now().minusDays(7), -1, null, 0, null, userName = "SYSTEM", attributes = emptyList())
    val visitOrderHistory5 = VisitOrderHistoryDto(VisitOrderHistoryType.VO_AND_PVO_ALLOCATION, LocalDateTime.now().minusDays(6), 4, null, 2, null, userName = "user3", attributes = emptyList())
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(prisonerId, inmateDetails)
    visitAllocationApiMockServer.stubGetVisitOrderHistory(prisonerId, fromDate, listOf(visitOrderHistory1, visitOrderHistory2, visitOrderHistory3, visitOrderHistory4, visitOrderHistory5))
    manageUsersApiMockServer.stubGetUserDetails("user1", "John Smith")
    manageUsersApiMockServer.stubGetUserDetails("user2", "Sarah Jones")
    manageUsersApiMockServer.stubGetUserDetailsFailure("user3")
    incentivesApiMockServer.stubGetAllIncentiveLevels(incentiveLevels)

    // When
    val responseSpec = callVisitOrderHistoryForPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, prisonerId, LocalDate.now().minusDays(10))
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val visitOrderHistoryDetailsDto = getResults(returnResult)
    val visitOrderHistory = visitOrderHistoryDetailsDto.visitOrderHistory

    assertThat(visitOrderHistory.size).isEqualTo(4)
    // latest on top
    assertVisitOrderHistory(visitOrderHistory[0], visitOrderHistory5, 5, 2, "user3")
    assertVisitOrderHistoryAttributes(visitOrderHistory5.attributes, emptyList())
    // visitOrderHistory4 not returned as no balance changed
    assertVisitOrderHistory(visitOrderHistory[1], visitOrderHistory3, -1, 0, "SYSTEM")
    assertVisitOrderHistoryAttributes(visitOrderHistory3.attributes, listOf(Pair(VisitOrderHistoryAttributeType.VISIT_REFERENCE, "aa-bb-cc-dd")))

    assertVisitOrderHistory(visitOrderHistory[2], visitOrderHistory2, -10, -3, "Sarah Jones")
    assertVisitOrderHistoryAttributes(visitOrderHistory2.attributes, emptyList())
    assertVisitOrderHistory(visitOrderHistory[3], visitOrderHistory1, 0, 0, "John Smith")
    assertVisitOrderHistoryAttributes(visitOrderHistory1.attributes, emptyList())

    verify(visitAllocationApiClientSpy, times(1)).getVisitOrderHistoryDetails(prisonerId, fromDate)
    verify(manageUsersApiClientSpy, times(1)).getUserDetails("user1")
    verify(manageUsersApiClientSpy, times(1)).getUserDetails("user2")
    verify(manageUsersApiClientSpy, times(1)).getUserDetails("user3")
    verify(manageUsersApiClientSpy, times(3)).getUserDetails(any())
    verify(incentivesApiClientSpy, times(0)).getAllIncentiveLevels()
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
    verify(incentivesApiClientSpy, times(0)).getAllIncentiveLevels()
  }

  @Test
  fun `when prisoner has multiple visit order history but maxResults expected is less then only equalling maxResults are returned`() {
    // Given
    val fromDate = LocalDate.now().minusDays(10)
    val incentiveLevels = listOf(IncentiveLevelDto("STD", "Standard"), IncentiveLevelDto("ENH", "Enhanced"))
    val visitOrderHistory1 = VisitOrderHistoryDto(VisitOrderHistoryType.MIGRATION, LocalDateTime.now().minusDays(10), 10, null, 3, null, userName = "user1", attributes = emptyList())
    val visitOrderHistory2 = VisitOrderHistoryDto(VisitOrderHistoryType.PRISONER_BALANCE_RESET, LocalDateTime.now().minusDays(9), 0, null, 0, null, userName = "user2", attributes = emptyList())
    val visitOrderHistory3 = VisitOrderHistoryDto(VisitOrderHistoryType.ALLOCATION_USED_BY_VISIT, LocalDateTime.now().minusDays(8), -1, null, 0, null, userName = "SYSTEM", attributes = listOf(VisitOrderHistoryAttributesDto(VisitOrderHistoryAttributeType.VISIT_REFERENCE, "aa-bb-cc-dd")))

    // this entry needs to be ignored as balance does not change
    val visitOrderHistory4 = VisitOrderHistoryDto(VisitOrderHistoryType.SYNC_FROM_NOMIS, LocalDateTime.now().minusDays(7), -1, null, 0, null, userName = "SYSTEM", attributes = emptyList())
    val visitOrderHistory5 = VisitOrderHistoryDto(VisitOrderHistoryType.VO_AND_PVO_ALLOCATION, LocalDateTime.now().minusDays(6), 4, null, 2, null, userName = "user3", attributes = emptyList())
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(prisonerId, inmateDetails)
    visitAllocationApiMockServer.stubGetVisitOrderHistory(prisonerId, fromDate, listOf(visitOrderHistory1, visitOrderHistory2, visitOrderHistory3, visitOrderHistory4, visitOrderHistory5))
    manageUsersApiMockServer.stubGetUserDetails("user1", "John Smith")
    manageUsersApiMockServer.stubGetUserDetails("user2", "Sarah Jones")
    manageUsersApiMockServer.stubGetUserDetailsFailure("user3")
    incentivesApiMockServer.stubGetAllIncentiveLevels(incentiveLevels)

    // When
    // maxResults expected is 2
    val responseSpec = callVisitOrderHistoryForPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, prisonerId, LocalDate.now().minusDays(10), maxResults = 2)
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val visitOrderHistoryDetailsDto = getResults(returnResult)
    val visitOrderHistory = visitOrderHistoryDetailsDto.visitOrderHistory

    assertThat(visitOrderHistory.size).isEqualTo(2)
    // latest on top
    assertVisitOrderHistory(visitOrderHistory[0], visitOrderHistory5, 5, 2, "user3")
    assertVisitOrderHistoryAttributes(visitOrderHistory5.attributes, emptyList())
    assertVisitOrderHistory(visitOrderHistory[1], visitOrderHistory3, -1, 0, "SYSTEM")
    assertVisitOrderHistoryAttributes(visitOrderHistory3.attributes, listOf(Pair(VisitOrderHistoryAttributeType.VISIT_REFERENCE, "aa-bb-cc-dd")))

    verify(visitAllocationApiClientSpy, times(1)).getVisitOrderHistoryDetails(prisonerId, fromDate)
    verify(manageUsersApiClientSpy, times(1)).getUserDetails("user3")
    verify(manageUsersApiClientSpy, times(1)).getUserDetails(any())
    verify(incentivesApiClientSpy, times(0)).getAllIncentiveLevels()
  }

  @Test
  fun `when prisoner has multiple visit order history but maxResults expected is more then returned results all results are returned`() {
    // Given
    val fromDate = LocalDate.now().minusDays(10)
    val incentiveLevels = listOf(IncentiveLevelDto("STD", "Standard"), IncentiveLevelDto("ENH", "Enhanced"))
    val visitOrderHistory1 = VisitOrderHistoryDto(VisitOrderHistoryType.MIGRATION, LocalDateTime.now().minusDays(10), 10, null, 3, null, userName = "user1", attributes = emptyList())
    val visitOrderHistory2 = VisitOrderHistoryDto(VisitOrderHistoryType.PRISONER_BALANCE_RESET, LocalDateTime.now().minusDays(9), 0, null, 0, null, userName = "user2", attributes = emptyList())
    val visitOrderHistory3 = VisitOrderHistoryDto(VisitOrderHistoryType.ALLOCATION_USED_BY_VISIT, LocalDateTime.now().minusDays(8), -1, null, 0, null, userName = "SYSTEM", attributes = listOf(VisitOrderHistoryAttributesDto(VisitOrderHistoryAttributeType.VISIT_REFERENCE, "aa-bb-cc-dd")))

    // this entry needs to be ignored as balance does not change
    val visitOrderHistory4 = VisitOrderHistoryDto(VisitOrderHistoryType.SYNC_FROM_NOMIS, LocalDateTime.now().minusDays(7), -1, null, 0, null, userName = "SYSTEM", attributes = emptyList())
    val visitOrderHistory5 = VisitOrderHistoryDto(VisitOrderHistoryType.VO_AND_PVO_ALLOCATION, LocalDateTime.now().minusDays(6), 4, null, 2, null, userName = "user3", attributes = emptyList())
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(prisonerId, inmateDetails)
    visitAllocationApiMockServer.stubGetVisitOrderHistory(prisonerId, fromDate, listOf(visitOrderHistory1, visitOrderHistory2, visitOrderHistory3, visitOrderHistory4, visitOrderHistory5))
    manageUsersApiMockServer.stubGetUserDetails("user1", "John Smith")
    manageUsersApiMockServer.stubGetUserDetails("user2", "Sarah Jones")
    manageUsersApiMockServer.stubGetUserDetailsFailure("user3")
    incentivesApiMockServer.stubGetAllIncentiveLevels(incentiveLevels)

    // When
    // maxResults expected is 21 - greater than returned results
    val responseSpec = callVisitOrderHistoryForPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, prisonerId, LocalDate.now().minusDays(10), maxResults = 21)
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val visitOrderHistoryDetailsDto = getResults(returnResult)
    val visitOrderHistory = visitOrderHistoryDetailsDto.visitOrderHistory

    assertThat(visitOrderHistory.size).isEqualTo(4)
    // latest on top
    assertVisitOrderHistory(visitOrderHistory[0], visitOrderHistory5, 5, 2, "user3")
    assertVisitOrderHistoryAttributes(visitOrderHistory5.attributes, emptyList())
    // visitOrderHistory4 not returned as no balance changed
    assertVisitOrderHistory(visitOrderHistory[1], visitOrderHistory3, -1, 0, "SYSTEM")
    assertVisitOrderHistoryAttributes(visitOrderHistory3.attributes, listOf(Pair(VisitOrderHistoryAttributeType.VISIT_REFERENCE, "aa-bb-cc-dd")))

    assertVisitOrderHistory(visitOrderHistory[2], visitOrderHistory2, -10, -3, "Sarah Jones")
    assertVisitOrderHistoryAttributes(visitOrderHistory2.attributes, emptyList())
    assertVisitOrderHistory(visitOrderHistory[3], visitOrderHistory1, 0, 0, "John Smith")
    assertVisitOrderHistoryAttributes(visitOrderHistory1.attributes, emptyList())

    verify(visitAllocationApiClientSpy, times(1)).getVisitOrderHistoryDetails(prisonerId, fromDate)
    verify(manageUsersApiClientSpy, times(3)).getUserDetails(any())
    verify(incentivesApiClientSpy, times(0)).getAllIncentiveLevels()
  }

  @Test
  fun `when prisoner has multiple visit order history but maxResults passed is 0 then all results are returned`() {
    // Given
    val fromDate = LocalDate.now().minusDays(10)
    val incentiveLevels = listOf(IncentiveLevelDto("STD", "Standard"), IncentiveLevelDto("ENH", "Enhanced"))
    val visitOrderHistory1 = VisitOrderHistoryDto(VisitOrderHistoryType.MIGRATION, LocalDateTime.now().minusDays(10), 10, null, 3, null, userName = "user1", attributes = emptyList())
    val visitOrderHistory2 = VisitOrderHistoryDto(VisitOrderHistoryType.PRISONER_BALANCE_RESET, LocalDateTime.now().minusDays(9), 0, null, 0, null, userName = "user2", attributes = emptyList())
    val visitOrderHistory3 = VisitOrderHistoryDto(VisitOrderHistoryType.ALLOCATION_USED_BY_VISIT, LocalDateTime.now().minusDays(8), -1, null, 0, null, userName = "SYSTEM", attributes = listOf(VisitOrderHistoryAttributesDto(VisitOrderHistoryAttributeType.VISIT_REFERENCE, "aa-bb-cc-dd")))

    // this entry needs to be ignored as balance does not change
    val visitOrderHistory4 = VisitOrderHistoryDto(VisitOrderHistoryType.SYNC_FROM_NOMIS, LocalDateTime.now().minusDays(7), -1, null, 0, null, userName = "SYSTEM", attributes = emptyList())
    val visitOrderHistory5 = VisitOrderHistoryDto(VisitOrderHistoryType.VO_AND_PVO_ALLOCATION, LocalDateTime.now().minusDays(6), 4, null, 2, null, userName = "user3", attributes = emptyList())
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(prisonerId, inmateDetails)
    visitAllocationApiMockServer.stubGetVisitOrderHistory(prisonerId, fromDate, listOf(visitOrderHistory1, visitOrderHistory2, visitOrderHistory3, visitOrderHistory4, visitOrderHistory5))
    manageUsersApiMockServer.stubGetUserDetails("user1", "John Smith")
    manageUsersApiMockServer.stubGetUserDetails("user2", "Sarah Jones")
    manageUsersApiMockServer.stubGetUserDetailsFailure("user3")
    incentivesApiMockServer.stubGetAllIncentiveLevels(incentiveLevels)

    // When
    // maxResults expected is 0 - invalid hence all results returned
    val responseSpec = callVisitOrderHistoryForPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, prisonerId, LocalDate.now().minusDays(10), maxResults = 0)
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val visitOrderHistoryDetailsDto = getResults(returnResult)
    val visitOrderHistory = visitOrderHistoryDetailsDto.visitOrderHistory

    assertThat(visitOrderHistory.size).isEqualTo(4)
    // latest on top
    assertVisitOrderHistory(visitOrderHistory[0], visitOrderHistory5, 5, 2, "user3")
    assertVisitOrderHistoryAttributes(visitOrderHistory5.attributes, emptyList())
    // visitOrderHistory4 not returned as no balance changed
    assertVisitOrderHistory(visitOrderHistory[1], visitOrderHistory3, -1, 0, "SYSTEM")
    assertVisitOrderHistoryAttributes(visitOrderHistory3.attributes, listOf(Pair(VisitOrderHistoryAttributeType.VISIT_REFERENCE, "aa-bb-cc-dd")))

    assertVisitOrderHistory(visitOrderHistory[2], visitOrderHistory2, -10, -3, "Sarah Jones")
    assertVisitOrderHistoryAttributes(visitOrderHistory2.attributes, emptyList())
    assertVisitOrderHistory(visitOrderHistory[3], visitOrderHistory1, 0, 0, "John Smith")
    assertVisitOrderHistoryAttributes(visitOrderHistory1.attributes, emptyList())

    verify(visitAllocationApiClientSpy, times(1)).getVisitOrderHistoryDetails(prisonerId, fromDate)
    verify(manageUsersApiClientSpy, times(3)).getUserDetails(any())
    verify(incentivesApiClientSpy, times(0)).getAllIncentiveLevels()
  }

  @Test
  fun `when prisoner has multiple visit order history but maxResults passed is negative then all results are returned`() {
    // Given
    val fromDate = LocalDate.now().minusDays(10)
    val incentiveLevels = listOf(IncentiveLevelDto("STD", "Standard"), IncentiveLevelDto("ENH", "Enhanced"))
    val visitOrderHistory1 = VisitOrderHistoryDto(VisitOrderHistoryType.MIGRATION, LocalDateTime.now().minusDays(10), 10, null, 3, null, userName = "user1", attributes = emptyList())
    val visitOrderHistory2 = VisitOrderHistoryDto(VisitOrderHistoryType.PRISONER_BALANCE_RESET, LocalDateTime.now().minusDays(9), 0, null, 0, null, userName = "user2", attributes = emptyList())
    val visitOrderHistory3 = VisitOrderHistoryDto(VisitOrderHistoryType.ALLOCATION_USED_BY_VISIT, LocalDateTime.now().minusDays(8), -1, null, 0, null, userName = "SYSTEM", attributes = listOf(VisitOrderHistoryAttributesDto(VisitOrderHistoryAttributeType.VISIT_REFERENCE, "aa-bb-cc-dd")))

    // this entry needs to be ignored as balance does not change
    val visitOrderHistory4 = VisitOrderHistoryDto(VisitOrderHistoryType.SYNC_FROM_NOMIS, LocalDateTime.now().minusDays(7), -1, null, 0, null, userName = "SYSTEM", attributes = emptyList())
    val visitOrderHistory5 = VisitOrderHistoryDto(VisitOrderHistoryType.VO_AND_PVO_ALLOCATION, LocalDateTime.now().minusDays(6), 4, null, 2, null, userName = "user3", attributes = emptyList())
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(prisonerId, inmateDetails)
    visitAllocationApiMockServer.stubGetVisitOrderHistory(prisonerId, fromDate, listOf(visitOrderHistory1, visitOrderHistory2, visitOrderHistory3, visitOrderHistory4, visitOrderHistory5))
    manageUsersApiMockServer.stubGetUserDetails("user1", "John Smith")
    manageUsersApiMockServer.stubGetUserDetails("user2", "Sarah Jones")
    manageUsersApiMockServer.stubGetUserDetailsFailure("user3")
    incentivesApiMockServer.stubGetAllIncentiveLevels(incentiveLevels)

    // When
    // maxResults expected is -5 - invalid hence all results returned
    val responseSpec = callVisitOrderHistoryForPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, prisonerId, LocalDate.now().minusDays(10), maxResults = -5)
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val visitOrderHistoryDetailsDto = getResults(returnResult)
    val visitOrderHistory = visitOrderHistoryDetailsDto.visitOrderHistory

    assertThat(visitOrderHistory.size).isEqualTo(4)
    // latest on top
    assertVisitOrderHistory(visitOrderHistory[0], visitOrderHistory5, 5, 2, "user3")
    assertVisitOrderHistoryAttributes(visitOrderHistory5.attributes, emptyList())
    // visitOrderHistory4 not returned as no balance changed
    assertVisitOrderHistory(visitOrderHistory[1], visitOrderHistory3, -1, 0, "SYSTEM")
    assertVisitOrderHistoryAttributes(visitOrderHistory3.attributes, listOf(Pair(VisitOrderHistoryAttributeType.VISIT_REFERENCE, "aa-bb-cc-dd")))

    assertVisitOrderHistory(visitOrderHistory[2], visitOrderHistory2, -10, -3, "Sarah Jones")
    assertVisitOrderHistoryAttributes(visitOrderHistory2.attributes, emptyList())
    assertVisitOrderHistory(visitOrderHistory[3], visitOrderHistory1, 0, 0, "John Smith")
    assertVisitOrderHistoryAttributes(visitOrderHistory1.attributes, emptyList())

    verify(visitAllocationApiClientSpy, times(1)).getVisitOrderHistoryDetails(prisonerId, fromDate)
    verify(manageUsersApiClientSpy, times(3)).getUserDetails(any())
    verify(incentivesApiClientSpy, times(0)).getAllIncentiveLevels()
  }

  @Test
  fun `when incentive levels are part of visit order history attributes then the value not the name is returned`() {
    // Given
    val fromDate = LocalDate.now().minusDays(10)
    val incentiveLevels = listOf(IncentiveLevelDto("STD", "Standard"), IncentiveLevelDto("ENH", "Enhanced"))
    val visitOrderHistory1Attributes = listOf(
      VisitOrderHistoryAttributesDto(VisitOrderHistoryAttributeType.INCENTIVE_LEVEL, "STD"),
    )
    val visitOrderHistory1 = VisitOrderHistoryDto(VisitOrderHistoryType.VO_AND_PVO_ALLOCATION, LocalDateTime.now().minusDays(3), 4, null, 2, null, userName = "user1", attributes = visitOrderHistory1Attributes)

    val visitOrderHistory2Attributes = listOf(
      VisitOrderHistoryAttributesDto(VisitOrderHistoryAttributeType.INCENTIVE_LEVEL, "ENH"),
    )
    val visitOrderHistory2 = VisitOrderHistoryDto(VisitOrderHistoryType.VO_AND_PVO_ALLOCATION, LocalDateTime.now().minusDays(2), 6, null, 3, null, userName = "user2", attributes = visitOrderHistory2Attributes)

    val visitOrderHistory3Attributes = listOf(
      VisitOrderHistoryAttributesDto(VisitOrderHistoryAttributeType.INCENTIVE_LEVEL, "BAS"),
    )
    val visitOrderHistory3 = VisitOrderHistoryDto(VisitOrderHistoryType.VO_AND_PVO_ALLOCATION, LocalDateTime.now().minusDays(1), 8, null, 4, null, userName = "user3", attributes = visitOrderHistory3Attributes)

    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(prisonerId, inmateDetails)
    visitAllocationApiMockServer.stubGetVisitOrderHistory(prisonerId, fromDate, listOf(visitOrderHistory1, visitOrderHistory2, visitOrderHistory3))
    manageUsersApiMockServer.stubGetUserDetails("user1", "John Smith")
    manageUsersApiMockServer.stubGetUserDetails("user2", "Sarah Jones")
    manageUsersApiMockServer.stubGetUserDetailsFailure("user3")
    incentivesApiMockServer.stubGetAllIncentiveLevels(incentiveLevels)

    // When
    val responseSpec = callVisitOrderHistoryForPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, prisonerId, LocalDate.now().minusDays(10))
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val visitOrderHistoryDetailsDto = getResults(returnResult)
    val visitOrderHistory = visitOrderHistoryDetailsDto.visitOrderHistory

    assertThat(visitOrderHistory.size).isEqualTo(3)
    assertVisitOrderHistory(visitOrderHistory[0], visitOrderHistory3, 2, 1, "user3")
    assertVisitOrderHistoryAttributes(visitOrderHistory[0].attributes, listOf(Pair(VisitOrderHistoryAttributeType.INCENTIVE_LEVEL, "BAS")))

    assertVisitOrderHistory(visitOrderHistory[1], visitOrderHistory2, 2, 1, "Sarah Jones")
    assertVisitOrderHistoryAttributes(visitOrderHistory[1].attributes, listOf(Pair(VisitOrderHistoryAttributeType.INCENTIVE_LEVEL, "Enhanced")))

    assertVisitOrderHistory(visitOrderHistory[2], visitOrderHistory1, 0, 0, "John Smith")
    assertVisitOrderHistoryAttributes(visitOrderHistory[2].attributes, listOf(Pair(VisitOrderHistoryAttributeType.INCENTIVE_LEVEL, "Standard")))

    verify(visitAllocationApiClientSpy, times(1)).getVisitOrderHistoryDetails(prisonerId, fromDate)
    verify(manageUsersApiClientSpy, times(1)).getUserDetails("user1")
    verify(manageUsersApiClientSpy, times(1)).getUserDetails("user2")
    verify(manageUsersApiClientSpy, times(1)).getUserDetails("user3")
    verify(manageUsersApiClientSpy, times(3)).getUserDetails(any())
    verify(incentivesApiClientSpy, times(1)).getAllIncentiveLevels()
  }

  @Test
  fun `when incentive levels call returns a NOT_FOUND error then incentive levels names are not replaced with values`() {
    // Given
    val fromDate = LocalDate.now().minusDays(10)
    val incentiveLevels = null
    val visitOrderHistory1Attributes = listOf(
      VisitOrderHistoryAttributesDto(VisitOrderHistoryAttributeType.INCENTIVE_LEVEL, "STD"),
    )
    val visitOrderHistory1 = VisitOrderHistoryDto(VisitOrderHistoryType.VO_AND_PVO_ALLOCATION, LocalDateTime.now().minusDays(3), 4, null, 2, null, userName = "user1", attributes = visitOrderHistory1Attributes)

    val visitOrderHistory2Attributes = listOf(
      VisitOrderHistoryAttributesDto(VisitOrderHistoryAttributeType.INCENTIVE_LEVEL, "ENH"),
    )
    val visitOrderHistory2 = VisitOrderHistoryDto(VisitOrderHistoryType.VO_AND_PVO_ALLOCATION, LocalDateTime.now().minusDays(2), 6, null, 3, null, userName = "user2", attributes = visitOrderHistory2Attributes)

    val visitOrderHistory3Attributes = listOf(
      VisitOrderHistoryAttributesDto(VisitOrderHistoryAttributeType.INCENTIVE_LEVEL, "BAS"),
    )
    val visitOrderHistory3 = VisitOrderHistoryDto(VisitOrderHistoryType.VO_AND_PVO_ALLOCATION, LocalDateTime.now().minusDays(1), 8, null, 4, null, userName = "user3", attributes = visitOrderHistory3Attributes)

    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(prisonerId, inmateDetails)
    visitAllocationApiMockServer.stubGetVisitOrderHistory(prisonerId, fromDate, listOf(visitOrderHistory1, visitOrderHistory2, visitOrderHistory3))
    manageUsersApiMockServer.stubGetUserDetails("user1", "John Smith")
    manageUsersApiMockServer.stubGetUserDetails("user2", "Sarah Jones")
    manageUsersApiMockServer.stubGetUserDetailsFailure("user3")
    incentivesApiMockServer.stubGetAllIncentiveLevels(incentiveLevels, NOT_FOUND)

    // When
    val responseSpec = callVisitOrderHistoryForPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, prisonerId, LocalDate.now().minusDays(10))
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val visitOrderHistoryDetailsDto = getResults(returnResult)
    val visitOrderHistory = visitOrderHistoryDetailsDto.visitOrderHistory

    assertThat(visitOrderHistory.size).isEqualTo(3)
    assertVisitOrderHistory(visitOrderHistory[0], visitOrderHistory3, 2, 1, "user3")
    assertVisitOrderHistoryAttributes(visitOrderHistory[0].attributes, listOf(Pair(VisitOrderHistoryAttributeType.INCENTIVE_LEVEL, "BAS")))

    assertVisitOrderHistory(visitOrderHistory[1], visitOrderHistory2, 2, 1, "Sarah Jones")
    assertVisitOrderHistoryAttributes(visitOrderHistory[1].attributes, listOf(Pair(VisitOrderHistoryAttributeType.INCENTIVE_LEVEL, "ENH")))

    assertVisitOrderHistory(visitOrderHistory[2], visitOrderHistory1, 0, 0, "John Smith")
    assertVisitOrderHistoryAttributes(visitOrderHistory[2].attributes, listOf(Pair(VisitOrderHistoryAttributeType.INCENTIVE_LEVEL, "STD")))

    verify(visitAllocationApiClientSpy, times(1)).getVisitOrderHistoryDetails(prisonerId, fromDate)
    verify(manageUsersApiClientSpy, times(1)).getUserDetails("user1")
    verify(manageUsersApiClientSpy, times(1)).getUserDetails("user2")
    verify(manageUsersApiClientSpy, times(1)).getUserDetails("user3")
    verify(manageUsersApiClientSpy, times(3)).getUserDetails(any())
    verify(incentivesApiClientSpy, times(1)).getAllIncentiveLevels()
  }

  @Test
  fun `when incentive levels call returns an INTERNAL_SERVER_ERROR then incentive levels names are not replaced with values`() {
    // Given
    val fromDate = LocalDate.now().minusDays(10)
    val incentiveLevels = null
    val visitOrderHistory1Attributes = listOf(
      VisitOrderHistoryAttributesDto(VisitOrderHistoryAttributeType.INCENTIVE_LEVEL, "STD"),
    )
    val visitOrderHistory1 = VisitOrderHistoryDto(VisitOrderHistoryType.VO_AND_PVO_ALLOCATION, LocalDateTime.now().minusDays(3), 4, null, 2, null, userName = "user1", attributes = visitOrderHistory1Attributes)

    val visitOrderHistory2Attributes = listOf(
      VisitOrderHistoryAttributesDto(VisitOrderHistoryAttributeType.INCENTIVE_LEVEL, "ENH"),
    )
    val visitOrderHistory2 = VisitOrderHistoryDto(VisitOrderHistoryType.VO_AND_PVO_ALLOCATION, LocalDateTime.now().minusDays(2), 6, null, 3, null, userName = "user2", attributes = visitOrderHistory2Attributes)

    val visitOrderHistory3Attributes = listOf(
      VisitOrderHistoryAttributesDto(VisitOrderHistoryAttributeType.INCENTIVE_LEVEL, "BAS"),
    )
    val visitOrderHistory3 = VisitOrderHistoryDto(VisitOrderHistoryType.VO_AND_PVO_ALLOCATION, LocalDateTime.now().minusDays(1), 8, null, 4, null, userName = "user3", attributes = visitOrderHistory3Attributes)

    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(prisonerId, inmateDetails)
    visitAllocationApiMockServer.stubGetVisitOrderHistory(prisonerId, fromDate, listOf(visitOrderHistory1, visitOrderHistory2, visitOrderHistory3))
    manageUsersApiMockServer.stubGetUserDetails("user1", "John Smith")
    manageUsersApiMockServer.stubGetUserDetails("user2", "Sarah Jones")
    manageUsersApiMockServer.stubGetUserDetailsFailure("user3")
    incentivesApiMockServer.stubGetAllIncentiveLevels(incentiveLevels, NOT_FOUND)

    // When
    val responseSpec = callVisitOrderHistoryForPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, prisonerId, LocalDate.now().minusDays(10))
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val visitOrderHistoryDetailsDto = getResults(returnResult)
    val visitOrderHistory = visitOrderHistoryDetailsDto.visitOrderHistory

    assertThat(visitOrderHistory.size).isEqualTo(3)
    assertVisitOrderHistory(visitOrderHistory[0], visitOrderHistory3, 2, 1, "user3")
    assertVisitOrderHistoryAttributes(visitOrderHistory[0].attributes, listOf(Pair(VisitOrderHistoryAttributeType.INCENTIVE_LEVEL, "BAS")))

    assertVisitOrderHistory(visitOrderHistory[1], visitOrderHistory2, 2, 1, "Sarah Jones")
    assertVisitOrderHistoryAttributes(visitOrderHistory[1].attributes, listOf(Pair(VisitOrderHistoryAttributeType.INCENTIVE_LEVEL, "ENH")))

    assertVisitOrderHistory(visitOrderHistory[2], visitOrderHistory1, 0, 0, "John Smith")
    assertVisitOrderHistoryAttributes(visitOrderHistory[2].attributes, listOf(Pair(VisitOrderHistoryAttributeType.INCENTIVE_LEVEL, "STD")))

    verify(visitAllocationApiClientSpy, times(1)).getVisitOrderHistoryDetails(prisonerId, fromDate)
    verify(manageUsersApiClientSpy, times(1)).getUserDetails("user1")
    verify(manageUsersApiClientSpy, times(1)).getUserDetails("user2")
    verify(manageUsersApiClientSpy, times(1)).getUserDetails("user3")
    verify(manageUsersApiClientSpy, times(3)).getUserDetails(any())
    verify(incentivesApiClientSpy, times(1)).getAllIncentiveLevels()
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
    Assertions.assertThat(visitOrderHistoryDto.visitOrderHistoryType).isEqualTo(expectedVisitOrderHistoryDto.visitOrderHistoryType)
    Assertions.assertThat(visitOrderHistoryDto.voBalance).isEqualTo(expectedVisitOrderHistoryDto.voBalance)
    Assertions.assertThat(visitOrderHistoryDto.pvoBalance).isEqualTo(expectedVisitOrderHistoryDto.pvoBalance)
    Assertions.assertThat(visitOrderHistoryDto.userName).isEqualTo(expectedUserName)
    Assertions.assertThat(visitOrderHistoryDto.createdTimeStamp).isEqualTo(expectedVisitOrderHistoryDto.createdTimeStamp)
    Assertions.assertThat(visitOrderHistoryDto.comment).isEqualTo(expectedVisitOrderHistoryDto.comment)
    Assertions.assertThat(visitOrderHistoryDto.voBalanceChange).isEqualTo(expectedVoBalanceChange)
    Assertions.assertThat(visitOrderHistoryDto.pvoBalanceChange).isEqualTo(expectedPVoBalanceChange)
  }

  private fun assertVisitOrderHistoryAttributes(visitOrderHistoryAttributesList: List<VisitOrderHistoryAttributesDto>, expectedNameValuePair: List<Pair<VisitOrderHistoryAttributeType, String>>) {
    Assertions.assertThat(visitOrderHistoryAttributesList.size).isEqualTo(expectedNameValuePair.size)

    visitOrderHistoryAttributesList.forEachIndexed { i, visitOrderHistoryAttributesDto ->
      Assertions.assertThat(visitOrderHistoryAttributesDto.attributeType).isEqualTo((expectedNameValuePair[i]).first)
      Assertions.assertThat(visitOrderHistoryAttributesDto.attributeValue).isEqualTo((expectedNameValuePair[i]).second)
    }
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): VisitOrderHistoryDetailsDto = objectMapper.readValue(returnResult.returnResult().responseBody, VisitOrderHistoryDetailsDto::class.java)

  fun callVisitOrderHistoryForPrisoner(
    webTestClient: WebTestClient,
    authHttpHeaders: (HttpHeaders) -> Unit,
    prisonerId: String,
    fromDate: LocalDate,
    maxResults: Int? = null,
  ): WebTestClient.ResponseSpec {
    var uri = "$VISIT_ORDER_HISTORY_FOR_PRISONER?fromDate=$fromDate".replace("{prisonerId}", prisonerId)
    if (maxResults != null) {
      uri = uri.plus("&maxResults=$maxResults")
    }

    return webTestClient.get().uri(uri)
      .headers(authHttpHeaders)
      .exchange()
  }
}
