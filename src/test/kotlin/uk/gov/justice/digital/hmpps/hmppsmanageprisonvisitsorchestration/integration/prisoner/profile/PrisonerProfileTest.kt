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
class PrisonerProfileTest : IntegrationTestBase() {
  companion object {
    private const val offenderNo = "AA112233B"
    private const val firstName = "FirstName"
    private const val lastName = "LastName"
    private val dateOfBirth = LocalDate.of(2000, 1, 31)
    private const val category = "Category - C"
  }
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
    val currentIncentive = createCurrentIncentive()
    val prisonerDto = createPrisoner(
      offenderNo = offenderNo,
      firstName = firstName,
      lastName = lastName,
      dateOfBirth = dateOfBirth,
      currentIncentive = currentIncentive,
    )
    val alert = AlertDto(offenderNo = offenderNo, comment = "Alert 1")
    val alerts = listOf(alert)
    val inmateDetailDto = createInmateDetails(offenderNo, category, alerts)
    val visitBalancesDto = createVisitBalancesDto()
    val prisonerBookingSummaryDto = createPrisonerBookingSummary(offenderNo)

    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(inmateDetailDto)
    prisonApiMockServer.stubGetBookings(prisonerDto.prisonId, offenderNo, listOf(prisonerBookingSummaryDto))
    prisonApiMockServer.stubGetVisitBalances(offenderNo, visitBalancesDto)

    // When
    val responseSpec = callGetPrisonerProfile(webTestClient, roleVisitSchedulerHttpHeaders, offenderNo)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerProfile = getResults(returnResult)

    Assertions.assertThat(prisonerProfile.offenderNo).isEqualTo(prisonerDto.prisonerNumber)
    Assertions.assertThat(prisonerProfile.firstName).isEqualTo(prisonerDto.firstName)
    Assertions.assertThat(prisonerProfile.lastName).isEqualTo(prisonerDto.lastName)
    Assertions.assertThat(prisonerProfile.dateOfBirth).isEqualTo(prisonerDto.dateOfBirth)
    Assertions.assertThat(prisonerProfile.prisonId).isEqualTo(prisonerDto.prisonId)
    Assertions.assertThat(prisonerProfile.prisonName).isEqualTo(prisonerDto.prisonName)
    Assertions.assertThat(prisonerProfile.cellLocation).isEqualTo(prisonerDto.cellLocation)
    Assertions.assertThat(prisonerProfile.category).isEqualTo(inmateDetailDto.category)
    Assertions.assertThat(prisonerProfile.convictedStatus).isEqualTo(prisonerBookingSummaryDto.convictedStatus)
    Assertions.assertThat(prisonerProfile.incentiveLevel).isEqualTo(currentIncentive.level.description)
    Assertions.assertThat(prisonerProfile.alerts).isEqualTo(alerts)
    Assertions.assertThat(prisonerProfile.visitBalances).isEqualTo(visitBalancesDto)
  }

/*  @Test
  fun `when active prisons do not exist then empty list is returned`() {
    // Given
    visitSchedulerMockServer.stubGetSupportedPrisons(mutableListOf())

    // When
    val responseSpec = callGetSupportedPrisons(webTestClient, roleVisitSchedulerHttpHeaders)

    // Then
    val results = responseSpec.expectStatus().isOk
      .expectBody()
  }*/

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

  private fun createPrisonerBookingSummary(offenderNo: String): PrisonerBookingSummaryDto {
    return PrisonerBookingSummaryDto(offenderNo, "Convicted")
  }
}
