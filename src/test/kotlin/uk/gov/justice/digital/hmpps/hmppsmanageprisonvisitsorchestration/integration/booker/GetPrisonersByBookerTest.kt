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
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.BookerPrisonersDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.CurrentIncentive
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.IncentiveLevel
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerBasicInfoDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("Get prisoners by booker")
class GetPrisonersByBookerTest : IntegrationTestBase() {
  @SpyBean
  lateinit var prisonVisitBookerRegistryClientSpy: PrisonVisitBookerRegistryClient

  @SpyBean
  lateinit var prisonerSearchClientSpy: PrisonerSearchClient

  private val prisonCode = "HEI"

  private val bookerReference = "booker-1"

  private final val currentIncentive = createCurrentIncentive()

  private final val prisoner1Dto = createPrisoner(
    prisonerId = "AA112233B",
    firstName = "FirstName",
    lastName = "LastName",
    dateOfBirth = LocalDate.of(2000, 1, 31),
    currentIncentive = currentIncentive,
  )

  private final val prisoner2Dto = createPrisoner(
    prisonerId = "BB112233B",
    firstName = "First",
    lastName = "Last",
    dateOfBirth = LocalDate.of(2001, 12, 1),
    currentIncentive = currentIncentive,
  )

  fun callGetPrisonersByBooker(
    webTestClient: WebTestClient,
    authHttpHeaders: (HttpHeaders) -> Unit,
    bookerReference: String,
  ): WebTestClient.ResponseSpec {
    return webTestClient.get().uri("/public/booker/$bookerReference/prisoners")
      .headers(authHttpHeaders)
      .exchange()
  }

