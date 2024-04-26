package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.visit

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.application.ChangeApplicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase

@DisplayName("Change a reserved Visit Slot")
class ChangeReservedVisitSlotTest : IntegrationTestBase() {

  fun callChangeReservedVisitSlot(
    webTestClient: WebTestClient,
    applicationReference: String,
    changeApplicationDto: ChangeApplicationDto,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    return webTestClient.put().uri("/visits/application/$applicationReference/slot/change")
      .headers(authHttpHeaders)
      .body(BodyInserters.fromValue(changeApplicationDto))
      .exchange()
  }

  @Test
  fun `when change a reserved visit slot is successful then OK status is returned`() {
    // Given
    val applicationReference = "aaa-bbb-ccc-ddd"
    val visitReference = "aa-bb-cc-dd"
    val changeApplicationDto = createChangeApplicationDto()
    val applicationDto = createApplicationDto(reference = applicationReference)
    visitSchedulerMockServer.stubChangeReservedVisitSlot(visitReference, applicationDto)

    // When
    val responseSpec = callChangeReservedVisitSlot(webTestClient, visitReference, changeApplicationDto, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
      .expectBody()
      .jsonPath("$.reference").isEqualTo(applicationReference)
  }

  @Test
  fun `when change a reserve visit slot is unsuccessful then an error status is returned`() {
    // Given
    val changeApplicationDto = createChangeApplicationDto()
    val visitReference = "aaa-bbb-ccc-ddd"
    val applicationDto = null
    visitSchedulerMockServer.stubChangeReservedVisitSlot(visitReference, applicationDto)

    // When
    val responseSpec = callChangeReservedVisitSlot(webTestClient, visitReference, changeApplicationDto, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
  }
}
