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
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonVisitBookerRegistryClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller.PUBLIC_BOOKER_GET_PRISONERS_CONTROLLER_PATH
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.PermittedPrisonerForBookerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.PermittedVisitorsForPermittedPrisonerBookerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.CurrentIncentive
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.IncentiveLevel
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerInfoDto
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

  @Test
  fun `when booker has valid prisoners then all allowed prisoners are returned`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerNumber, prisoner1Dto)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner2Dto.prisonerNumber, prisoner2Dto)
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      BOOKER_REFERENCE,
      listOf(
        PermittedPrisonerForBookerDto(prisoner1Dto.prisonerNumber, true, listOf(PermittedVisitorsForPermittedPrisonerBookerDto(1L, true))),
        PermittedPrisonerForBookerDto(prisoner2Dto.prisonerNumber, true, listOf(PermittedVisitorsForPermittedPrisonerBookerDto(1L, true))),
      ),
    )

    // When
    val responseSpec = callGetPrisonersByBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(2)
    assertPrisonerBasicDetails(prisonerDetailsList[0], prisoner1Dto)
    assertPrisonerBasicDetails(prisonerDetailsList[1], prisoner2Dto)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForPermittedPrisonerAndBooker(BOOKER_REFERENCE)
    verify(prisonerSearchClientSpy, times(2)).getPrisonerByIdAsMono(any())
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner1Dto.prisonerNumber)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner2Dto.prisonerNumber)
  }

  @Test
  fun `when booker has no valid prisoners then an empty list is returned`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerNumber, prisoner1Dto)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner2Dto.prisonerNumber, prisoner2Dto)
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      BOOKER_REFERENCE,
      listOf(),
    )

    // When
    val responseSpec = callGetPrisonersByBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(0)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForPermittedPrisonerAndBooker(BOOKER_REFERENCE)
    verify(prisonerSearchClientSpy, times(0)).getPrisonerByIdAsMono(any())
  }

  @Test
  fun `when booker has valid prisoners but 1 of them cannot be retrieved from prisoner search then that prisoner is not returned`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerNumber, prisoner1Dto)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner2Dto.prisonerNumber, null)
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      BOOKER_REFERENCE,
      listOf(
        PermittedPrisonerForBookerDto(prisoner1Dto.prisonerNumber, true, listOf()),
        PermittedPrisonerForBookerDto(prisoner2Dto.prisonerNumber, true, listOf()),
      ),
    )

    // When
    val responseSpec = callGetPrisonersByBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(1)
    assertPrisonerBasicDetails(prisonerDetailsList[0], prisoner1Dto)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForPermittedPrisonerAndBooker(BOOKER_REFERENCE)
    verify(prisonerSearchClientSpy, times(2)).getPrisonerByIdAsMono(any())
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner1Dto.prisonerNumber)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner2Dto.prisonerNumber)
  }

  @Test
  fun `when booker has valid prisoners but 1 of them has prison code as null then that prisoner is not returned`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerNumber, prisoner1Dto)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner3Dto.prisonerNumber, prisoner3Dto)
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      BOOKER_REFERENCE,
      listOf(
        PermittedPrisonerForBookerDto(prisoner1Dto.prisonerNumber, true, listOf()),
        PermittedPrisonerForBookerDto(prisoner3Dto.prisonerNumber, true, listOf()),
      ),
    )

    // When
    val responseSpec = callGetPrisonersByBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(1)
    assertPrisonerBasicDetails(prisonerDetailsList[0], prisoner1Dto)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForPermittedPrisonerAndBooker(BOOKER_REFERENCE)
    verify(prisonerSearchClientSpy, times(2)).getPrisonerByIdAsMono(any())
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner1Dto.prisonerNumber)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner3Dto.prisonerNumber)
  }

  @Test
  fun `when booker has valid prisoners but prison is not on VSIP then that prisoner is not returned`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerNumber, prisoner1Dto)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner2Dto.prisonerNumber, prisoner2Dto)
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, null)
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      BOOKER_REFERENCE,
      listOf(
        PermittedPrisonerForBookerDto(prisoner1Dto.prisonerNumber, true, listOf()),
        PermittedPrisonerForBookerDto(prisoner2Dto.prisonerNumber, true, listOf()),
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
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner1Dto.prisonerNumber)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner2Dto.prisonerNumber)
  }

  @Test
  fun `when booker has valid prisoners but prison is inactive then that prisoner is not returned`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerNumber, prisoner1Dto)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner4Dto.prisonerNumber, prisoner4Dto)
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)
    visitSchedulerMockServer.stubGetPrison(inactivePrison.code, inactivePrison)
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      BOOKER_REFERENCE,
      listOf(
        PermittedPrisonerForBookerDto(prisoner1Dto.prisonerNumber, true, listOf()),
        PermittedPrisonerForBookerDto(prisoner4Dto.prisonerNumber, true, listOf()),
      ),
    )

    // When
    val responseSpec = callGetPrisonersByBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(1)
    assertPrisonerBasicDetails(prisonerDetailsList[0], prisoner1Dto)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForPermittedPrisonerAndBooker(BOOKER_REFERENCE)
    verify(prisonerSearchClientSpy, times(2)).getPrisonerByIdAsMono(any())
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner1Dto.prisonerNumber)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner4Dto.prisonerNumber)
  }

  @Test
  fun `when booker has valid prisoners but prison is inactive for public users then that prisoner is not returned`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerNumber, prisoner1Dto)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner5Dto.prisonerNumber, prisoner5Dto)
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)
    visitSchedulerMockServer.stubGetPrison(inactiveForPublicPrison.code, inactiveForPublicPrison)
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      BOOKER_REFERENCE,
      listOf(
        PermittedPrisonerForBookerDto(prisoner1Dto.prisonerNumber, true, listOf()),
        PermittedPrisonerForBookerDto(prisoner5Dto.prisonerNumber, true, listOf()),
      ),
    )

    // When
    val responseSpec = callGetPrisonersByBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(1)
    assertPrisonerBasicDetails(prisonerDetailsList[0], prisoner1Dto)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForPermittedPrisonerAndBooker(BOOKER_REFERENCE)
    verify(prisonerSearchClientSpy, times(2)).getPrisonerByIdAsMono(any())
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner1Dto.prisonerNumber)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner5Dto.prisonerNumber)
  }

  @Test
  fun `when booker has valid prisoners but prison is only active for staff users then that prisoner is not returned`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerNumber, prisoner1Dto)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner5Dto.prisonerNumber, prisoner5Dto)
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)
    visitSchedulerMockServer.stubGetPrison(inactiveForPublicPrison.code, onlyActiveForStaffPrison)
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      BOOKER_REFERENCE,
      listOf(
        PermittedPrisonerForBookerDto(prisoner1Dto.prisonerNumber, true, listOf()),
        PermittedPrisonerForBookerDto(prisoner5Dto.prisonerNumber, true, listOf()),
      ),
    )

    // When
    val responseSpec = callGetPrisonersByBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(1)
    assertPrisonerBasicDetails(prisonerDetailsList[0], prisoner1Dto)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForPermittedPrisonerAndBooker(BOOKER_REFERENCE)
    verify(prisonerSearchClientSpy, times(2)).getPrisonerByIdAsMono(any())
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner1Dto.prisonerNumber)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner5Dto.prisonerNumber)
  }

  @Test
  fun `when booker has valid prisoners but none of them can be retrieved from prisoner search then an empty list is returned`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      BOOKER_REFERENCE,
      listOf(
        PermittedPrisonerForBookerDto(prisoner1Dto.prisonerNumber, true, listOf()),
        PermittedPrisonerForBookerDto(prisoner2Dto.prisonerNumber, true, listOf()),
      ),
    )
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerNumber, null)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner2Dto.prisonerNumber, null)

    // When
    val responseSpec = callGetPrisonersByBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(0)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForPermittedPrisonerAndBooker(BOOKER_REFERENCE)
    verify(prisonerSearchClientSpy, times(2)).getPrisonerByIdAsMono(any())
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner1Dto.prisonerNumber)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner2Dto.prisonerNumber)
  }

  @Test
  fun `when NOT_FOUND  is returned from booker registry then NOT_FOUND status is sent back`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      BOOKER_REFERENCE,
      null,
      HttpStatus.NOT_FOUND,
    )
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerNumber, null)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner2Dto.prisonerNumber, null)

    // When
    val responseSpec = callGetPrisonersByBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE)

    // Then
    responseSpec.expectStatus().isNotFound
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForPermittedPrisonerAndBooker(BOOKER_REFERENCE)
    verify(prisonerSearchClientSpy, times(0)).getPrisonerByIdAsMono(any())
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
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerNumber, null)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner2Dto.prisonerNumber, null)

    // When
    val responseSpec = callGetPrisonersByBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE)

    // Then
    responseSpec.expectStatus().is5xxServerError
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForPermittedPrisonerAndBooker(BOOKER_REFERENCE)
    verify(prisonerSearchClientSpy, times(0)).getPrisonerByIdAsMono(any())
  }

  @Test
  fun `when NOT_FOUND  is returned from prisoner search then empty list is returned`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      BOOKER_REFERENCE,
      listOf(
        PermittedPrisonerForBookerDto(prisoner1Dto.prisonerNumber, true, listOf()),
        PermittedPrisonerForBookerDto(prisoner2Dto.prisonerNumber, true, listOf()),
      ),
    )
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerNumber, null, HttpStatus.NOT_FOUND)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner2Dto.prisonerNumber, null, HttpStatus.NOT_FOUND)

    // When
    val responseSpec = callGetPrisonersByBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)
    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(0)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForPermittedPrisonerAndBooker(BOOKER_REFERENCE)
    verify(prisonerSearchClientSpy, times(2)).getPrisonerByIdAsMono(any())
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner1Dto.prisonerNumber)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner2Dto.prisonerNumber)
  }

  @Test
  fun `when INTERNAL_SERVER_ERROR is returned from prisoner search then INTERNAL_SERVER_ERROR status is sent back`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      BOOKER_REFERENCE,
      listOf(
        PermittedPrisonerForBookerDto(prisoner1Dto.prisonerNumber, true, listOf()),
        PermittedPrisonerForBookerDto(prisoner2Dto.prisonerNumber, true, listOf()),
      ),
    )
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerNumber, null, HttpStatus.INTERNAL_SERVER_ERROR)

    // When
    val responseSpec = callGetPrisonersByBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE)

    // Then
    responseSpec.expectStatus().is5xxServerError
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForPermittedPrisonerAndBooker(BOOKER_REFERENCE)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner1Dto.prisonerNumber)
  }

  @Test
  fun `when NOT_FOUND  is returned from get prison then empty list is returned`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      BOOKER_REFERENCE,
      listOf(
        PermittedPrisonerForBookerDto(prisoner1Dto.prisonerNumber, true, listOf()),
        PermittedPrisonerForBookerDto(prisoner2Dto.prisonerNumber, true, listOf()),
      ),
    )
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, null)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerNumber, prisoner1Dto)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner2Dto.prisonerNumber, prisoner2Dto)

    // When
    val responseSpec = callGetPrisonersByBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)
    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(0)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForPermittedPrisonerAndBooker(BOOKER_REFERENCE)
    verify(prisonerSearchClientSpy, times(2)).getPrisonerByIdAsMono(any())
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner1Dto.prisonerNumber)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner2Dto.prisonerNumber)
  }

  @Test
  fun `when INTERNAL_SERVER_ERROR is returned from get prison then INTERNAL_SERVER_ERROR status is sent back`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      BOOKER_REFERENCE,
      listOf(
        PermittedPrisonerForBookerDto(prisoner1Dto.prisonerNumber, true, listOf()),
        PermittedPrisonerForBookerDto(prisoner2Dto.prisonerNumber, true, listOf()),
      ),
    )
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, null, HttpStatus.INTERNAL_SERVER_ERROR)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerNumber, prisoner1Dto)

    // When
    val responseSpec = callGetPrisonersByBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE)

    // Then
    responseSpec.expectStatus().is5xxServerError
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForPermittedPrisonerAndBooker(BOOKER_REFERENCE)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner1Dto.prisonerNumber)
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

  private fun assertPrisonerBasicDetails(prisonerBasicInfo: PrisonerInfoDto, prisonerDto: PrisonerDto) {
    Assertions.assertThat(prisonerBasicInfo.prisonerNumber).isEqualTo(prisonerDto.prisonerNumber)
    Assertions.assertThat(prisonerBasicInfo.firstName).isEqualTo(prisonerDto.firstName)
    Assertions.assertThat(prisonerBasicInfo.lastName).isEqualTo(prisonerDto.lastName)
  }

  private fun createCurrentIncentive(): CurrentIncentive {
    val incentiveLevel = IncentiveLevel("S", "Standard")
    return CurrentIncentive(incentiveLevel, LocalDateTime.now())
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): List<PrisonerInfoDto> {
    return objectMapper.readValue(returnResult.returnResult().responseBody, Array<PrisonerInfoDto>::class.java).toList()
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
