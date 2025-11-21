package uk.gov.justice.digital.hmpps.visits.orchestration.integration.visit

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.application.ChangeApplicationDto
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.enums.ApplicationStatus.ACCEPTED
import uk.gov.justice.digital.hmpps.visits.orchestration.integration.IntegrationTestBase

@DisplayName("Change a reserved application")
class ChangeReservedApplicationTest : IntegrationTestBase() {

  fun callChangeReservedApplicationSlot(
    webTestClient: WebTestClient,
    applicationReference: String,
    changeApplicationDto: ChangeApplicationDto,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec = webTestClient.put().uri("/visits/application/$applicationReference/slot/change")
    .headers(authHttpHeaders)
    .body(BodyInserters.fromValue(changeApplicationDto))
    .exchange()

  @Test
  fun `when change a reserved application slot is successful then OK status is returned`() {
    // Given
    val applicationReference = "aaa-bbb-ccc-ddd"
    val visitReference = "aa-bb-cc-dd"
    val changeApplicationDto = createChangeApplicationDto()
    val applicationDto = createApplicationDto(reference = applicationReference, applicationStatus = ACCEPTED)
    visitSchedulerMockServer.stubChangeReservedVisitSlot(visitReference, applicationDto)

    // When
    val responseSpec = callChangeReservedApplicationSlot(webTestClient, visitReference, changeApplicationDto, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
      .expectBody()
      .jsonPath("$.reference").isEqualTo(applicationReference)
  }

  @Test
  fun `when change a reserve application slot is unsuccessful then an error status is returned`() {
    // Given
    val changeApplicationDto = createChangeApplicationDto()
    val visitReference = "aaa-bbb-ccc-ddd"
    val applicationDto = null
    visitSchedulerMockServer.stubChangeReservedVisitSlot(visitReference, applicationDto)

    // When
    val responseSpec = callChangeReservedApplicationSlot(webTestClient, visitReference, changeApplicationDto, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
  }
}
