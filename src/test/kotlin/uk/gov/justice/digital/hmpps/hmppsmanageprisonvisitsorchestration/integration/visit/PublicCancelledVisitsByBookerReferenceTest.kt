package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.visit

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitStatus.CANCELLED
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import java.time.LocalDateTime

@DisplayName("Get public cancelled visits by booker reference")
class PublicCancelledVisitsByBookerReferenceTest : IntegrationTestBase() {
  @Test
  fun `when cancelled visits for booker exists then get cancelled visits returns content`() {
    // Given
    val visitDto = createVisitDto(reference = "ss-bb", startTimestamp = LocalDateTime.now().plusDays(1), endTimestamp = LocalDateTime.now().plusDays(1), visitStatus = CANCELLED)
    val visitDto2 = createVisitDto(reference = "xx-bb", startTimestamp = LocalDateTime.now().plusDays(2), endTimestamp = LocalDateTime.now().plusDays(2), visitStatus = CANCELLED)
    val prisonerId = "ABC"
    val visitsList = mutableListOf(visitDto, visitDto2)
    visitSchedulerMockServer.stubPublicCancelledVisitsByBookerReference(prisonerId, visitsList)

    // When
    val responseSpec = callPublicCancelledVisits(webTestClient, prisonerId, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk
      .expectBody()

    val visits = getResults(returnResult)
    Assertions.assertThat(visits.size).isEqualTo(2)
  }

  @Test
  fun `when cancelled visits for booker do not exists then get cancelled visits returns empty content`() {
    // Given
    val prisonerId = "AABBCC"
    val emptyList = mutableListOf<VisitDto>()
    visitSchedulerMockServer.stubPublicCancelledVisitsByBookerReference(prisonerId, emptyList)

    // When
    val responseSpec = callPublicCancelledVisits(webTestClient, prisonerId, roleVSIPOrchestrationServiceHttpHeaders)

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
