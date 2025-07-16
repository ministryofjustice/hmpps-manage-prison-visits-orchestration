package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.visit

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller.VISIT_REQUESTS_APPROVE_VISIT_BY_REFERENCE_PATH
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.OrchestrationApproveVisitRequestResponseDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.ApproveVisitRequestResponseDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase

@DisplayName("GET $VISIT_REQUESTS_APPROVE_VISIT_BY_REFERENCE_PATH")
class ApproveVisitRequestByReferenceTest : IntegrationTestBase() {

  @BeforeEach
  fun resetStubs() {
    visitSchedulerMockServer.resetAll()
  }

  val prisonCode = "ABC"

  @Test
  fun `when approve visit request is called then success response is returned`() {
    // Given
    val approveVisitResponse = ApproveVisitRequestResponseDto(
      visitReference = "ab-cd-ef-gh",
      prisonerFirstName = "Prisoner",
      prisonerLastName = "Name",
    )

    visitSchedulerMockServer.stubApproveVisitRequestByReference("ab-cd-ef-gh", approveVisitResponse)

    // When
    val responseSpec = callApproveVisitRequestByReference(webTestClient, "ab-cd-ef-gh", roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val approvalResponse = getApproveVisitRequestByReferenceResult(responseSpec)
    Assertions.assertThat(approvalResponse.visitReference).isEqualTo("ab-cd-ef-gh")
    Assertions.assertThat(approvalResponse.prisonerFirstName).isEqualTo("Prisoner")
    Assertions.assertThat(approvalResponse.prisonerLastName).isEqualTo("Name")
  }

  @Test
  fun `when no role specified then access forbidden status is returned`() {
    // Given
    val authHttpHeaders = setAuthorisation(roles = listOf())

    // When
    val responseSpec = callApproveVisitRequestByReference(webTestClient, "ab-cd-ef-gh", authHttpHeaders)

    // Then
    responseSpec.expectStatus().isForbidden
  }

  @Test
  fun `when no token passed then unauthorized status is returned`() {
    // Given

    // When
    val responseSpec = webTestClient.get().uri(VISIT_REQUESTS_APPROVE_VISIT_BY_REFERENCE_PATH.replace("{reference}", "ab-cd-ef-gh")).exchange()

    // Then
    responseSpec.expectStatus().isUnauthorized
  }

  fun getApproveVisitRequestByReferenceResult(responseSpec: ResponseSpec): OrchestrationApproveVisitRequestResponseDto = objectMapper.readValue(responseSpec.expectBody().returnResult().responseBody, OrchestrationApproveVisitRequestResponseDto::class.java)

  fun callApproveVisitRequestByReference(
    webTestClient: WebTestClient,
    visitReference: String,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): ResponseSpec {
    val url = VISIT_REQUESTS_APPROVE_VISIT_BY_REFERENCE_PATH.replace("{reference}", visitReference)

    return webTestClient.put().uri(url)
      .headers(authHttpHeaders)
      .exchange()
  }
}
