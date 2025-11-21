package uk.gov.justice.digital.hmpps.visits.orchestration.integration.visit

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.application.CreateApplicationDto
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.enums.ApplicationStatus.ACCEPTED
import uk.gov.justice.digital.hmpps.visits.orchestration.integration.IntegrationTestBase

@DisplayName("Create reserve application tests")
class CreateInitialReservedApplicationTest : IntegrationTestBase() {
  fun createInitialApplication(
    webTestClient: WebTestClient,
    createApplicationDto: CreateApplicationDto,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec = webTestClient.post().uri("/visits/application/slot/reserve")
    .headers(authHttpHeaders)
    .body(BodyInserters.fromValue(createApplicationDto))
    .exchange()

  @Test
  fun `when reserve visit slot is successful then CREATED status is returned`() {
    // Given
    val prisonerId = "A123567B"
    val referenceCreated = "aa-bb-cc-dd"
    val createApplicationDto = createCreateApplicationDto(prisonerId)
    val applicationDto = createApplicationDto(reference = referenceCreated, applicationStatus = ACCEPTED)
    visitSchedulerMockServer.stubCreateApplication(applicationDto)

    // When
    val responseSpec = createInitialApplication(webTestClient, createApplicationDto, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reference").isEqualTo(referenceCreated)
  }

  @Test
  fun `when reserve visit slot is unsuccessful then an error status is returned`() {
    // Given
    val prisonerId = "A123567B"
    val reserveVisitSlotDto = createCreateApplicationDto(prisonerId)
    val applicationDto = null
    visitSchedulerMockServer.stubCreateApplication(applicationDto)

    // When
    val responseSpec = createInitialApplication(webTestClient, reserveVisitSlotDto, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isBadRequest
  }
}
