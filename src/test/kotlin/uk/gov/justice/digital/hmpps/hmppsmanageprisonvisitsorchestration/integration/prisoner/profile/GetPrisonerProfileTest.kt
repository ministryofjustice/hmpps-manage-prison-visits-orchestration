package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.prisoner.profile

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonerOffenderSearchClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitSchedulerClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.PrisonerProfileDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.AlertDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.InmateDetailDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.PrisonerBookingSummaryDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.VisitBalancesDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.CurrentIncentive
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.IncentiveLevel
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

@DisplayName("Get Prisoner Profile")
class GetPrisonerProfileTest(
  @Value("\${prisoner.profile.past-visits.duration-in-months: -P3M}") private val pastVisitsPeriod: Period,
  @Value("\${prisoner.profile.future-visits.duration-in-months: P2M}") private val futureVisitsPeriod: Period,
) : IntegrationTestBase() {
  companion object {
    private const val prisonerId = "AA112233B"
    private const val firstName = "FirstName"
    private const val lastName = "LastName"
    private val dateOfBirth = LocalDate.of(2000, 1, 31)
    private const val category = "Category - C"
    private const val prisonId = "MDI"
    private val alert = AlertDto(comment = "Alert 1", alertCode = "C", alertCodeDescription = "Alert Code Desc", alertType = "T", alertTypeDescription = "Type Description", dateCreated = LocalDate.now())
  }

  private final val currentIncentive = createCurrentIncentive()

  private final val prisonerDto = createPrisoner(
    prisonerId = prisonerId,
    firstName = firstName,
    lastName = lastName,
    dateOfBirth = dateOfBirth,
    currentIncentive = currentIncentive,
  )

  private final val alerts = listOf(alert)
  private final val inmateDetailDto = createInmateDetails(prisonerId, category, alerts)
  private final val visitBalancesDto = createVisitBalancesDto()
  private final val prisonerBookingSummaryDto = createPrisonerBookingSummary(prisonerId, "Convicted")
  private final val visit1 = createVisitDto(reference = "visit-1", prisonerId = prisonerId)
  private final val visit2 = createVisitDto(reference = "visit-2", prisonerId = prisonerId)

  @SpyBean
  lateinit var visitSchedulerClientSpy: VisitSchedulerClient

  @SpyBean
  lateinit var prisonAPiClientSpy: PrisonApiClient

  @SpyBean
  lateinit var prisonerOffenderSearchClientSpy: PrisonerOffenderSearchClient

  @Autowired
  protected lateinit var objectMapper: ObjectMapper

  fun callGetPrisonerProfile(
    webTestClient: WebTestClient,
    authHttpHeaders: (HttpHeaders) -> Unit,
    prisonId: String,
    prisonerId: String,
  ): WebTestClient.ResponseSpec {
    return webTestClient.get().uri("/prisoner/$prisonId/$prisonerId/profile")
      .headers(authHttpHeaders)
      .exchange()
  }

  @Test
  fun `when valid prisoner prisoner profile is collated and returned`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(prisonerId, inmateDetailDto)
    prisonApiMockServer.stubGetBookings(prisonId, prisonerId, listOf(prisonerBookingSummaryDto))
    prisonApiMockServer.stubGetVisitBalances(prisonerId, visitBalancesDto)
    stubGetVisits(listOf(visit1, visit2))

    // When
    val responseSpec = callGetPrisonerProfile(webTestClient, roleVisitSchedulerHttpHeaders, prisonId, prisonerId)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerProfile = getResults(returnResult)

    assertPrisonerDtoDetails(prisonerProfile, prisonerDto)
    Assertions.assertThat(prisonerProfile.incentiveLevel).isEqualTo(prisonerDto.currentIncentive!!.level.description)
    assertInmateDetails(prisonerProfile, inmateDetailDto)
    Assertions.assertThat(prisonerProfile.convictedStatus).isEqualTo(prisonerBookingSummaryDto.convictedStatus)
    Assertions.assertThat(prisonerProfile.visitBalances).isEqualTo(visitBalancesDto)
    Assertions.assertThat(prisonerProfile.visits.size).isEqualTo(2)
    assertVisits(prisonerProfile, listOf(visit1, visit2))

    verifyExternalAPIClientCalls()
  }

  @Test
  fun `when prison API get prisoner details returns NOT_FOUND prisoner profile call returns NOT_FOUND status`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, null)
    prisonApiMockServer.stubGetInmateDetails(prisonerId, inmateDetailDto)
    prisonApiMockServer.stubGetBookings(prisonId, prisonerId, listOf(prisonerBookingSummaryDto))
    prisonApiMockServer.stubGetVisitBalances(prisonerId, visitBalancesDto)
    stubGetVisits(listOf(visit1, visit2))

    // When
    val responseSpec = callGetPrisonerProfile(webTestClient, roleVisitSchedulerHttpHeaders, prisonId, prisonerId)

    // Then
    responseSpec.expectStatus().isNotFound

    verifyExternalAPIClientCalls()
  }

  @Test
  fun `when prison API get inmate details returns NOT_FOUND prisoner profile call returns NOT_FOUND status`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(prisonerId, null)
    prisonApiMockServer.stubGetBookings(prisonId, prisonerId, listOf(prisonerBookingSummaryDto))
    prisonApiMockServer.stubGetVisitBalances(prisonerId, visitBalancesDto)
    stubGetVisits(listOf(visit1, visit2))

    // When
    val responseSpec = callGetPrisonerProfile(webTestClient, roleVisitSchedulerHttpHeaders, prisonId, prisonerId)

    // Then
    responseSpec.expectStatus().isNotFound

    verifyExternalAPIClientCalls()
  }

  @Test
  fun `when prison returned by prison API does not match prison id passed to endpoint prisoner profile call returns internal server error`() {
    // Given prisoner's prison ID is different to the prison ID passed in endpoint
    val prisonerDto = createPrisoner(
      prisonerId = prisonerId,
      prisonId = "XYZ",
      firstName = firstName,
      lastName = lastName,
      dateOfBirth = dateOfBirth,
      currentIncentive = currentIncentive,
    )

    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(prisonerId, inmateDetailDto)
    prisonApiMockServer.stubGetBookings(prisonId, prisonerId, listOf(prisonerBookingSummaryDto))
    prisonApiMockServer.stubGetVisitBalances(prisonerId, visitBalancesDto)
    stubGetVisits(listOf(visit1, visit2))

    // When
    val responseSpec = callGetPrisonerProfile(webTestClient, roleVisitSchedulerHttpHeaders, prisonId, prisonerId)

    // Then
    responseSpec.expectStatus().is4xxClientError

    verifyExternalAPIClientCalls()
  }

  @Test
  fun `when prison API get visit balances returns NOT_FOUND prisoner profile call returns a profile profile with visitBalances as null`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(prisonerId, inmateDetailDto)
    prisonApiMockServer.stubGetBookings(prisonId, prisonerId, listOf(prisonerBookingSummaryDto))
    prisonApiMockServer.stubGetVisitBalances(prisonerId, null)
    stubGetVisits(listOf(visit1, visit2))

    // When
    val responseSpec = callGetPrisonerProfile(webTestClient, roleVisitSchedulerHttpHeaders, prisonId, prisonerId)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerProfile = getResults(returnResult)

    assertPrisonerDtoDetails(prisonerProfile, prisonerDto)
    Assertions.assertThat(prisonerProfile.incentiveLevel).isEqualTo(prisonerDto.currentIncentive!!.level.description)
    assertInmateDetails(prisonerProfile, inmateDetailDto)
    Assertions.assertThat(prisonerProfile.convictedStatus).isEqualTo(prisonerBookingSummaryDto.convictedStatus)
    Assertions.assertThat(prisonerProfile.visitBalances).isNull()
    Assertions.assertThat(prisonerProfile.visits.size).isEqualTo(2)
    assertVisits(prisonerProfile, listOf(visit1, visit2))

    verifyExternalAPIClientCalls()
  }

  @Test
  fun `when prison API get bookings returns no records some prisoner profile details are blank`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(prisonerId, inmateDetailDto)
    // get bookings will return empty contents
    prisonApiMockServer.stubGetBookings(prisonId, prisonerId, mutableListOf())
    prisonApiMockServer.stubGetVisitBalances(prisonerId, visitBalancesDto)
    stubGetVisits(listOf(visit1, visit2))

    // When
    val responseSpec = callGetPrisonerProfile(webTestClient, roleVisitSchedulerHttpHeaders, prisonId, prisonerId)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerProfile = getResults(returnResult)

    assertPrisonerDtoDetails(prisonerProfile, prisonerDto)
    Assertions.assertThat(prisonerProfile.incentiveLevel).isEqualTo(prisonerDto.currentIncentive!!.level.description)
    assertInmateDetails(prisonerProfile, inmateDetailDto)
    Assertions.assertThat(prisonerProfile.convictedStatus).isNull()
    Assertions.assertThat(prisonerProfile.visitBalances).isEqualTo(visitBalancesDto)
    assertVisits(prisonerProfile, listOf(visit1, visit2))

    verifyExternalAPIClientCalls()
  }

  @Test
  fun `when prison API get bookings returns multiple records only 1st one is populated on prisoner profile`() {
    // Given

    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(prisonerId, inmateDetailDto)
    val prisonerBookingSummaryDto1 = createPrisonerBookingSummary(prisonerId, "Remand")
    prisonApiMockServer.stubGetBookings(prisonId, prisonerId, listOf(prisonerBookingSummaryDto, prisonerBookingSummaryDto1))
    prisonApiMockServer.stubGetVisitBalances(prisonerId, visitBalancesDto)
    stubGetVisits(listOf(visit1, visit2))
    // When
    val responseSpec = callGetPrisonerProfile(webTestClient, roleVisitSchedulerHttpHeaders, prisonId, prisonerId)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerProfile = getResults(returnResult)

    assertPrisonerDtoDetails(prisonerProfile, prisonerDto)
    Assertions.assertThat(prisonerProfile.incentiveLevel).isEqualTo(prisonerDto.currentIncentive!!.level.description)
    assertInmateDetails(prisonerProfile, inmateDetailDto)
    Assertions.assertThat(prisonerProfile.convictedStatus).isEqualTo(prisonerBookingSummaryDto.convictedStatus)
    Assertions.assertThat(prisonerProfile.visitBalances).isEqualTo(visitBalancesDto)
    assertVisits(prisonerProfile, listOf(visit1, visit2))

    verifyExternalAPIClientCalls()
  }

  @Test
  fun `when prison API get prisoner current incentive is null prisoner profile current incentive is null`() {
    // Given
    val prisonerDto = createPrisoner(
      prisonerId = prisonerId,
      firstName = firstName,
      lastName = lastName,
      dateOfBirth = dateOfBirth,
      currentIncentive = null,
    )
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(prisonerId, inmateDetailDto)
    prisonApiMockServer.stubGetBookings(prisonId, prisonerId, listOf(prisonerBookingSummaryDto))
    prisonApiMockServer.stubGetVisitBalances(prisonerId, visitBalancesDto)
    stubGetVisits(listOf(visit1, visit2))
    // When
    val responseSpec = callGetPrisonerProfile(webTestClient, roleVisitSchedulerHttpHeaders, prisonId, prisonerId)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerProfile = getResults(returnResult)

    assertPrisonerDtoDetails(prisonerProfile, prisonerDto)
    Assertions.assertThat(prisonerProfile.incentiveLevel).isNull()
    assertInmateDetails(prisonerProfile, inmateDetailDto)
    Assertions.assertThat(prisonerProfile.convictedStatus).isEqualTo(prisonerBookingSummaryDto.convictedStatus)
    Assertions.assertThat(prisonerProfile.visitBalances).isEqualTo(visitBalancesDto)
    assertVisits(prisonerProfile, listOf(visit1, visit2))

    verifyExternalAPIClientCalls()
  }

  @Test
  fun `when visits do not exist for prisoner visits list is empty`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(prisonerId, inmateDetailDto)
    prisonApiMockServer.stubGetBookings(prisonId, prisonerId, listOf(prisonerBookingSummaryDto))
    prisonApiMockServer.stubGetVisitBalances(prisonerId, visitBalancesDto)
    stubGetVisits(mutableListOf())

    // When
    val responseSpec = callGetPrisonerProfile(webTestClient, roleVisitSchedulerHttpHeaders, prisonId, prisonerId)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerProfile = getResults(returnResult)

    assertPrisonerDtoDetails(prisonerProfile, prisonerDto)
    Assertions.assertThat(prisonerProfile.incentiveLevel).isEqualTo(prisonerDto.currentIncentive!!.level.description)
    assertInmateDetails(prisonerProfile, inmateDetailDto)
    Assertions.assertThat(prisonerProfile.convictedStatus).isEqualTo(prisonerBookingSummaryDto.convictedStatus)
    Assertions.assertThat(prisonerProfile.visitBalances).isEqualTo(visitBalancesDto)
    Assertions.assertThat(prisonerProfile.visits).isEmpty()

    verifyExternalAPIClientCalls()
  }

  private fun assertPrisonerDtoDetails(prisonerProfile: PrisonerProfileDto, prisonerDto: PrisonerDto) {
    Assertions.assertThat(prisonerProfile.prisonerId).isEqualTo(prisonerDto.prisonerNumber)
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

  private fun assertVisits(prisonerProfile: PrisonerProfileDto, visits: List<VisitDto>) {
    val visitReferences = prisonerProfile.visits.stream().map { it.reference }.toList()
    Assertions.assertThat(prisonerProfile.visits.size).isEqualTo(visits.size)
    visits.forEach {
      Assertions.assertThat(visitReferences).contains(it.reference)
    }
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): PrisonerProfileDto {
    return objectMapper.readValue(returnResult.returnResult().responseBody, PrisonerProfileDto::class.java)
  }

  private fun createPrisoner(
    prisonerId: String,
    firstName: String,
    lastName: String,
    dateOfBirth: LocalDate,
    prisonId: String = "MDI",
    prisonName: String = "HMP Leeds",
    cellLocation: String? = null,
    currentIncentive: CurrentIncentive? = null,
  ): PrisonerDto {
    return PrisonerDto(
      prisonerNumber = prisonerId,
      firstName = firstName,
      lastName = lastName,
      dateOfBirth = dateOfBirth,
      prisonId = prisonId,
      prisonName = prisonName,
      cellLocation = cellLocation,
      currentIncentive = currentIncentive,
    )
  }

  private fun createInmateDetails(
    prisonerId: String,
    category: String? = null,
    alerts: List<AlertDto>? = null,
  ): InmateDetailDto {
    return InmateDetailDto(offenderNo = prisonerId, category = category, alerts = alerts)
  }

  private fun createVisitBalancesDto(): VisitBalancesDto {
    return VisitBalancesDto(remainingVo = 10, remainingPvo = 10)
  }

  private fun createCurrentIncentive(): CurrentIncentive {
    val incentiveLevel = IncentiveLevel("S", "Standard")
    return CurrentIncentive(incentiveLevel, LocalDateTime.now())
  }

  private fun createPrisonerBookingSummary(prisonerId: String, convictedStatus: String): PrisonerBookingSummaryDto {
    return PrisonerBookingSummaryDto(prisonerId, convictedStatus)
  }

  private fun stubGetVisits(visits: List<VisitDto>) {
    visitSchedulerMockServer.stubGetVisits(
      prisonerId,
      listOf("BOOKED", "CANCELLED"),
      LocalDateTime.now().minus(pastVisitsPeriod).with(TemporalAdjusters.firstDayOfMonth()).truncatedTo(ChronoUnit.DAYS),
      LocalDateTime.now().plus(futureVisitsPeriod).with(TemporalAdjusters.lastDayOfMonth()).truncatedTo(ChronoUnit.DAYS),
      0,
      1000,
      visits,
    )
  }

  private fun verifyExternalAPIClientCalls() {
    verify(visitSchedulerClientSpy, times(1)).getVisitsAsMono(any())
    verify(prisonerOffenderSearchClientSpy, times(1)).getPrisonerByIdAsMono(any())
    verify(prisonAPiClientSpy, times(1)).getInmateDetailsAsMono(any())
    verify(prisonAPiClientSpy, times(1)).getVisitBalancesAsMono(any())
    verify(prisonAPiClientSpy, times(1)).getBookingsAsMono(any(), any())
  }
}
