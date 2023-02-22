package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.visit

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.ReserveVisitSlotDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase

@DisplayName("Change a booked Visit Slot tests")
class ChangeBookedVisitSlotTest : IntegrationTestBase() {
  fun callChangeBookedVisitSlot(
    webTestClient: WebTestClient,
    reference: String,
    reserveVisitSlotDto: ReserveVisitSlotDto,
    authHttpHeaders: (HttpHeaders) -> Unit
  ): WebTestClient.ResponseSpec {

    return webTestClient.put().uri("/visits/$reference/change")
      .headers(authHttpHeaders)
      .body(BodyInserters.fromValue(reserveVisitSlotDto))
      .exchange()
  }

  @Test
  fun `when change a booked slot is successful then OK status is returned`() {
    // Given
    val prisonerId = "A123567B"
    val reference = "aa-bb-cc-dd"
    val reserveVisitSlotDto = createReserveVisitSlotDto(prisonerId)
    val visitDto = createVisitDto(reference = reference)
    visitSchedulerMockServer.stubChangeBookedVisit(reference, visitDto)

    // When
    val responseSpec = callChangeBookedVisitSlot(webTestClient, reference, reserveVisitSlotDto, roleVisitSchedulerHttpHeaders)

    // Then
    responseSpec.expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reference").isEqualTo(reference)
  }

  @Test
  fun `when change a booked slot is unsuccessful then an error status is returned`() {
    // Given
    val prisonerId = "A123567B"
    val reserveVisitSlotDto = createReserveVisitSlotDto(prisonerId)
    val reference = "aa-bb-cc-dd"
    val visitDto = null
    visitSchedulerMockServer.stubChangeBookedVisit(reference, visitDto)

    // When
    val responseSpec = callChangeBookedVisitSlot(webTestClient, reference, reserveVisitSlotDto, roleVisitSchedulerHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
  }
}
