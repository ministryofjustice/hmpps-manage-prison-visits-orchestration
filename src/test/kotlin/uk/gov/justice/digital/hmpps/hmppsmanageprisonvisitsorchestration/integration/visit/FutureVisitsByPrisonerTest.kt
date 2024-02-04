package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.visit

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import java.time.LocalDateTime

@DisplayName("Get multiple visits by filter")
class FutureVisitsByPrisonerTest : IntegrationTestBase() {
  @Test
  fun `when future visits for prisoner exists then get future visits returns content`() {
    // Given
    val visitDto = createVisitDto(reference = "ss-bb", startTimestamp = LocalDateTime.now().plusDays(1), endTimestamp = LocalDateTime.now().plusDays(1))
    val visitDto2 = createVisitDto(reference = "xx-bb", startTimestamp = LocalDateTime.now().plusDays(2), endTimestamp = LocalDateTime.now().plusDays(2))
    val prisonerId = "ABC"
    val visitsList = mutableListOf(visitDto, visitDto2)
    visitSchedulerMockServer.stubGetFutureVisits(prisonerId, visitsList)

    // When
    val responseSpec = callFutureVisits(webTestClient, prisonerId, roleVisitSchedulerHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk
      .expectBody()

    val visits = getResults(returnResult)
    Assertions.assertThat(visits.size).isEqualTo(2)
  }

  @Test
  fun `when visits for parameters do not exist then get visits by filter returns empty content`() {
    // Given
    val prisonerId = "AABBCC"
    val emptyList = mutableListOf<VisitDto>()
    visitSchedulerMockServer.stubGetFutureVisits(prisonerId, emptyList)

    // When
    val responseSpec = callFutureVisits(webTestClient, prisonerId, roleVisitSchedulerHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk
      .expectBody()
    val visits = getResults(returnResult)
    Assertions.assertThat(visits.size).isEqualTo(0)
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): Array<VisitDto> {
    return objectMapper.readValue(returnResult.returnResult().responseBody, Array<VisitDto>::class.java)
  }
}
