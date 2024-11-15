package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.visit

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.VisitDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.ContactDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase

@DisplayName("Get visits by reference")
class VisitByReferenceTest : IntegrationTestBase() {
  fun callVisitByReference(
    webTestClient: WebTestClient,
    reference: String,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    return webTestClient.get().uri("/visits/$reference")
      .headers(authHttpHeaders)
      .exchange()
  }

  @Test
  fun `when visit exists search by reference returns that visit`() {
    // Given
    val reference = "aa-bb-cc-dd"
    val visitDto = createVisitDto(reference = reference)
    visitSchedulerMockServer.stubGetVisit(reference, visitDto)

    // When
    val responseSpec = callVisitByReference(webTestClient, reference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val visitDtoResponse = objectMapper.readValue(responseSpec.expectBody().returnResult().responseBody, VisitDetailsDto::class.java)
    Assertions.assertThat(visitDtoResponse.reference).isEqualTo(visitDto.reference)
  }

  @Test
  fun `when visit exists without a phone number or email for contact search by reference still returns that visit`() {
    // Given
    val reference = "aa-bb-cc-dd"
    val visitDto = createVisitDto(reference = reference, contact = ContactDto("Jane Doe", null, null))
    visitSchedulerMockServer.stubGetVisit(reference, visitDto)

    // When
    val responseSpec = callVisitByReference(webTestClient, reference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val visitDtoResponse = objectMapper.readValue(responseSpec.expectBody().returnResult().responseBody, VisitDetailsDto::class.java)
    Assertions.assertThat(visitDtoResponse.reference).isEqualTo(visitDto.reference)
    Assertions.assertThat(visitDtoResponse.visitContact!!.telephone).isNull()
    Assertions.assertThat(visitDtoResponse.visitContact!!.email).isNull()
    Assertions.assertThat(visitDtoResponse.visitContact!!.name).isEqualTo("Jane Doe")
  }

  @Test
  fun `when visit does not exist search by reference returns NOT_FOUND status`() {
    // Given
    val reference = "xx-yy-cc-dd"
    visitSchedulerMockServer.stubGetVisit(reference, null)

    // When
    val responseSpec = callVisitByReference(webTestClient, reference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
  }
}
