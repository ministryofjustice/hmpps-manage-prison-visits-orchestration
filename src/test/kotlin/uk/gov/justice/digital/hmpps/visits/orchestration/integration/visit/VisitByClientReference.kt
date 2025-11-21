package uk.gov.justice.digital.hmpps.visits.orchestration.integration.visit

import com.fasterxml.jackson.databind.type.TypeFactory
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.visits.orchestration.integration.IntegrationTestBase

class VisitByClientReference : IntegrationTestBase() {
  fun callVisitByClientReference(
    webTestClient: WebTestClient,
    clientReference: String,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec = webTestClient.get().uri("/visits/external-system/$clientReference")
    .headers(authHttpHeaders)
    .exchange()

  @Test
  fun `when a visit exists for a client reference get the visit reference`() {
    // Given
    val reference = "aa-bb-cc-dd"
    val clientReference = "ABC123"
    val visitReference = listOf(reference)
    visitSchedulerMockServer.stubGetVisitByClientRef(clientReference, visitReference)

    // When
    val responseSpec = callVisitByClientReference(webTestClient, clientReference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val responseBody = responseSpec.expectBody().returnResult().responseBody
    val mappedResponse: List<String> = objectMapper.readValue(
      responseBody,
      TypeFactory.defaultInstance().constructCollectionType(
        MutableList::class.java,
        String::class.java,
      ),
    )
    Assertions.assertThat(mappedResponse).isEqualTo(visitReference)
  }

  @Test
  fun `when multiple visits exists for a client reference get the visit reference`() {
    // Given
    val reference = "aa-bb-cc-dd"
    val reference1 = "zz-bb-cc-dd"
    val clientReference = "ABC123"
    val visitReference = listOf(reference, reference1)
    visitSchedulerMockServer.stubGetVisitByClientRef(clientReference, visitReference)

    // When
    val responseSpec = callVisitByClientReference(webTestClient, clientReference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val responseBody = responseSpec.expectBody().returnResult().responseBody
    val mappedResponse: List<String> = objectMapper.readValue(
      responseBody,
      TypeFactory.defaultInstance().constructCollectionType(
        MutableList::class.java,
        String::class.java,
      ),
    )
    Assertions.assertThat(mappedResponse).isEqualTo(visitReference)
  }

  @Test
  fun `when a visit does not exist for client reference`() {
    // Given
    val clientReference = "ABC123"
    visitSchedulerMockServer.stubGetVisitByClientRef(clientReference, null)

    // When
    val responseSpec = callVisitByClientReference(webTestClient, clientReference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
  }
}
