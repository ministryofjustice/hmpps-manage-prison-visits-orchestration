package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.visit

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.application.CreateApplicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase

@DisplayName("Reserve Visit Slot tests")
class CreateInitialApplicationTest : IntegrationTestBase() {
  fun createInitialApplication(
    webTestClient: WebTestClient,
    createApplicationDto: CreateApplicationDto,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    return webTestClient.post().uri("/visits/application/slot/reserve")
      .headers(authHttpHeaders)
      .body(BodyInserters.fromValue(createApplicationDto))
      .exchange()
  }

  @Test
  fun `when reserve visit slot is successful then CREATED status is returned`() {
    // Given
    val prisonerId = "A123567B"
    val referenceCreated = "aa-bb-cc-dd"
    val createApplicationDto = createCreateApplicationDto(prisonerId)
    val applicationDto = createApplicationDto(reference = referenceCreated)
    visitSchedulerMockServer.stubCreateApplication(applicationDto)

    // When
    val responseSpec = createInitialApplication(webTestClient, createApplicationDto, roleVisitSchedulerHttpHeaders)

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
    val responseSpec = createInitialApplication(webTestClient, reserveVisitSlotDto, roleVisitSchedulerHttpHeaders)

    // Then
    responseSpec.expectStatus().isBadRequest
  }
}
