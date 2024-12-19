package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.booker

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonRegisterClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonVisitBookerRegistryClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller.PUBLIC_BOOKER_GET_PRISONERS_CONTROLLER_PATH
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.BookerPrisonerInfoDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.PermittedPrisonerForBookerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.PermittedVisitorsForPermittedPrisonerBookerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.RegisteredPrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.VisitBalancesDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.register.PrisonRegisterPrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.CurrentIncentive
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.IncentiveLevel
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.utils.DateUtils
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.utils.VisitBalancesUtil
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("Get permitted prisoners for booker")
class GetPermittedPrisonersForBookerTest : IntegrationTestBase() {
  companion object {
    private const val PRISON_CODE = "MDI"
    private const val PRISON_NAME = "Test Prison (MDI)"
    private const val BOOKER_REFERENCE = "booker-1"
    private const val PRISONER1_ID = "AA112233B"
    private const val PRISONER2_ID = "BB112233B"

    private val dateUtils = DateUtils()
    private val visitBalancesUtil = VisitBalancesUtil(dateUtils)
  }

  @MockitoSpyBean
  lateinit var prisonVisitBookerRegistryClientSpy: PrisonVisitBookerRegistryClient

  @MockitoSpyBean
  lateinit var prisonerSearchClientSpy: PrisonerSearchClient

  @MockitoSpyBean
  lateinit var prisonApiClientSpy: PrisonApiClient

  @MockitoSpyBean
  lateinit var prisonRegisterClientSpy: PrisonRegisterClient

  private final val prisonDto = PrisonRegisterPrisonDto(PRISON_CODE, PRISON_NAME, true)
  private final val registeredPrisonDto = RegisteredPrisonDto(PRISON_CODE, PRISON_NAME)

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

  private val visitBalance1 = VisitBalancesDto(4, 3, LocalDate.now().plusDays(7), LocalDate.now().plusDays(2))
  private val visitBalance2 = VisitBalancesDto(2, 3, LocalDate.now().plusDays(14), LocalDate.now().plusDays(7))

