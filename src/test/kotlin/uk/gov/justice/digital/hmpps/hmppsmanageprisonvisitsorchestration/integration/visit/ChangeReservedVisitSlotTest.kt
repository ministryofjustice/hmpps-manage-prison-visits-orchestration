package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.visit

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.ChangeVisitSlotRequestDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase

@DisplayName("Change a reserved Visit Slot")
class ChangeReservedVisitSlotTest : IntegrationTestBase() {
  fun callChangeReservedVisitSlot(
    webTestClient: WebTestClient,
    applicationReference: String,
    changeVisitSlotRequestDto: ChangeVisitSlotRequestDto,
    authHttpHeaders: (HttpHeaders) -> Unit
  ): WebTestClient.ResponseSpec {

    return webTestClient.put().uri("/visits/$applicationReference/slot/change")
      .headers(authHttpHeaders)
      .body(BodyInserters.fromValue(changeVisitSlotRequestDto))
      .exchange()
  }

  @Test
  fun `when change a reserved visit slot is successful then OK status is returned`() {
    // Given
    val applicationReference = "aaa-bbb-ccc-ddd"
    val reference = "aa-bb-cc-dd"
    val changeVisitSlotDto = createChangeVisitSlotRequestDto()
    val visitDto = createVisitDto(reference = reference, applicationReference = applicationReference)
    visitSchedulerMockServer.stubChangeReservedVisitSlot(applicationReference, visitDto)

    // When
    val responseSpec = callChangeReservedVisitSlot(webTestClient, applicationReference, changeVisitSlotDto, roleVisitSchedulerHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
      .expectBody()
      .jsonPath("$.reference").isEqualTo(reference)
  }

  @Test
  fun `when change a reserve visit slot is unsuccessful then an error status is returned`() {
    // Given
    val reserveVisitSlotDto = createChangeVisitSlotRequestDto()
    val applicationReference = "aaa-bbb-ccc-ddd"
    val visitDto = null
    visitSchedulerMockServer.stubChangeReservedVisitSlot(applicationReference, visitDto)

    // When
    val responseSpec = callChangeReservedVisitSlot(webTestClient, applicationReference, reserveVisitSlotDto, roleVisitSchedulerHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
  }
}
