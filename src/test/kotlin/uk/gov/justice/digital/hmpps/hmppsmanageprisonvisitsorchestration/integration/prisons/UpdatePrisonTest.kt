package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.prisons

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSchedulerPrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSchedulerUpdatePrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.TestObjectMapper
import java.time.DayOfWeek

@DisplayName("Update prison tests")
class UpdatePrisonTest : IntegrationTestBase() {

  final val prisonCode = "HEI"

  @Test
  fun `when prison is updated then successfully updated prison is returned`() {
    // Given
    val updatePrisonDto = VisitSchedulerUpdatePrisonDto(DayOfWeek.MONDAY, 3)
    val responsePrisonDto = VisitSchedulerPrisonDto(prisonCode, true, 2, 28, 6, 3, 3, 18, DayOfWeek.MONDAY, 3)

    visitSchedulerMockServer.stubPutUpdatePrison("HEI", updatePrisonDto, responsePrisonDto)

    // When
    val responseSpec = callPutUpdatePrison(webTestClient, "HEI", updatePrisonDto, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val result = getResult(returnResult)

    Assertions.assertThat(result.active).isEqualTo(responsePrisonDto.active)
    Assertions.assertThat(result.code).isEqualTo(responsePrisonDto.code)
    Assertions.assertThat(result.policyNoticeDaysMin).isEqualTo(responsePrisonDto.policyNoticeDaysMin)
    Assertions.assertThat(result.policyNoticeDaysMax).isEqualTo(responsePrisonDto.policyNoticeDaysMax)
    Assertions.assertThat(result.maxAdultVisitors).isEqualTo(responsePrisonDto.maxAdultVisitors)
    Assertions.assertThat(result.maxChildVisitors).isEqualTo(responsePrisonDto.maxChildVisitors)
    Assertions.assertThat(result.maxTotalVisitors).isEqualTo(responsePrisonDto.maxTotalVisitors)
    Assertions.assertThat(result.adultAgeYears).isEqualTo(responsePrisonDto.adultAgeYears)
    Assertions.assertThat(result.weekStartDay).isEqualTo(responsePrisonDto.weekStartDay)
    Assertions.assertThat(result.remandVisitLimitPerWeek).isEqualTo(responsePrisonDto.remandVisitLimitPerWeek)

    verify(visitSchedulerClientSpy, times(1)).updatePrison(prisonCode, updatePrisonDto)
  }

  @Test
  fun `when BAD_REQUEST is returned from visit scheduler then BAD_REQUEST status is sent back`() {
    // Given
    val updatePrisonDto = VisitSchedulerUpdatePrisonDto(DayOfWeek.MONDAY, 3)

    visitSchedulerMockServer.stubPutUpdatePrison("HEI", updatePrisonDto, null, HttpStatus.BAD_REQUEST)

    // When
    val responseSpec = callPutUpdatePrison(webTestClient, "HEI", updatePrisonDto, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isBadRequest
    verify(visitSchedulerClientSpy, times(1)).updatePrison(prisonCode, updatePrisonDto)
  }

  private fun getResult(returnResult: WebTestClient.BodyContentSpec): VisitSchedulerPrisonDto = TestObjectMapper.mapper.readValue(returnResult.returnResult().responseBody, VisitSchedulerPrisonDto::class.java)

  private fun callPutUpdatePrison(
    webTestClient: WebTestClient,
    prisonCode: String,
    updatePrisonDto: VisitSchedulerUpdatePrisonDto,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec = webTestClient.put().uri("/config/prisons/prison/$prisonCode")
    .bodyValue(updatePrisonDto)
    .headers(authHttpHeaders)
    .exchange()
}
