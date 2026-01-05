package uk.gov.justice.digital.hmpps.prison.visits.orchestration.integration.sessions

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.prison.api.OffenderRestrictionDto
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.prison.api.OffenderRestrictionsDto
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.visit.scheduler.AvailableVisitSessionRestrictionDto
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.visit.scheduler.enums.SessionRestriction
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.service.PrisonerProfileService
import java.time.LocalDate

@DisplayName("Get available visit session restriction")
class AvailableVisitSessionsRestrictionTest : IntegrationTestBase() {

  @MockitoSpyBean
  private lateinit var prisonerProfileService: PrisonerProfileService

  @Test
  fun `when get available session restriction called and no restriction for prisoner or visitors, OPEN is returned`() {
    // Given
    val prisonerId = "AA123456B"

    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, OffenderRestrictionsDto(offenderRestrictions = emptyList()))

    val visitorIds = listOf(1L, 2L, 3L)
    prisonerContactRegistryMockServer.stubDoVisitorsHaveClosedRestrictions(prisonerId, visitorIds = visitorIds, result = false)

    // When
    val responseSpec = callGetAvailableVisitSessionsRestriction(webTestClient, prisonerId, visitorIds = visitorIds, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val result = responseSpec.expectStatus().isOk.expectBody()
    val sessionRestrictionDto = getResults(result)
    Assertions.assertThat(sessionRestrictionDto.sessionRestriction).isEqualTo(SessionRestriction.OPEN)

    verify(prisonerProfileService, times(1)).hasPrisonerGotClosedRestrictions(prisonerId)
    verify(prisonerProfileService, times(1)).hasVisitorsGotClosedRestrictions(prisonerId, visitorIds)
  }

  @Test
  fun `when get available session restriction called and prisoner has restrictions but visitors don't, CLOSED is returned`() {
    // Given
    val prisonerId = "AA123456B"

    val offenderRestrictionsDto = OffenderRestrictionsDto(
      bookingId = 1,
      offenderRestrictions = listOf(
        OffenderRestrictionDto(restrictionId = 1, restrictionType = "CLOSED", restrictionTypeDescription = "", startDate = LocalDate.now(), expiryDate = null, active = true),
      ),
    )
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, offenderRestrictionsDto)

    val visitorIds = listOf(1L, 2L, 3L)

    // When
    val responseSpec = callGetAvailableVisitSessionsRestriction(webTestClient, prisonerId, visitorIds = visitorIds, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val result = responseSpec.expectStatus().isOk.expectBody()
    val sessionRestrictionDto = getResults(result)
    Assertions.assertThat(sessionRestrictionDto.sessionRestriction).isEqualTo(SessionRestriction.CLOSED)

    verify(prisonerProfileService, times(1)).hasPrisonerGotClosedRestrictions(prisonerId)
  }

  @Test
  fun `when get available session restriction called and prisoner has no restrictions but visitors do, CLOSED is returned`() {
    // Given
    val prisonerId = "AA123456B"

    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, OffenderRestrictionsDto(offenderRestrictions = emptyList()))

    val visitorIds = listOf(1L, 2L, 3L)
    prisonerContactRegistryMockServer.stubDoVisitorsHaveClosedRestrictions(prisonerId, visitorIds = visitorIds, result = true)

    // When
    val responseSpec = callGetAvailableVisitSessionsRestriction(webTestClient, prisonerId, visitorIds = visitorIds, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val result = responseSpec.expectStatus().isOk.expectBody()
    val sessionRestrictionDto = getResults(result)
    Assertions.assertThat(sessionRestrictionDto.sessionRestriction).isEqualTo(SessionRestriction.CLOSED)

    verify(prisonerProfileService, times(1)).hasPrisonerGotClosedRestrictions(prisonerId)
    verify(prisonerProfileService, times(1)).hasVisitorsGotClosedRestrictions(prisonerId, visitorIds)
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): AvailableVisitSessionRestrictionDto = objectMapper.readValue(returnResult.returnResult().responseBody, AvailableVisitSessionRestrictionDto::class.java)
}
