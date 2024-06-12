package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.booker

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonVisitBookerRegistryClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller.PUBLIC_BOOKER_GET_PRISONERS_CONTROLLER_PATH
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.BookerPrisonerInfoDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.PermittedPrisonerForBookerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.PermittedVisitorsForPermittedPrisonerBookerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.VisitBalancesDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.CurrentIncentive
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.IncentiveLevel
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.PrisonUserClientDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("Get permitted prisoners for booker")
class GetPermittedPrisonersForBookerTest : IntegrationTestBase() {
  companion object {
    private const val PRISON_CODE = "MDI"
    private const val BOOKER_REFERENCE = "booker-1"
    private const val PRISONER1_ID = "AA112233B"
    private const val PRISONER2_ID = "BB112233B"
  }

  @SpyBean
  lateinit var prisonVisitBookerRegistryClientSpy: PrisonVisitBookerRegistryClient

  @SpyBean
  lateinit var prisonerSearchClientSpy: PrisonerSearchClient

  @SpyBean
  lateinit var prisonApiClientSpy: PrisonApiClient

  private final val prisonDto = createPrison(PRISON_CODE)
  private final val inactivePrison = createPrison("INA", active = false)
  private final val inactiveForPublicPrison = createPrison("IFP", active = true, clients = listOf(PrisonUserClientDto(UserType.PUBLIC, false)))
  private final val onlyActiveForStaffPrison = createPrison("OFS", active = true, clients = listOf(PrisonUserClientDto(UserType.STAFF, true)))

  private final val currentIncentive = createCurrentIncentive()

  private final val prisoner1Dto = createPrisoner(
    prisonerId = PRISONER1_ID,
    firstName = "FirstName",
    lastName = "LastName",
    dateOfBirth = LocalDate.of(2000, 1, 31),
    currentIncentive = currentIncentive,
  )

  private final val prisoner2Dto = createPrisoner(
    prisonerId = PRISONER2_ID,
    firstName = "First",
    lastName = "Last",
    dateOfBirth = LocalDate.of(2001, 12, 1),
    currentIncentive = currentIncentive,
  )

  // prisoner with prison ID as null
  private final val prisoner3Dto = createPrisoner(
    prisonerId = "CC112233C",
    firstName = "First",
    lastName = "Last",
    dateOfBirth = LocalDate.of(2001, 12, 1),
    currentIncentive = currentIncentive,
    prisonId = null,
  )

  // prisoner in inactive prison
  private final val prisoner4Dto = createPrisoner(
    prisonerId = "CC112233C",
    firstName = "First",
    lastName = "Last",
    dateOfBirth = LocalDate.of(2001, 12, 1),
    currentIncentive = currentIncentive,
    prisonId = inactivePrison.code,
  )

  // prisoner is in prison that is inactive for public
  private final val prisoner5Dto = createPrisoner(
    prisonerId = "CC112233C",
    firstName = "First",
    lastName = "Last",
    dateOfBirth = LocalDate.of(2001, 12, 1),
    currentIncentive = currentIncentive,
    prisonId = inactiveForPublicPrison.code,
  )

  private val visitBalance1 = VisitBalancesDto(4, 3, null, LocalDate.now().plusDays(2))
  private val visitBalance2 = VisitBalancesDto(2, 3, LocalDate.now().plusDays(7), null)

