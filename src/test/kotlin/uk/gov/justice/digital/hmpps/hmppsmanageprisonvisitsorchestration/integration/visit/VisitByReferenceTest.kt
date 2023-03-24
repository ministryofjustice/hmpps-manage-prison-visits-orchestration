package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.visit

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitDto
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
    val createdBy = "created-user"
    val lastUpdatedBy = "updated-user"
    val visitDto = createVisitDto(reference = reference, createdBy = createdBy, updatedBy = lastUpdatedBy, cancelledBy = null)
    visitSchedulerMockServer.stubGetVisit(reference, visitDto)

    // When
    val responseSpec = callVisitByReference(webTestClient, reference, roleVisitSchedulerHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val visitDtoResponse = objectMapper.readValue(responseSpec.expectBody().returnResult().responseBody, VisitDto::class.java)
    Assertions.assertThat(visitDtoResponse.reference).isEqualTo(visitDto.reference)
    Assertions.assertThat(visitDtoResponse.createdBy).isEqualTo(createdBy)
    Assertions.assertThat(visitDtoResponse.createdByFullName).isNull()
    Assertions.assertThat(visitDtoResponse.updatedBy).isEqualTo(lastUpdatedBy)
    Assertions.assertThat(visitDtoResponse.updatedByFullName).isNull()
    Assertions.assertThat(visitDtoResponse.cancelledBy).isNull()
    Assertions.assertThat(visitDtoResponse.cancelledByFullName).isNull()
  }

  @Test
  fun `when visit does not exist search by reference returns NOT_FOUND status`() {
    // Given
    val reference = "xx-yy-cc-dd"
    visitSchedulerMockServer.stubGetVisit(reference, null)

    // When
    val responseSpec = callVisitByReference(webTestClient, reference, roleVisitSchedulerHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
  }
}
