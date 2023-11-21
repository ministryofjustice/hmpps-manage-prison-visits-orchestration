package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.visit

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.PrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import java.time.LocalDate

@DisplayName("Get supported prisons")
class GetPrisonTest : IntegrationTestBase() {
  fun callGetSupportedPrisons(
    webTestClient: WebTestClient,
    prisonCode: String,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    return webTestClient.get().uri("/config/prisons/prison/$prisonCode")
      .headers(authHttpHeaders)
      .exchange()
  }

  @Test
  fun `when active prisons exist then all active prisons are returned`() {
    // Given
    val prisonDto = PrisonDto("HEI", true, 2, 28, setOf(LocalDate.now()))
    visitSchedulerMockServer.stubGetPrison("HEI", prisonDto = prisonDto)

    // When
    val responseSpec = callGetSupportedPrisons(webTestClient, "HEI", roleVisitSchedulerHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val result = getResult(returnResult)

    Assertions.assertThat(result.active).isEqualTo(prisonDto.active)
    Assertions.assertThat(result.code).isEqualTo(prisonDto.code)
    Assertions.assertThat(result.excludeDates).hasSize(prisonDto.excludeDates.size)
    Assertions.assertThat(result.policyNoticeDaysMin).isEqualTo(prisonDto.policyNoticeDaysMin)
    Assertions.assertThat(result.policyNoticeDaysMax).isEqualTo(prisonDto.policyNoticeDaysMax)
  }

  private fun getResult(returnResult: WebTestClient.BodyContentSpec): PrisonDto {
    return objectMapper.readValue(returnResult.returnResult().responseBody, PrisonDto::class.java)
  }
}
