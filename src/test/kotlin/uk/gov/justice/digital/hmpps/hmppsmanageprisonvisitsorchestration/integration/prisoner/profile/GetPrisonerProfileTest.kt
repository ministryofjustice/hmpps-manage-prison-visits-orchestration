package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.prisoner.profile

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.PrisonerProfileDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.AlertDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.InmateDetailDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.PrisonerBookingSummaryDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.VisitBalancesDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.CurrentIncentive
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.IncentiveLevel
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("Get Prisoner Profile")
class GetPrisonerProfileTest : IntegrationTestBase() {
  companion object {
    private const val offenderNo = "AA112233B"
    private const val firstName = "FirstName"
    private const val lastName = "LastName"
    private val dateOfBirth = LocalDate.of(2000, 1, 31)
    private const val category = "Category - C"
    private const val prisonId = "MDI"
    private val alert = AlertDto(offenderNo = offenderNo, comment = "Alert 1")
  }

  private final val currentIncentive = createCurrentIncentive()

  private final val prisonerDto = createPrisoner(
    offenderNo = offenderNo,
    firstName = firstName,
    lastName = lastName,
    dateOfBirth = dateOfBirth,
    currentIncentive = currentIncentive,
  )

  private final val alerts = listOf(alert)
  private final val inmateDetailDto = createInmateDetails(offenderNo, category, alerts)
  private final val visitBalancesDto = createVisitBalancesDto()
  private final val prisonerBookingSummaryDto = createPrisonerBookingSummary(offenderNo, "Convicted")

  @Autowired
  protected lateinit var objectMapper: ObjectMapper

  fun callGetPrisonerProfile(
    webTestClient: WebTestClient,
    authHttpHeaders: (HttpHeaders) -> Unit,
    offenderNo: String
  ): WebTestClient.ResponseSpec {
    return webTestClient.get().uri("/prisoner/$offenderNo/profile")
      .headers(authHttpHeaders)
      .exchange()
  }

  @Test
  fun `when valid prisoner prisoner profile is collated and returned`() {
    // Given

    prisonOffenderSearchMockServer.stubGetPrisonerById(offenderNo, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(offenderNo, inmateDetailDto)
    prisonApiMockServer.stubGetBookings(prisonerDto.prisonId, offenderNo, listOf(prisonerBookingSummaryDto))
    prisonApiMockServer.stubGetVisitBalances(offenderNo, visitBalancesDto)

    // When
    val responseSpec = callGetPrisonerProfile(webTestClient, roleVisitSchedulerHttpHeaders, offenderNo)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerProfile = getResults(returnResult)

    assertPrisonerDtoDetails(prisonerProfile, prisonerDto)
    Assertions.assertThat(prisonerProfile.incentiveLevel).isEqualTo(prisonerDto.currentIncentive!!.level.description)
    assertInmateDetails(prisonerProfile, inmateDetailDto)
    Assertions.assertThat(prisonerProfile.convictedStatus).isEqualTo(prisonerBookingSummaryDto.convictedStatus)
    Assertions.assertThat(prisonerProfile.visitBalances).isEqualTo(visitBalancesDto)
  }

  @Test
  fun `when prison API get prisoner details returns NOT_FOUND prisoner profile call returns NOT_FOUND status`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(offenderNo, null)
    prisonApiMockServer.stubGetInmateDetails(offenderNo, inmateDetailDto)
    prisonApiMockServer.stubGetBookings(prisonId, offenderNo, listOf(prisonerBookingSummaryDto))
    prisonApiMockServer.stubGetVisitBalances(offenderNo, visitBalancesDto)

    // When
    val responseSpec = callGetPrisonerProfile(webTestClient, roleVisitSchedulerHttpHeaders, offenderNo)

    // Then
    responseSpec.expectStatus().isNotFound
  }

  @Test
  fun `when prison API get inmate details returns NOT_FOUND prisoner profile call returns NOT_FOUND status`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(offenderNo, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(offenderNo, null)
    prisonApiMockServer.stubGetBookings(prisonId, offenderNo, listOf(prisonerBookingSummaryDto))
    prisonApiMockServer.stubGetVisitBalances(offenderNo, visitBalancesDto)

    // When
    val responseSpec = callGetPrisonerProfile(webTestClient, roleVisitSchedulerHttpHeaders, offenderNo)

    // Then
    responseSpec.expectStatus().isNotFound
  }
  @Test
  fun `when prison API get visit balances returns NOT_FOUND prisoner profile call returns NOT_FOUND status`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(offenderNo, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(offenderNo, inmateDetailDto)
    prisonApiMockServer.stubGetBookings(prisonId, offenderNo, listOf(prisonerBookingSummaryDto))
    prisonApiMockServer.stubGetVisitBalances(offenderNo, null)

    // When
    val responseSpec = callGetPrisonerProfile(webTestClient, roleVisitSchedulerHttpHeaders, offenderNo)

    // Then
    responseSpec.expectStatus().isNotFound
  }

  @Test
  fun `when prison API get bookings returns no records some prisoner profile details are blank`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(offenderNo, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(offenderNo, inmateDetailDto)
    // get bookings will return empty contents
    prisonApiMockServer.stubGetBookings(prisonId, offenderNo, mutableListOf())
    prisonApiMockServer.stubGetVisitBalances(offenderNo, visitBalancesDto)

    // When
    val responseSpec = callGetPrisonerProfile(webTestClient, roleVisitSchedulerHttpHeaders, offenderNo)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerProfile = getResults(returnResult)

    assertPrisonerDtoDetails(prisonerProfile, prisonerDto)
    Assertions.assertThat(prisonerProfile.incentiveLevel).isEqualTo(prisonerDto.currentIncentive!!.level.description)
    assertInmateDetails(prisonerProfile, inmateDetailDto)
    Assertions.assertThat(prisonerProfile.convictedStatus).isNull()
    Assertions.assertThat(prisonerProfile.visitBalances).isEqualTo(visitBalancesDto)
  }