  @Test
  fun `when booker has valid prisoners then all allowed prisoners are returned`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerNumber, prisoner1Dto)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner2Dto.prisonerNumber, prisoner2Dto)
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      bookerReference,
      listOf(
        BookerPrisonersDto(prisoner1Dto.prisonerNumber, prisonCode),
        BookerPrisonersDto(prisoner2Dto.prisonerNumber, prisonCode),
      ),
    )

    // When
    val responseSpec = callGetPrisonersByBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, bookerReference)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(2)
    assertPrisonerBasicDetails(prisonerDetailsList[0], prisoner1Dto)
    assertPrisonerBasicDetails(prisonerDetailsList[1], prisoner2Dto)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPrisonersForBooker(bookerReference)
    verify(prisonerSearchClientSpy, times(2)).getPrisonerByIdAsMono(any())
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner1Dto.prisonerNumber)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner2Dto.prisonerNumber)
  }

  @Test
  fun `when booker has no valid prisoners then an empty list is returned`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerNumber, prisoner1Dto)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner2Dto.prisonerNumber, prisoner2Dto)
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      bookerReference,
      listOf(),
    )

    // When
    val responseSpec = callGetPrisonersByBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, bookerReference)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(0)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPrisonersForBooker(bookerReference)
    verify(prisonerSearchClientSpy, times(0)).getPrisonerByIdAsMono(any())
  }

  @Test
  fun `when booker has valid prisoners but 1 of them cannot be retrieved from prisoner search then that prisoner is not returned`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerNumber, prisoner1Dto)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner2Dto.prisonerNumber, null)

    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      bookerReference,
      listOf(
        BookerPrisonersDto(prisoner1Dto.prisonerNumber, prisonCode),
        BookerPrisonersDto(prisoner2Dto.prisonerNumber, prisonCode),
      ),
    )

    // When
    val responseSpec = callGetPrisonersByBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, bookerReference)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(1)
    assertPrisonerBasicDetails(prisonerDetailsList[0], prisoner1Dto)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPrisonersForBooker(bookerReference)
    verify(prisonerSearchClientSpy, times(2)).getPrisonerByIdAsMono(any())
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner1Dto.prisonerNumber)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner2Dto.prisonerNumber)
  }

  @Test
  fun `when booker has valid prisoners but none of them can be retrieved from prisoner search then an empty list is returned`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      bookerReference,
      listOf(
        BookerPrisonersDto(prisoner1Dto.prisonerNumber, prisonCode),
        BookerPrisonersDto(prisoner2Dto.prisonerNumber, prisonCode),
      ),
    )
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerNumber, null)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner2Dto.prisonerNumber, null)

    // When
    val responseSpec = callGetPrisonersByBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, bookerReference)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(0)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPrisonersForBooker(bookerReference)
    verify(prisonerSearchClientSpy, times(2)).getPrisonerByIdAsMono(any())
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner1Dto.prisonerNumber)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner2Dto.prisonerNumber)
  }

  @Test
  fun `when NOT_FOUND  is returned from booker registry then NOT_FOUND status is sent back`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      bookerReference,
      null,
      HttpStatus.NOT_FOUND,
    )
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerNumber, null)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner2Dto.prisonerNumber, null)

    // When
    val responseSpec = callGetPrisonersByBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, bookerReference)

    // Then
    responseSpec.expectStatus().isNotFound
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPrisonersForBooker(bookerReference)
    verify(prisonerSearchClientSpy, times(0)).getPrisonerByIdAsMono(any())
  }

  @Test
  fun `when INTERNAL_SERVER_ERROR  is returned from booker registry then INTERNAL_SERVER_ERROR status is sent back`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      bookerReference,
      null,
      HttpStatus.INTERNAL_SERVER_ERROR,
    )
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerNumber, null)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner2Dto.prisonerNumber, null)

    // When
    val responseSpec = callGetPrisonersByBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, bookerReference)

    // Then
    responseSpec.expectStatus().is5xxServerError
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPrisonersForBooker(bookerReference)
    verify(prisonerSearchClientSpy, times(0)).getPrisonerByIdAsMono(any())
  }

  @Test
  fun `when NOT_FOUND  is returned from prisoner search then empty list is returned`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      bookerReference,
      listOf(
        BookerPrisonersDto(prisoner1Dto.prisonerNumber, prisonCode),
        BookerPrisonersDto(prisoner2Dto.prisonerNumber, prisonCode),
      ),
    )
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerNumber, null, HttpStatus.NOT_FOUND)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner2Dto.prisonerNumber, null, HttpStatus.NOT_FOUND)

    // When
    val responseSpec = callGetPrisonersByBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, bookerReference)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)
    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(0)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPrisonersForBooker(bookerReference)
    verify(prisonerSearchClientSpy, times(2)).getPrisonerByIdAsMono(any())
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner1Dto.prisonerNumber)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner2Dto.prisonerNumber)
  }

  @Test
  fun `when INTERNAL_SERVER_ERROR is returned from prisoner search then INTERNAL_SERVER_ERROR status is sent back`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      bookerReference,
      listOf(
        BookerPrisonersDto(prisoner1Dto.prisonerNumber, prisonCode),
        BookerPrisonersDto(prisoner2Dto.prisonerNumber, prisonCode),
      ),
    )
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisoner1Dto.prisonerNumber, null, HttpStatus.INTERNAL_SERVER_ERROR)

    // When
    val responseSpec = callGetPrisonersByBooker(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, bookerReference)

    // Then
    responseSpec.expectStatus().is5xxServerError
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPrisonersForBooker(bookerReference)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(prisoner1Dto.prisonerNumber)
  }

  @Test
  fun `when get prisoners by booker called without correct role then access forbidden is returned`() {
    // When
    val invalidRoleHttpHeaders = setAuthorisation(roles = listOf("ROLE_INVALID"))
    val responseSpec = callGetPrisonersByBooker(webTestClient, invalidRoleHttpHeaders, bookerReference)

    // Then
    responseSpec.expectStatus().isForbidden

    // And

    verify(prisonVisitBookerRegistryClientSpy, times(0)).getPrisonersForBooker(any())
    verify(prisonerSearchClientSpy, times(0)).getPrisonerByIdAsMono(any())
  }

  @Test
  fun `when get prisoners by booker called without token then unauthorised status  is returned`() {
    // When
    val responseSpec = webTestClient.get().uri("/public/booker/$bookerReference/prisoners").exchange()

    // Then
    responseSpec.expectStatus().isUnauthorized

    // And

    verify(prisonVisitBookerRegistryClientSpy, times(0)).getPrisonersForBooker(any())
    verify(prisonerSearchClientSpy, times(0)).getPrisonerByIdAsMono(any())
  }

  private fun assertPrisonerBasicDetails(prisonerBasicInfo: PrisonerBasicInfoDto, prisonerDto: PrisonerDto) {
    Assertions.assertThat(prisonerBasicInfo.prisonerNumber).isEqualTo(prisonerDto.prisonerNumber)
    Assertions.assertThat(prisonerBasicInfo.firstName).isEqualTo(prisonerDto.firstName)
    Assertions.assertThat(prisonerBasicInfo.lastName).isEqualTo(prisonerDto.lastName)
    Assertions.assertThat(prisonerBasicInfo.dateOfBirth).isEqualTo(prisonerDto.dateOfBirth)
  }

  private fun createCurrentIncentive(): CurrentIncentive {
    val incentiveLevel = IncentiveLevel("S", "Standard")
    return CurrentIncentive(incentiveLevel, LocalDateTime.now())
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): List<PrisonerBasicInfoDto> {
    return objectMapper.readValue(returnResult.returnResult().responseBody, Array<PrisonerBasicInfoDto>::class.java).toList()
  }
}
