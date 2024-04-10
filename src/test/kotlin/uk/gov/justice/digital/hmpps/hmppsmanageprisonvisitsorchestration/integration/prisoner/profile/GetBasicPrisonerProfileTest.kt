package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.prisoner.profile

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.CurrentIncentive
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.IncentiveLevel
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerBasicInfoDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("Get Basic Prisoner Profile for a list of prisoners")
class GetBasicPrisonerProfileTest : IntegrationTestBase() {

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

  @SpyBean
  lateinit var prisonerSearchClientSpy: PrisonerSearchClient

  fun callGetBasicPrisonerProfile(
    webTestClient: WebTestClient,
    authHttpHeaders: (HttpHeaders) -> Unit,
    prisonerIds: List<String>,
  ): WebTestClient.ResponseSpec {
    return webTestClient.get().uri("/prisoner/${prisonerIds.joinToString(",")}/basic-details")
      .headers(authHttpHeaders)
      .exchange()
  }

  @Test
  fun `when single valid prisoner number passed then basic profile is returned`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById("AA112233B", prisoner1Dto)

    // When
    val responseSpec = callGetBasicPrisonerProfile(webTestClient, rolePublicVisitsBookingHttpHeaders, listOf("AA112233B"))

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    assertPrisonerBasicDetails(prisonerDetailsList[0], prisoner1Dto)

    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(any())
  }

  @Test
  fun `when multiple valid prisoner numbers passed then basic profile is returned`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById("AA112233B", prisoner1Dto)
    prisonOffenderSearchMockServer.stubGetPrisonerById("BB112233B", prisoner2Dto)

    // When
    val responseSpec = callGetBasicPrisonerProfile(webTestClient, rolePublicVisitsBookingHttpHeaders, listOf("AA112233B", "BB112233B"))

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    assertPrisonerBasicDetails(prisonerDetailsList[0], prisoner1Dto)
    assertPrisonerBasicDetails(prisonerDetailsList[1], prisoner2Dto)

    verify(prisonerSearchClientSpy, times(2)).getPrisonerByIdAsMono(any())
  }

  @Test
  fun `when multiple valid prisoner numbers passed but one of them throws NOT_FOUND then basic profile and blank profile is returned`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById("AA112233B", prisoner1Dto)
    prisonOffenderSearchMockServer.stubGetPrisonerById("BB112233B", null)

    // When
    val responseSpec = callGetBasicPrisonerProfile(webTestClient, rolePublicVisitsBookingHttpHeaders, listOf("AA112233B", "BB112233B"))

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    assertPrisonerBasicDetails(prisonerDetailsList[0], prisoner1Dto)
    assertUnknownPrisonerDetails(prisonerDetailsList[1], prisoner2Dto)

    verify(prisonerSearchClientSpy, times(2)).getPrisonerByIdAsMono(any())
  }

  @Test
  fun `when prisoner search returns NOT_FOUND prisoner profile call returns a basic prisoner profile`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById("AA112233B", null)

    // When
    val responseSpec = callGetBasicPrisonerProfile(webTestClient, rolePublicVisitsBookingHttpHeaders, listOf("AA112233B"))

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    assertUnknownPrisonerDetails(prisonerDetailsList[0], prisoner1Dto)

    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(any())
  }

  @Test
  fun `when get basic prisoner details called without correct role then access forbidden is returned`() {
    // When
    val responseSpec = callGetBasicPrisonerProfile(webTestClient, setAuthorisation(roles = listOf("TEST")), listOf("AA112233B"))

    // Then
    responseSpec.expectStatus().isForbidden

    // And
    verify(prisonerSearchClientSpy, times(0)).getPrisonerByIdAsMono(any())
  }

  @Test
  fun `when get basic prisoner details called without token then unauthorised status returned`() {
    // When
    val responseSpec = webTestClient.put().uri("/prisoner/AA123/basic-details")
      .exchange()

    // Then
    responseSpec.expectStatus().isUnauthorized

    // And
    verify(prisonerSearchClientSpy, times(0)).getPrisonerByIdAsMono(any())
  }

  private fun assertPrisonerBasicDetails(prisonerBasicInfo: PrisonerBasicInfoDto, prisonerDto: PrisonerDto) {
    Assertions.assertThat(prisonerBasicInfo.prisonerNumber).isEqualTo(prisonerDto.prisonerNumber)
    Assertions.assertThat(prisonerBasicInfo.firstName).isEqualTo(prisonerDto.firstName)
    Assertions.assertThat(prisonerBasicInfo.lastName).isEqualTo(prisonerDto.lastName)
    Assertions.assertThat(prisonerBasicInfo.dateOfBirth).isEqualTo(prisonerDto.dateOfBirth)
    Assertions.assertThat(prisonerBasicInfo.prisonId).isEqualTo(prisonerDto.prisonId)
  }

  private fun assertUnknownPrisonerDetails(prisonerBasicInfo: PrisonerBasicInfoDto, prisonerDto: PrisonerDto) {
    Assertions.assertThat(prisonerBasicInfo.prisonerNumber).isEqualTo(prisonerDto.prisonerNumber)
    Assertions.assertThat(prisonerBasicInfo.firstName).isEqualTo("UNKNOWN")
    Assertions.assertThat(prisonerBasicInfo.lastName).isEqualTo("UNKNOWN")
    Assertions.assertThat(prisonerBasicInfo.dateOfBirth).isNull()
    Assertions.assertThat(prisonerBasicInfo.prisonId).isNull()
  }

  private fun createCurrentIncentive(): CurrentIncentive {
    val incentiveLevel = IncentiveLevel("S", "Standard")
    return CurrentIncentive(incentiveLevel, LocalDateTime.now())
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): List<PrisonerBasicInfoDto> {
    return objectMapper.readValue(returnResult.returnResult().responseBody, Array<PrisonerBasicInfoDto>::class.java).toList()
  }
}
