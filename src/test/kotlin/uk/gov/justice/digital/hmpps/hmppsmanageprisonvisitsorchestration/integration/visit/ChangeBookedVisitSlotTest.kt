package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.visit

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.application.CreateApplicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase

@DisplayName("Change a booked Visit Slot tests")
class ChangeBookedVisitSlotTest : IntegrationTestBase() {
  fun callChangeBookedVisitSlot(
    webTestClient: WebTestClient,
    bookingReference: String,
    createApplicationDto: CreateApplicationDto,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    return webTestClient.put().uri("/visits/application/$bookingReference/change")
      .headers(authHttpHeaders)
      .body(BodyInserters.fromValue(createApplicationDto))
      .exchange()
  }

  @Test
  fun `when change a booked slot is successful then OK status is returned`() {
    // Given
    val prisonerId = "A123567B"
    val reference = "aa-bb-cc-dd"
    val createApplicationDto = createCreateApplicationDto(prisonerId)
    val applicationDto = createApplicationDto(reference = reference, prisonerId = prisonerId)
    visitSchedulerMockServer.stubChangeBookedVisit(reference, applicationDto)

    // When
    val responseSpec = callChangeBookedVisitSlot(webTestClient, reference, createApplicationDto, roleVisitSchedulerHttpHeaders)

    // Then
    responseSpec.expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reference").isEqualTo(reference)
  }

  @Test
  fun `when change a booked slot is unsuccessful then an error status is returned`() {
    // Given
    val prisonerId = "A123567B"
    val createApplicationDto = createCreateApplicationDto(prisonerId)
    val reference = "aa-bb-cc-dd"
    val applicationDto = null
    visitSchedulerMockServer.stubChangeBookedVisit(reference, applicationDto)

    // When
    val responseSpec = callChangeBookedVisitSlot(webTestClient, reference, createApplicationDto, roleVisitSchedulerHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
  }
}
