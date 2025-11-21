package uk.gov.justice.digital.hmpps.visits.orchestration.integration.visit

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.orchestration.CancelVisitOrchestrationDto
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.OutcomeDto
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.enums.ApplicationMethodType.PHONE
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.enums.OutcomeStatus.CANCELLATION
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.enums.UserType
import uk.gov.justice.digital.hmpps.visits.orchestration.integration.IntegrationTestBase

@DisplayName("Cancel a visit")
class CancelVisitTest : IntegrationTestBase() {
  fun callCancelVisit(
    webTestClient: WebTestClient,
    reference: String,
    cancelVisitOrchestrationDto: CancelVisitOrchestrationDto,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec = webTestClient.put().uri("/visits/$reference/cancel")
    .headers(authHttpHeaders)
    .body(BodyInserters.fromValue(cancelVisitOrchestrationDto))
    .exchange()

  @Test
  fun `when cancel visit is successful then OK status is returned`() {
    // Given
    val reference = "aa-bb-cc-dd"
    val username = "A_USER"
    val cancelVisitOrchestrationDto = CancelVisitOrchestrationDto(OutcomeDto(CANCELLATION), PHONE, username, UserType.STAFF)
    val visitDto = createVisitDto(reference = reference)
    visitSchedulerMockServer.stubCancelVisit(reference, visitDto)

    // When
    val responseSpec = callCancelVisit(webTestClient, reference, cancelVisitOrchestrationDto, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
      .expectBody()
      .jsonPath("$.reference").isEqualTo(reference)
  }

  @Test
  fun `when cancel visit is unsuccessful then NOT_FOUND status is returned`() {
    // Given
    val reference = "aa-bb-cc-dd"
    val username = "A_USER"
    val cancelVisitOrchestrationDto = CancelVisitOrchestrationDto(OutcomeDto(CANCELLATION), PHONE, username, UserType.STAFF)
    val visitDto = null
    visitSchedulerMockServer.stubCancelVisit(reference, visitDto)

    // When
    val responseSpec = callCancelVisit(webTestClient, reference, cancelVisitOrchestrationDto, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
  }
}