  @Test
  fun `when prison API get bookings returns multiple records only 1st one is populated on prisoner profile`() {
    // Given

    prisonOffenderSearchMockServer.stubGetPrisonerById(offenderNo, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(offenderNo, inmateDetailDto)
    val prisonerBookingSummaryDto1 = createPrisonerBookingSummary(offenderNo, "Remand")
    prisonApiMockServer.stubGetBookings(prisonerDto.prisonId, offenderNo, listOf(prisonerBookingSummaryDto, prisonerBookingSummaryDto1))
    prisonApiMockServer.stubGetVisitBalances(offenderNo, visitBalancesDto)

    // When
    val responseSpec = callGetPrisonerProfile(webTestClient, roleVisitSchedulerHttpHeaders, offenderNo)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerProfile = getResults(returnResult)

    assertPrisonerDtoDetails(prisonerProfile, prisonerDto)
    Assertions.assertThat(prisonerProfile.incentiveLevel).isEqualTo(prisonerDto.currentIncentive!!.level.description)
    assertInmateDetails(prisonerProfile, inmateDetailDto)
    Assertions.assertThat(prisonerProfile.convictedStatus).isEqualTo(prisonerBookingSummaryDto.convictedStatus)
    Assertions.assertThat(prisonerProfile.visitBalances).isEqualTo(visitBalancesDto)
  }

  @Test
  fun `when prison API get prisoner current incentive is null prisoner profile current incentive is null`() {
    // Given
    val prisonerDto = createPrisoner(
      offenderNo = offenderNo,
      firstName = firstName,
      lastName = lastName,
      dateOfBirth = dateOfBirth,
      currentIncentive = null,
    )
    prisonOffenderSearchMockServer.stubGetPrisonerById(offenderNo, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(offenderNo, inmateDetailDto)
    prisonApiMockServer.stubGetBookings(prisonerDto.prisonId, offenderNo, listOf(prisonerBookingSummaryDto))
    prisonApiMockServer.stubGetVisitBalances(offenderNo, visitBalancesDto)

    // When
    val responseSpec = callGetPrisonerProfile(webTestClient, roleVisitSchedulerHttpHeaders, offenderNo)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerProfile = getResults(returnResult)

    assertPrisonerDtoDetails(prisonerProfile, prisonerDto)
    Assertions.assertThat(prisonerProfile.incentiveLevel).isNull()
    assertInmateDetails(prisonerProfile, inmateDetailDto)
    Assertions.assertThat(prisonerProfile.convictedStatus).isEqualTo(prisonerBookingSummaryDto.convictedStatus)
    Assertions.assertThat(prisonerProfile.visitBalances).isEqualTo(visitBalancesDto)
  }

  private fun assertPrisonerDtoDetails(prisonerProfile: PrisonerProfileDto, prisonerDto: PrisonerDto) {
    Assertions.assertThat(prisonerProfile.offenderNo).isEqualTo(prisonerDto.prisonerNumber)
    Assertions.assertThat(prisonerProfile.firstName).isEqualTo(prisonerDto.firstName)
    Assertions.assertThat(prisonerProfile.lastName).isEqualTo(prisonerDto.lastName)
    Assertions.assertThat(prisonerProfile.dateOfBirth).isEqualTo(prisonerDto.dateOfBirth)
    Assertions.assertThat(prisonerProfile.prisonId).isEqualTo(prisonerDto.prisonId)
    Assertions.assertThat(prisonerProfile.prisonName).isEqualTo(prisonerDto.prisonName)
    Assertions.assertThat(prisonerProfile.cellLocation).isEqualTo(prisonerDto.cellLocation)
  }

  private fun assertInmateDetails(prisonerProfile: PrisonerProfileDto, inmateDetails: InmateDetailDto) {
    Assertions.assertThat(prisonerProfile.category).isEqualTo(inmateDetails.category)
    Assertions.assertThat(prisonerProfile.alerts).isEqualTo(inmateDetails.alerts)
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): PrisonerProfileDto {
    return objectMapper.readValue(returnResult.returnResult().responseBody, PrisonerProfileDto::class.java)
  }

  private fun createPrisoner(
    offenderNo: String,
    firstName: String,
    lastName: String,
    dateOfBirth: LocalDate,
    prisonId: String = "MDI",
    prisonName: String = "HMP Leeds",
    cellLocation: String? = null,
    currentIncentive: CurrentIncentive? = null
  ): PrisonerDto {
    return PrisonerDto(
      prisonerNumber = offenderNo,
      firstName = firstName,
      lastName = lastName,
      dateOfBirth = dateOfBirth,
      prisonId = prisonId,
      prisonName = prisonName,
      cellLocation = cellLocation,
      currentIncentive = currentIncentive
    )
  }

  private fun createInmateDetails(
    offenderNo: String,
    category: String? = null,
    alerts: List<AlertDto>? = null
  ): InmateDetailDto {
    return InmateDetailDto(offenderNo = offenderNo, category = category, alerts = alerts)
  }

  private fun createVisitBalancesDto(): VisitBalancesDto {
    return VisitBalancesDto(remainingVo = 10, remainingPvo = 10)
  }

  private fun createCurrentIncentive(): CurrentIncentive {
    val incentiveLevel = IncentiveLevel("S", "Standard")
    return CurrentIncentive(incentiveLevel, LocalDateTime.now())
  }

  private fun createPrisonerBookingSummary(offenderNo: String, convictedStatus: String): PrisonerBookingSummaryDto {
    return PrisonerBookingSummaryDto(offenderNo, convictedStatus)
  }
}
