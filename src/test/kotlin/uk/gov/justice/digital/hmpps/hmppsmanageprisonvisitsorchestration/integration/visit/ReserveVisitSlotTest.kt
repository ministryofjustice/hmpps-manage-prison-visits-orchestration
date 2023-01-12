package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.visit

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.ReserveVisitSlotDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase

@DisplayName("Reserve Visit Slot tests")
class ReserveVisitSlotTest : IntegrationTestBase() {
  fun callVisitReserveSlot(
    webTestClient: WebTestClient,
    reserveVisitSlotDto: ReserveVisitSlotDto,
    authHttpHeaders: (HttpHeaders) -> Unit
  ): WebTestClient.ResponseSpec {

    return webTestClient.post().uri("/visits/slot/reserve")
      .headers(authHttpHeaders)
      .body(BodyInserters.fromValue(reserveVisitSlotDto))
      .exchange()
  }

  @Test
  fun `when reserve visit slot is successful then CREATED status is returned`() {
    // Given
    val prisonerId = "A123567B"
    val referenceCreated = "aa-bb-cc-dd"
    val reserveVisitSlotDto = createReserveVisitSlotDto(prisonerId)
    val visitDto = createVisitDto(reference = referenceCreated)
    visitSchedulerMockServer.stubReserveVisitSlot(visitDto)

    // When
    val responseSpec = callVisitReserveSlot(webTestClient, reserveVisitSlotDto, roleVisitSchedulerHttpHeaders)

    // Then
    responseSpec.expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reference").isEqualTo(referenceCreated)
  }

  @Test
  fun `when reserve visit slot is unsuccessful then an error status is returned`() {
    // Given
    val prisonerId = "A123567B"
    val reserveVisitSlotDto = createReserveVisitSlotDto(prisonerId)
    val visitDto = null
    visitSchedulerMockServer.stubReserveVisitSlot(visitDto)

    // When
    val responseSpec = callVisitReserveSlot(webTestClient, reserveVisitSlotDto, roleVisitSchedulerHttpHeaders)

    // Then
    responseSpec.expectStatus().isBadRequest
  }
}