  @Test
  fun `when booker has valid prisoners then all allowed prisoners are returned`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerId, prisoner1Dto)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner2Dto.prisonerId, prisoner2Dto)
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      BOOKER_REFERENCE,
      listOf(
        PermittedPrisonerForBookerDto(prisoner1Dto.prisonerId, true, listOf(PermittedVisitorsForPermittedPrisonerBookerDto(1L, true))),
        PermittedPrisonerForBookerDto(prisoner2Dto.prisonerId, true, listOf(PermittedVisitorsForPermittedPrisonerBookerDto(1L, true))),
      ),
    )
    prisonApiMockServer.stubGetVisitBalances(prisoner1Dto.prisonerId, visitBalance1)
    prisonApiMockServer.stubGetVisitBalances(prisoner2Dto.prisonerId, visitBalance2)

    // When
    val responseSpec = callGetPrisonersByBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(2)
    assertPrisonerBasicDetails(prisonerDetailsList[0], prisoner1Dto, 7, LocalDate.now().plusDays(2))
    assertPrisonerBasicDetails(prisonerDetailsList[1], prisoner2Dto, 5, LocalDate.now().plusDays(7))

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForPermittedPrisonerAndBooker(BOOKER_REFERENCE)
    verify(prisonerSearchClientSpy, times(2)).getPrisonerByIdAsMono(any())
    verify(prisonApiClientSpy, times(1)).getVisitBalancesAsMono(prisoner1Dto.prisonerId)
    verify(prisonApiClientSpy, times(1)).getVisitBalancesAsMono(prisoner2Dto.prisonerId)
  }

  @Test
  fun `when booker has no valid prisoners then an empty list is returned`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerId, prisoner1Dto)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner2Dto.prisonerId, prisoner2Dto)
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      BOOKER_REFERENCE,
      listOf(),
    )
    prisonApiMockServer.stubGetVisitBalances(prisoner1Dto.prisonerId, visitBalance1)
    prisonApiMockServer.stubGetVisitBalances(prisoner2Dto.prisonerId, visitBalance2)

    // When
    val responseSpec = callGetPrisonersByBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(0)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForPermittedPrisonerAndBooker(BOOKER_REFERENCE)
    verify(prisonerSearchClientSpy, times(0)).getPrisonerByIdAsMono(any())
    verify(prisonApiClientSpy, times(0)).getVisitBalancesAsMono(prisoner1Dto.prisonerId)
    verify(prisonApiClientSpy, times(0)).getVisitBalancesAsMono(prisoner2Dto.prisonerId)
  }

  @Test
  fun `when booker has valid prisoners but 1 of them cannot be retrieved from prisoner search then that prisoner is not returned`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerId, prisoner1Dto)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner2Dto.prisonerId, null)
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      BOOKER_REFERENCE,
      listOf(
        PermittedPrisonerForBookerDto(prisoner1Dto.prisonerId, true, listOf()),
        PermittedPrisonerForBookerDto(prisoner2Dto.prisonerId, true, listOf()),
      ),
    )
    prisonApiMockServer.stubGetVisitBalances(prisoner1Dto.prisonerId, visitBalance1)
    prisonApiMockServer.stubGetVisitBalances(prisoner2Dto.prisonerId, visitBalance2)

    // When
    val responseSpec = callGetPrisonersByBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(1)
    assertPrisonerBasicDetails(prisonerDetailsList[0], prisoner1Dto, 7, LocalDate.now().plusDays(2))

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForPermittedPrisonerAndBooker(BOOKER_REFERENCE)
    verify(prisonerSearchClientSpy, times(2)).getPrisonerByIdAsMono(any())
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner1Dto.prisonerId)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner2Dto.prisonerId)
    verify(prisonApiClientSpy, times(1)).getVisitBalancesAsMono(prisoner1Dto.prisonerId)
    verify(prisonApiClientSpy, times(0)).getVisitBalancesAsMono(prisoner2Dto.prisonerId)
  }

  @Test
  fun `when booker has valid prisoners but 1 of them has prison code as null then that prisoner is not returned`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerId, prisoner1Dto)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner3Dto.prisonerId, prisoner3Dto)
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      BOOKER_REFERENCE,
      listOf(
        PermittedPrisonerForBookerDto(prisoner1Dto.prisonerId, true, listOf()),
        PermittedPrisonerForBookerDto(prisoner3Dto.prisonerId, true, listOf()),
      ),
    )
    prisonApiMockServer.stubGetVisitBalances(prisoner1Dto.prisonerId, visitBalance1)

    // When
    val responseSpec = callGetPrisonersByBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(1)
    assertPrisonerBasicDetails(prisonerDetailsList[0], prisoner1Dto, 7, LocalDate.now().plusDays(2))

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForPermittedPrisonerAndBooker(BOOKER_REFERENCE)
    verify(prisonerSearchClientSpy, times(2)).getPrisonerByIdAsMono(any())
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner1Dto.prisonerId)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner3Dto.prisonerId)
    verify(prisonApiClientSpy, times(1)).getVisitBalancesAsMono(prisoner1Dto.prisonerId)
    verify(prisonApiClientSpy, times(0)).getVisitBalancesAsMono(prisoner3Dto.prisonerId)
  }

  @Test
  fun `when booker has valid prisoners but prison is not on VSIP then that prisoner is not returned`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerId, prisoner1Dto)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner2Dto.prisonerId, prisoner2Dto)
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, null)
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      BOOKER_REFERENCE,
      listOf(
        PermittedPrisonerForBookerDto(prisoner1Dto.prisonerId, true, listOf()),
        PermittedPrisonerForBookerDto(prisoner2Dto.prisonerId, true, listOf()),
      ),
    )

    // When
    val responseSpec = callGetPrisonersByBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(0)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForPermittedPrisonerAndBooker(BOOKER_REFERENCE)
    verify(prisonerSearchClientSpy, times(2)).getPrisonerByIdAsMono(any())
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner1Dto.prisonerId)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner2Dto.prisonerId)
    verify(prisonApiClientSpy, times(1)).getVisitBalancesAsMono(prisoner1Dto.prisonerId)
    verify(prisonApiClientSpy, times(1)).getVisitBalancesAsMono(prisoner2Dto.prisonerId)
  }

  @Test
  fun `when booker has valid prisoners but prison is inactive then that prisoner is not returned`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerId, prisoner1Dto)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner4Dto.prisonerId, prisoner4Dto)
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)
    visitSchedulerMockServer.stubGetPrison(inactivePrison.code, inactivePrison)
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      BOOKER_REFERENCE,
      listOf(
        PermittedPrisonerForBookerDto(prisoner1Dto.prisonerId, true, listOf()),
        PermittedPrisonerForBookerDto(prisoner4Dto.prisonerId, true, listOf()),
      ),
    )
    prisonApiMockServer.stubGetVisitBalances(prisoner1Dto.prisonerId, visitBalance1)
    prisonApiMockServer.stubGetVisitBalances(prisoner2Dto.prisonerId, visitBalance2)

    // When
    val responseSpec = callGetPrisonersByBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(1)
    assertPrisonerBasicDetails(prisonerDetailsList[0], prisoner1Dto, 7, LocalDate.now().plusDays(2))

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForPermittedPrisonerAndBooker(BOOKER_REFERENCE)
    verify(prisonerSearchClientSpy, times(2)).getPrisonerByIdAsMono(any())
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner1Dto.prisonerId)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner4Dto.prisonerId)
    verify(prisonApiClientSpy, times(1)).getVisitBalancesAsMono(prisoner1Dto.prisonerId)
    verify(prisonApiClientSpy, times(1)).getVisitBalancesAsMono(prisoner4Dto.prisonerId)
  }

  @Test
  fun `when booker has valid prisoners but prison is inactive for public users then that prisoner is not returned`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerId, prisoner1Dto)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner5Dto.prisonerId, prisoner5Dto)
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)
    visitSchedulerMockServer.stubGetPrison(inactiveForPublicPrison.code, inactiveForPublicPrison)
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      BOOKER_REFERENCE,
      listOf(
        PermittedPrisonerForBookerDto(prisoner1Dto.prisonerId, true, listOf()),
        PermittedPrisonerForBookerDto(prisoner5Dto.prisonerId, true, listOf()),
      ),
    )
    prisonApiMockServer.stubGetVisitBalances(prisoner1Dto.prisonerId, visitBalance1)
    prisonApiMockServer.stubGetVisitBalances(prisoner2Dto.prisonerId, visitBalance2)

    // When
    val responseSpec = callGetPrisonersByBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(1)
    assertPrisonerBasicDetails(prisonerDetailsList[0], prisoner1Dto, 7, LocalDate.now().plusDays(2))

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForPermittedPrisonerAndBooker(BOOKER_REFERENCE)
    verify(prisonerSearchClientSpy, times(2)).getPrisonerByIdAsMono(any())
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner1Dto.prisonerId)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner5Dto.prisonerId)
    verify(prisonApiClientSpy, times(1)).getVisitBalancesAsMono(prisoner1Dto.prisonerId)
    verify(prisonApiClientSpy, times(1)).getVisitBalancesAsMono(prisoner5Dto.prisonerId)
  }

  @Test
  fun `when booker has valid prisoners but prison is only active for staff users then that prisoner is not returned`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerId, prisoner1Dto)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner5Dto.prisonerId, prisoner5Dto)
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)
    visitSchedulerMockServer.stubGetPrison(inactiveForPublicPrison.code, onlyActiveForStaffPrison)
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      BOOKER_REFERENCE,
      listOf(
        PermittedPrisonerForBookerDto(prisoner1Dto.prisonerId, true, listOf()),
        PermittedPrisonerForBookerDto(prisoner5Dto.prisonerId, true, listOf()),
      ),
    )
    prisonApiMockServer.stubGetVisitBalances(prisoner1Dto.prisonerId, visitBalance1)
    prisonApiMockServer.stubGetVisitBalances(prisoner2Dto.prisonerId, visitBalance2)

    // When
    val responseSpec = callGetPrisonersByBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(1)
    assertPrisonerBasicDetails(prisonerDetailsList[0], prisoner1Dto, 7, LocalDate.now().plusDays(2))

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForPermittedPrisonerAndBooker(BOOKER_REFERENCE)
    verify(prisonerSearchClientSpy, times(2)).getPrisonerByIdAsMono(any())
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner1Dto.prisonerId)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner5Dto.prisonerId)
    verify(prisonApiClientSpy, times(1)).getVisitBalancesAsMono(prisoner1Dto.prisonerId)
    verify(prisonApiClientSpy, times(1)).getVisitBalancesAsMono(prisoner5Dto.prisonerId)
  }

  @Test
  fun `when booker has valid prisoners but none of them can be retrieved from prisoner search then an empty list is returned`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      BOOKER_REFERENCE,
      listOf(
        PermittedPrisonerForBookerDto(prisoner1Dto.prisonerId, true, listOf()),
        PermittedPrisonerForBookerDto(prisoner2Dto.prisonerId, true, listOf()),
      ),
    )
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerId, null)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner2Dto.prisonerId, null)

    // When
    val responseSpec = callGetPrisonersByBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(0)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForPermittedPrisonerAndBooker(BOOKER_REFERENCE)
    verify(prisonerSearchClientSpy, times(2)).getPrisonerByIdAsMono(any())
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner1Dto.prisonerId)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner2Dto.prisonerId)
    verify(prisonApiClientSpy, times(0)).getVisitBalancesAsMono(prisoner1Dto.prisonerId)
    verify(prisonApiClientSpy, times(0)).getVisitBalancesAsMono(prisoner5Dto.prisonerId)
  }

  @Test
  fun `when NOT_FOUND is returned from booker registry then NOT_FOUND status is sent back`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      BOOKER_REFERENCE,
      null,
      HttpStatus.NOT_FOUND,
    )
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerId, null)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner2Dto.prisonerId, null)

    // When
    val responseSpec = callGetPrisonersByBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE)

    // Then
    responseSpec.expectStatus().isNotFound
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForPermittedPrisonerAndBooker(BOOKER_REFERENCE)
    verify(prisonerSearchClientSpy, times(0)).getPrisonerByIdAsMono(any())
    verify(prisonApiClientSpy, times(0)).getVisitBalancesAsMono(prisoner1Dto.prisonerId)
    verify(prisonApiClientSpy, times(0)).getVisitBalancesAsMono(prisoner2Dto.prisonerId)
  }

  @Test
  fun `when INTERNAL_SERVER_ERROR  is returned from booker registry then INTERNAL_SERVER_ERROR status is sent back`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      BOOKER_REFERENCE,
      null,
      HttpStatus.INTERNAL_SERVER_ERROR,
    )
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerId, null)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner2Dto.prisonerId, null)

    // When
    val responseSpec = callGetPrisonersByBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE)

    // Then
    responseSpec.expectStatus().is5xxServerError
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForPermittedPrisonerAndBooker(BOOKER_REFERENCE)
    verify(prisonerSearchClientSpy, times(0)).getPrisonerByIdAsMono(any())
    verify(prisonApiClientSpy, times(0)).getVisitBalancesAsMono(prisoner1Dto.prisonerId)
    verify(prisonApiClientSpy, times(0)).getVisitBalancesAsMono(prisoner2Dto.prisonerId)
  }

  @Test
  fun `when NOT_FOUND  is returned from prisoner search then empty list is returned`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      BOOKER_REFERENCE,
      listOf(
        PermittedPrisonerForBookerDto(prisoner1Dto.prisonerId, true, listOf()),
        PermittedPrisonerForBookerDto(prisoner2Dto.prisonerId, true, listOf()),
      ),
    )
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerId, null, HttpStatus.NOT_FOUND)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner2Dto.prisonerId, null, HttpStatus.NOT_FOUND)

    // When
    val responseSpec = callGetPrisonersByBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)
    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(0)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForPermittedPrisonerAndBooker(BOOKER_REFERENCE)
    verify(prisonerSearchClientSpy, times(2)).getPrisonerByIdAsMono(any())
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner1Dto.prisonerId)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner2Dto.prisonerId)
    verify(prisonApiClientSpy, times(0)).getVisitBalancesAsMono(prisoner1Dto.prisonerId)
    verify(prisonApiClientSpy, times(0)).getVisitBalancesAsMono(prisoner2Dto.prisonerId)
  }

  @Test
  fun `when INTERNAL_SERVER_ERROR is returned from prisoner search then INTERNAL_SERVER_ERROR status is sent back`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      BOOKER_REFERENCE,
      listOf(
        PermittedPrisonerForBookerDto(prisoner1Dto.prisonerId, true, listOf()),
        PermittedPrisonerForBookerDto(prisoner2Dto.prisonerId, true, listOf()),
      ),
    )
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerId, null, HttpStatus.INTERNAL_SERVER_ERROR)

    // When
    val responseSpec = callGetPrisonersByBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE)

    // Then
    responseSpec.expectStatus().is5xxServerError
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForPermittedPrisonerAndBooker(BOOKER_REFERENCE)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner1Dto.prisonerId)
    verify(prisonApiClientSpy, times(0)).getVisitBalancesAsMono(prisoner1Dto.prisonerId)
    verify(prisonApiClientSpy, times(0)).getVisitBalancesAsMono(prisoner2Dto.prisonerId)
  }

  @Test
  fun `when NOT_FOUND  is returned from get prison then empty list is returned`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      BOOKER_REFERENCE,
      listOf(
        PermittedPrisonerForBookerDto(prisoner1Dto.prisonerId, true, listOf()),
        PermittedPrisonerForBookerDto(prisoner2Dto.prisonerId, true, listOf()),
      ),
    )
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, null)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerId, prisoner1Dto)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner2Dto.prisonerId, prisoner2Dto)

    // When
    val responseSpec = callGetPrisonersByBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)
    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(0)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForPermittedPrisonerAndBooker(BOOKER_REFERENCE)
    verify(prisonerSearchClientSpy, times(2)).getPrisonerByIdAsMono(any())
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner1Dto.prisonerId)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner2Dto.prisonerId)
    verify(prisonApiClientSpy, times(1)).getVisitBalancesAsMono(prisoner1Dto.prisonerId)
    verify(prisonApiClientSpy, times(1)).getVisitBalancesAsMono(prisoner2Dto.prisonerId)
  }

  @Test
  fun `when NOT_FOUND is returned from get visit balances then prisoners are still returned`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      BOOKER_REFERENCE,
      listOf(
        PermittedPrisonerForBookerDto(prisoner1Dto.prisonerId, true, listOf()),
        PermittedPrisonerForBookerDto(prisoner2Dto.prisonerId, true, listOf()),
      ),
    )
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerId, prisoner1Dto)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner2Dto.prisonerId, prisoner2Dto)
    prisonApiMockServer.stubGetVisitBalances(prisoner1Dto.prisonerId, null)
    prisonApiMockServer.stubGetVisitBalances(prisoner2Dto.prisonerId, null)

    // When
    val responseSpec = callGetPrisonersByBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)
    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(2)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForPermittedPrisonerAndBooker(BOOKER_REFERENCE)
    verify(prisonerSearchClientSpy, times(2)).getPrisonerByIdAsMono(any())
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner1Dto.prisonerId)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner2Dto.prisonerId)
    verify(prisonApiClientSpy, times(1)).getVisitBalancesAsMono(prisoner1Dto.prisonerId)
    verify(prisonApiClientSpy, times(1)).getVisitBalancesAsMono(prisoner2Dto.prisonerId)
  }

  @Test
  fun `when INTERNAL_SERVER_ERROR is returned from get prison then INTERNAL_SERVER_ERROR status is sent back`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      BOOKER_REFERENCE,
      listOf(
        PermittedPrisonerForBookerDto(prisoner1Dto.prisonerId, true, listOf()),
        PermittedPrisonerForBookerDto(prisoner2Dto.prisonerId, true, listOf()),
      ),
    )
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, null, HttpStatus.INTERNAL_SERVER_ERROR)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerId, prisoner1Dto)

    // When
    val responseSpec = callGetPrisonersByBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE)

    // Then
    responseSpec.expectStatus().is5xxServerError
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForPermittedPrisonerAndBooker(BOOKER_REFERENCE)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner1Dto.prisonerId)
    verify(prisonApiClientSpy, times(1)).getVisitBalancesAsMono(prisoner1Dto.prisonerId)
    verify(prisonApiClientSpy, times(0)).getVisitBalancesAsMono(prisoner2Dto.prisonerId)
  }

  @Test
  fun `when get prisoners by booker called without correct role then access forbidden is returned`() {
    // When
    val invalidRoleHttpHeaders = setAuthorisation(roles = listOf("ROLE_INVALID"))
    val responseSpec = callGetPrisonersByBooker(webTestClient, invalidRoleHttpHeaders, BOOKER_REFERENCE)

    // Then
    responseSpec.expectStatus().isForbidden

    // And

    verify(prisonVisitBookerRegistryClientSpy, times(0)).getPermittedVisitorsForPermittedPrisonerAndBooker(any())
    verify(prisonerSearchClientSpy, times(0)).getPrisonerByIdAsMono(any())
    verify(prisonApiClientSpy, times(0)).getVisitBalancesAsMono(prisoner1Dto.prisonerId)
    verify(prisonApiClientSpy, times(0)).getVisitBalancesAsMono(prisoner2Dto.prisonerId)
  }

  @Test
  fun `when get prisoners by booker called without token then unauthorised status  is returned`() {
    // When
    val responseSpec = webTestClient.get().uri("/public/booker/booker-1/prisoners").exchange()

    // Then
    responseSpec.expectStatus().isUnauthorized

    // And

    verify(prisonVisitBookerRegistryClientSpy, times(0)).getPermittedVisitorsForPermittedPrisonerAndBooker(any())
    verify(prisonerSearchClientSpy, times(0)).getPrisonerByIdAsMono(any())
  }

  private fun assertPrisonerBasicDetails(prisonerBasicInfo: BookerPrisonerInfoDto, prisonerDto: PrisonerDto, availableVOs: Int, nextVORefreshDate: LocalDate) {
    Assertions.assertThat(prisonerBasicInfo.prisonerId).isEqualTo(prisonerDto.prisonerId)
    Assertions.assertThat(prisonerBasicInfo.firstName).isEqualTo(prisonerDto.firstName)
    Assertions.assertThat(prisonerBasicInfo.lastName).isEqualTo(prisonerDto.lastName)
    Assertions.assertThat(prisonerBasicInfo.availableVos).isEqualTo(availableVOs)
    Assertions.assertThat(prisonerBasicInfo.nextAvailableVoDate).isEqualTo(nextVORefreshDate)
  }

  private fun createCurrentIncentive(): CurrentIncentive {
    val incentiveLevel = IncentiveLevel("S", "Standard")
    return CurrentIncentive(incentiveLevel, LocalDateTime.now())
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): List<BookerPrisonerInfoDto> {
    return objectMapper.readValue(returnResult.returnResult().responseBody, Array<BookerPrisonerInfoDto>::class.java).toList()
  }

  fun callGetPrisonersByBooker(
    webTestClient: WebTestClient,
    authHttpHeaders: (HttpHeaders) -> Unit,
    bookerReference: String,
  ): WebTestClient.ResponseSpec {
    return webTestClient.get().uri(PUBLIC_BOOKER_GET_PRISONERS_CONTROLLER_PATH.replace("{bookerReference}", bookerReference))
      .headers(authHttpHeaders)
      .exchange()
  }
}