  @Test
  fun `when booker has valid prisoners then all allowed prisoners are returned`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerNumber, prisoner1Dto)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner2Dto.prisonerNumber, prisoner2Dto)
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      BOOKER_REFERENCE,
      listOf(
        PermittedPrisonerForBookerDto(prisoner1Dto.prisonerNumber, true, PRISON_CODE, listOf(PermittedVisitorsForPermittedPrisonerBookerDto(1L, true))),
        PermittedPrisonerForBookerDto(prisoner2Dto.prisonerNumber, true, PRISON_CODE, listOf(PermittedVisitorsForPermittedPrisonerBookerDto(1L, true))),
      ),
    )
    prisonApiMockServer.stubGetVisitBalances(prisoner1Dto.prisonerNumber, visitBalance1)
    prisonApiMockServer.stubGetVisitBalances(prisoner2Dto.prisonerNumber, visitBalance2)
    prisonRegisterMockServer.stubGetPrison(PRISON_CODE, prisonDto)

    // When
    val responseSpec = callGetPrisonersByBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(2)
    assertPrisonerBasicDetails(prisonerBasicInfo = prisonerDetailsList[0], prisonerDto = prisoner1Dto, availableVOs = 7, nextVORefreshDate = visitBalancesUtil.calculateVoRenewalDate(visitBalance1), registeredPrisonDto)
    assertPrisonerBasicDetails(prisonerBasicInfo = prisonerDetailsList[1], prisonerDto = prisoner2Dto, availableVOs = 5, nextVORefreshDate = visitBalancesUtil.calculateVoRenewalDate(visitBalance2), registeredPrisonDto)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedPrisonersForBooker(BOOKER_REFERENCE)
    verify(prisonerSearchClientSpy, times(2)).getPrisonerByIdAsMono(any())
    verify(prisonApiClientSpy, times(1)).getVisitBalancesAsMono(prisoner1Dto.prisonerNumber)
    verify(prisonApiClientSpy, times(1)).getVisitBalancesAsMono(prisoner2Dto.prisonerNumber)
    verify(prisonRegisterClientSpy, times(2)).getPrisonAsMonoEmptyIfNotFound(PRISON_CODE)
  }

  @Test
  fun `when booker has no valid prisoners then an empty list is returned`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerNumber, prisoner1Dto)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner2Dto.prisonerNumber, prisoner2Dto)
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      BOOKER_REFERENCE,
      listOf(),
    )
    prisonApiMockServer.stubGetVisitBalances(prisoner1Dto.prisonerNumber, visitBalance1)
    prisonApiMockServer.stubGetVisitBalances(prisoner2Dto.prisonerNumber, visitBalance2)
    prisonRegisterMockServer.stubGetPrison(PRISON_CODE, prisonDto)

    // When
    val responseSpec = callGetPrisonersByBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(0)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedPrisonersForBooker(BOOKER_REFERENCE)
    verify(prisonerSearchClientSpy, times(0)).getPrisonerByIdAsMono(any())
    verify(prisonApiClientSpy, times(0)).getVisitBalancesAsMono(prisoner1Dto.prisonerNumber)
    verify(prisonApiClientSpy, times(0)).getVisitBalancesAsMono(prisoner2Dto.prisonerNumber)
    verify(prisonRegisterClientSpy, times(0)).getPrisonAsMonoEmptyIfNotFound(any())
  }

  @Test
  fun `when booker has valid prisoners but 1 of them cannot be retrieved from prisoner search then that prisoner is not returned`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerNumber, prisoner1Dto)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner2Dto.prisonerNumber, null)
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      BOOKER_REFERENCE,
      listOf(
        PermittedPrisonerForBookerDto(prisoner1Dto.prisonerNumber, true, PRISON_CODE, listOf()),
        PermittedPrisonerForBookerDto(prisoner2Dto.prisonerNumber, true, PRISON_CODE, listOf()),
      ),
    )
    prisonApiMockServer.stubGetVisitBalances(prisoner1Dto.prisonerNumber, visitBalance1)
    prisonApiMockServer.stubGetVisitBalances(prisoner2Dto.prisonerNumber, visitBalance2)
    prisonRegisterMockServer.stubGetPrison(PRISON_CODE, PrisonRegisterPrisonDto(PRISON_CODE, "MDI", active = true))
    prisonRegisterMockServer.stubGetPrison(PRISON_CODE, prisonDto)

    // When
    val responseSpec = callGetPrisonersByBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(1)
    assertPrisonerBasicDetails(prisonerBasicInfo = prisonerDetailsList[0], prisonerDto = prisoner1Dto, availableVOs = 7, nextVORefreshDate = visitBalancesUtil.calculateVoRenewalDate(visitBalance1), registeredPrisonDto)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedPrisonersForBooker(BOOKER_REFERENCE)
    verify(prisonerSearchClientSpy, times(2)).getPrisonerByIdAsMono(any())
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner1Dto.prisonerNumber)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner2Dto.prisonerNumber)
    verify(prisonApiClientSpy, times(1)).getVisitBalancesAsMono(prisoner1Dto.prisonerNumber)
    verify(prisonApiClientSpy, times(1)).getVisitBalancesAsMono(prisoner2Dto.prisonerNumber)
    verify(prisonRegisterClientSpy, times(2)).getPrisonAsMonoEmptyIfNotFound(PRISON_CODE)
  }

  @Test
  fun `when booker has valid prisoners but 1 of them has prison code as null then that prisoner is not returned`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerNumber, prisoner1Dto)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner3Dto.prisonerNumber, prisoner3Dto)
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      BOOKER_REFERENCE,
      listOf(
        PermittedPrisonerForBookerDto(prisoner1Dto.prisonerNumber, true, PRISON_CODE, listOf()),
        PermittedPrisonerForBookerDto(prisoner3Dto.prisonerNumber, true, PRISON_CODE, listOf()),
      ),
    )
    prisonApiMockServer.stubGetVisitBalances(prisoner1Dto.prisonerNumber, visitBalance1)
    prisonRegisterMockServer.stubGetPrison(PRISON_CODE, prisonDto)

    // When
    val responseSpec = callGetPrisonersByBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(2)
    assertPrisonerBasicDetails(prisonerBasicInfo = prisonerDetailsList[0], prisonerDto = prisoner1Dto, availableVOs = 7, nextVORefreshDate = visitBalancesUtil.calculateVoRenewalDate(visitBalance1), registeredPrisonDto)
    assertPrisonerBasicDetails(prisonerBasicInfo = prisonerDetailsList[1], prisonerDto = prisoner3Dto, availableVOs = 0, nextVORefreshDate = visitBalancesUtil.calculateVoRenewalDate(null), registeredPrisonDto)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedPrisonersForBooker(BOOKER_REFERENCE)
    verify(prisonerSearchClientSpy, times(2)).getPrisonerByIdAsMono(any())
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner1Dto.prisonerNumber)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner3Dto.prisonerNumber)
    verify(prisonApiClientSpy, times(1)).getVisitBalancesAsMono(prisoner1Dto.prisonerNumber)
    verify(prisonApiClientSpy, times(1)).getVisitBalancesAsMono(prisoner3Dto.prisonerNumber)
    verify(prisonRegisterClientSpy, times(2)).getPrisonAsMonoEmptyIfNotFound(PRISON_CODE)
  }

  @Test
  fun `when booker has valid prisoners but none of them can be retrieved from prisoner search then an empty list is returned`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      BOOKER_REFERENCE,
      listOf(
        PermittedPrisonerForBookerDto(prisoner1Dto.prisonerNumber, true, PRISON_CODE, listOf()),
        PermittedPrisonerForBookerDto(prisoner2Dto.prisonerNumber, true, PRISON_CODE, listOf()),
      ),
    )
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerNumber, null)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner2Dto.prisonerNumber, null)
    prisonRegisterMockServer.stubGetPrison(PRISON_CODE, prisonDto)

    // When
    val responseSpec = callGetPrisonersByBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(0)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedPrisonersForBooker(BOOKER_REFERENCE)
    verify(prisonerSearchClientSpy, times(2)).getPrisonerByIdAsMono(any())
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner1Dto.prisonerNumber)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner2Dto.prisonerNumber)
    verify(prisonApiClientSpy, times(1)).getVisitBalancesAsMono(prisoner1Dto.prisonerNumber)
    verify(prisonApiClientSpy, times(1)).getVisitBalancesAsMono(prisoner2Dto.prisonerNumber)
    verify(prisonRegisterClientSpy, times(2)).getPrisonAsMonoEmptyIfNotFound(PRISON_CODE)
  }

  @Test
  fun `when NOT_FOUND is returned from booker registry then NOT_FOUND status is sent back`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      BOOKER_REFERENCE,
      null,
      HttpStatus.NOT_FOUND,
    )
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerNumber, null)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner2Dto.prisonerNumber, null)
    prisonRegisterMockServer.stubGetPrison(PRISON_CODE, prisonDto)

    // When
    val responseSpec = callGetPrisonersByBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE)

    // Then
    responseSpec.expectStatus().isNotFound
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedPrisonersForBooker(BOOKER_REFERENCE)
    verify(prisonerSearchClientSpy, times(0)).getPrisonerByIdAsMono(any())
    verify(prisonApiClientSpy, times(0)).getVisitBalancesAsMono(prisoner1Dto.prisonerNumber)
    verify(prisonApiClientSpy, times(0)).getVisitBalancesAsMono(prisoner2Dto.prisonerNumber)
    verify(prisonRegisterClientSpy, times(0)).getPrisonAsMonoEmptyIfNotFound(any())
  }

  @Test
  fun `when INTERNAL_SERVER_ERROR  is returned from booker registry then INTERNAL_SERVER_ERROR status is sent back`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      BOOKER_REFERENCE,
      null,
      HttpStatus.INTERNAL_SERVER_ERROR,
    )
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerNumber, null)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner2Dto.prisonerNumber, null)
    prisonRegisterMockServer.stubGetPrison(PRISON_CODE, prisonDto)

    // When
    val responseSpec = callGetPrisonersByBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE)

    // Then
    responseSpec.expectStatus().is5xxServerError
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedPrisonersForBooker(BOOKER_REFERENCE)
    verify(prisonerSearchClientSpy, times(0)).getPrisonerByIdAsMono(any())
    verify(prisonApiClientSpy, times(0)).getVisitBalancesAsMono(prisoner1Dto.prisonerNumber)
    verify(prisonApiClientSpy, times(0)).getVisitBalancesAsMono(prisoner2Dto.prisonerNumber)
    verify(prisonRegisterClientSpy, times(0)).getPrisonAsMonoEmptyIfNotFound(any())
  }

  @Test
  fun `when NOT_FOUND  is returned from prisoner search then empty list is returned`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      BOOKER_REFERENCE,
      listOf(
        PermittedPrisonerForBookerDto(prisoner1Dto.prisonerNumber, true, PRISON_CODE, listOf()),
        PermittedPrisonerForBookerDto(prisoner2Dto.prisonerNumber, true, PRISON_CODE, listOf()),
      ),
    )
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerNumber, null, HttpStatus.NOT_FOUND)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner2Dto.prisonerNumber, null, HttpStatus.NOT_FOUND)
    prisonRegisterMockServer.stubGetPrison(PRISON_CODE, prisonDto)

    // When
    val responseSpec = callGetPrisonersByBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)
    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(0)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedPrisonersForBooker(BOOKER_REFERENCE)
    verify(prisonerSearchClientSpy, times(2)).getPrisonerByIdAsMono(any())
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner1Dto.prisonerNumber)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner2Dto.prisonerNumber)
    verify(prisonApiClientSpy, times(1)).getVisitBalancesAsMono(prisoner1Dto.prisonerNumber)
    verify(prisonApiClientSpy, times(1)).getVisitBalancesAsMono(prisoner2Dto.prisonerNumber)
    verify(prisonRegisterClientSpy, times(2)).getPrisonAsMonoEmptyIfNotFound(PRISON_CODE)
  }

  @Test
  fun `when INTERNAL_SERVER_ERROR is returned from prisoner search then INTERNAL_SERVER_ERROR status is sent back`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      BOOKER_REFERENCE,
      listOf(
        PermittedPrisonerForBookerDto(prisoner1Dto.prisonerNumber, true, PRISON_CODE, listOf()),
        PermittedPrisonerForBookerDto(prisoner2Dto.prisonerNumber, true, PRISON_CODE, listOf()),
      ),
    )
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerNumber, null, HttpStatus.INTERNAL_SERVER_ERROR)
    prisonRegisterMockServer.stubGetPrison(PRISON_CODE, prisonDto)

    // When
    val responseSpec = callGetPrisonersByBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE)

    // Then
    responseSpec.expectStatus().is5xxServerError
  }

  @Test
  fun `when NOT_FOUND is returned from get visit balances then prisoners are still returned`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      BOOKER_REFERENCE,
      listOf(
        PermittedPrisonerForBookerDto(prisoner1Dto.prisonerNumber, true, PRISON_CODE, listOf()),
        PermittedPrisonerForBookerDto(prisoner2Dto.prisonerNumber, true, PRISON_CODE, listOf()),
      ),
    )
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerNumber, prisoner1Dto)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner2Dto.prisonerNumber, prisoner2Dto)
    prisonApiMockServer.stubGetVisitBalances(prisoner1Dto.prisonerNumber, null)
    prisonApiMockServer.stubGetVisitBalances(prisoner2Dto.prisonerNumber, null)
    prisonRegisterMockServer.stubGetPrison(PRISON_CODE, prisonDto)

    // When
    val responseSpec = callGetPrisonersByBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)
    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(2)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedPrisonersForBooker(BOOKER_REFERENCE)
    verify(prisonerSearchClientSpy, times(2)).getPrisonerByIdAsMono(any())
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner1Dto.prisonerNumber)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner2Dto.prisonerNumber)
    verify(prisonApiClientSpy, times(1)).getVisitBalancesAsMono(prisoner1Dto.prisonerNumber)
    verify(prisonApiClientSpy, times(1)).getVisitBalancesAsMono(prisoner2Dto.prisonerNumber)
    verify(prisonRegisterClientSpy, times(2)).getPrisonAsMonoEmptyIfNotFound(PRISON_CODE)
  }

  @Test
  fun `when NOT_FOUND is returned from prison register then prisoner list is returned but prison name is same as code`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      BOOKER_REFERENCE,
      listOf(
        PermittedPrisonerForBookerDto(prisoner1Dto.prisonerNumber, true, PRISON_CODE, listOf()),
        PermittedPrisonerForBookerDto(prisoner2Dto.prisonerNumber, true, PRISON_CODE, listOf()),
      ),
    )
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerNumber, prisoner1Dto)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner2Dto.prisonerNumber, prisoner2Dto)
    prisonRegisterMockServer.stubGetPrison(PRISON_CODE, null, HttpStatus.NOT_FOUND)
    prisonApiMockServer.stubGetVisitBalances(prisoner1Dto.prisonerNumber, visitBalance1)
    prisonApiMockServer.stubGetVisitBalances(prisoner2Dto.prisonerNumber, visitBalance2)

    // expect both code and name to be same as PRISON_CODE when prison registry returns 404
    val registeredPrisonDtoWhenNotReturned = RegisteredPrisonDto(PRISON_CODE, PRISON_CODE)
    // When
    val responseSpec = callGetPrisonersByBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)
    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(2)
    assertPrisonerBasicDetails(prisonerBasicInfo = prisonerDetailsList[0], prisonerDto = prisoner1Dto, availableVOs = 7, nextVORefreshDate = visitBalancesUtil.calculateVoRenewalDate(visitBalance1), registeredPrisonDtoWhenNotReturned)
    assertPrisonerBasicDetails(prisonerBasicInfo = prisonerDetailsList[1], prisonerDto = prisoner2Dto, availableVOs = 5, nextVORefreshDate = visitBalancesUtil.calculateVoRenewalDate(visitBalance2), registeredPrisonDtoWhenNotReturned)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedPrisonersForBooker(BOOKER_REFERENCE)
    verify(prisonerSearchClientSpy, times(2)).getPrisonerByIdAsMono(any())
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner1Dto.prisonerNumber)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner2Dto.prisonerNumber)
    verify(prisonApiClientSpy, times(1)).getVisitBalancesAsMono(prisoner1Dto.prisonerNumber)
    verify(prisonApiClientSpy, times(1)).getVisitBalancesAsMono(prisoner2Dto.prisonerNumber)
    verify(prisonRegisterClientSpy, times(2)).getPrisonAsMonoEmptyIfNotFound(PRISON_CODE)
  }

  @Test
  fun `when INTERNAL_SERVER_ERROR is returned from prison register then prisoner list is returned but prison name is same as code`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      BOOKER_REFERENCE,
      listOf(
        PermittedPrisonerForBookerDto(prisoner1Dto.prisonerNumber, true, PRISON_CODE, listOf()),
        PermittedPrisonerForBookerDto(prisoner2Dto.prisonerNumber, true, PRISON_CODE, listOf()),
      ),
    )
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerNumber, prisoner1Dto)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner2Dto.prisonerNumber, prisoner2Dto)
    prisonRegisterMockServer.stubGetPrison(PRISON_CODE, null, HttpStatus.INTERNAL_SERVER_ERROR)
    prisonApiMockServer.stubGetVisitBalances(prisoner1Dto.prisonerNumber, visitBalance1)
    prisonApiMockServer.stubGetVisitBalances(prisoner2Dto.prisonerNumber, visitBalance2)

    // When
    val responseSpec = callGetPrisonersByBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE)

    // Then
    responseSpec.expectStatus().is5xxServerError

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedPrisonersForBooker(BOOKER_REFERENCE)
  }

  @Test
  fun `when get prisoners by booker called without correct role then access forbidden is returned`() {
    // When
    val invalidRoleHttpHeaders = setAuthorisation(roles = listOf("ROLE_INVALID"))
    val responseSpec = callGetPrisonersByBooker(webTestClient, invalidRoleHttpHeaders, BOOKER_REFERENCE)

    // Then
    responseSpec.expectStatus().isForbidden

    // And

    verify(prisonVisitBookerRegistryClientSpy, times(0)).getPermittedPrisonersForBooker(any())
    verify(prisonerSearchClientSpy, times(0)).getPrisonerByIdAsMono(any())
    verify(prisonApiClientSpy, times(0)).getVisitBalancesAsMono(prisoner1Dto.prisonerNumber)
    verify(prisonApiClientSpy, times(0)).getVisitBalancesAsMono(prisoner2Dto.prisonerNumber)
    verify(prisonRegisterClientSpy, times(0)).getPrison(any())
  }

  @Test
  fun `when get prisoners by booker called without token then unauthorised status  is returned`() {
    // When
    val responseSpec = webTestClient.get().uri("/public/booker/booker-1/prisoners").exchange()

    // Then
    responseSpec.expectStatus().isUnauthorized

    // And

    verify(prisonVisitBookerRegistryClientSpy, times(0)).getPermittedPrisonersForBooker(any())
    verify(prisonerSearchClientSpy, times(0)).getPrisonerByIdAsMono(any())
    verify(prisonRegisterClientSpy, times(0)).getPrisonAsMonoEmptyIfNotFound(any())
  }

  private fun assertPrisonerBasicDetails(prisonerBasicInfo: BookerPrisonerInfoDto, prisonerDto: PrisonerDto, availableVOs: Int, nextVORefreshDate: LocalDate, registeredPrisonDto: RegisteredPrisonDto) {
    Assertions.assertThat(prisonerBasicInfo.prisoner.prisonerNumber).isEqualTo(prisonerDto.prisonerNumber)
    Assertions.assertThat(prisonerBasicInfo.prisoner.firstName).isEqualTo(prisonerDto.firstName)
    Assertions.assertThat(prisonerBasicInfo.prisoner.lastName).isEqualTo(prisonerDto.lastName)
    Assertions.assertThat(prisonerBasicInfo.availableVos).isEqualTo(availableVOs)
    Assertions.assertThat(prisonerBasicInfo.nextAvailableVoDate).isEqualTo(nextVORefreshDate)
    Assertions.assertThat(prisonerBasicInfo.registeredPrison).isEqualTo(registeredPrisonDto)
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
