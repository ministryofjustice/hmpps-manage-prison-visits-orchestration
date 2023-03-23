package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.visit

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitDetailsClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase

@DisplayName("Get visits by reference")
class VisitDetailsTest : IntegrationTestBase() {
  fun callVisitDetailsByReference(
    webTestClient: WebTestClient,
    reference: String,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    return webTestClient.get().uri("/visits/full-details/$reference")
      .headers(authHttpHeaders)
      .exchange()
  }

  @Test
  fun `when visit exists the full details search by reference returns the visit and usernames`() {
    // Given
    val reference = "aa-bb-cc-dd"
    val createdBy = "created-user"
    val lastUpdatedBy = "updated-user"
    val cancelledBy = "cancelled-user"
    val visitDto = createVisitDto(reference = reference, createdBy = createdBy, updatedBy = lastUpdatedBy, cancelledBy = cancelledBy)
    visitSchedulerMockServer.stubGetVisit(reference, visitDto)

    // When
    val responseSpec = callVisitDetailsByReference(webTestClient, reference, roleVisitSchedulerHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val visitDtoResponse = objectMapper.readValue(responseSpec.expectBody().returnResult().responseBody, VisitDto::class.java)
    Assertions.assertThat(visitDtoResponse.reference).isEqualTo(visitDto.reference)
    Assertions.assertThat(visitDtoResponse.createdBy).isEqualTo(createdBy)
    Assertions.assertThat(visitDtoResponse.createdByFullName).isEqualTo("$createdBy-name")
    Assertions.assertThat(visitDtoResponse.updatedBy).isEqualTo(lastUpdatedBy)
    Assertions.assertThat(visitDtoResponse.updatedByFullName).isEqualTo("$lastUpdatedBy-name")
    Assertions.assertThat(visitDtoResponse.cancelledBy).isEqualTo(cancelledBy)
    Assertions.assertThat(visitDtoResponse.cancelledByFullName).isEqualTo("$cancelledBy-name")
  }

  @Test
  fun `when visit exists but userid is NOT_KNOWN search by reference returns the visit and names as NOT_KNOWN`() {
    // Given
    val reference = "aa-bb-cc-dd"
    val createdBy = "NOT_KNOWN"
    val lastUpdatedBy = "NOT_KNOWN"
    val cancelledBy = "NOT_KNOWN"

    val visitDto = createVisitDto(reference = reference, createdBy = createdBy, updatedBy = lastUpdatedBy, cancelledBy = cancelledBy)
    visitSchedulerMockServer.stubGetVisit(reference, visitDto)

    // When
    val responseSpec = callVisitDetailsByReference(webTestClient, reference, roleVisitSchedulerHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val visitDtoResponse = objectMapper.readValue(responseSpec.expectBody().returnResult().responseBody, VisitDto::class.java)
    Assertions.assertThat(visitDtoResponse.reference).isEqualTo(visitDto.reference)
    Assertions.assertThat(visitDtoResponse.createdBy).isEqualTo(createdBy)
    Assertions.assertThat(visitDtoResponse.createdByFullName).isEqualTo(VisitDetailsClient.NOT_KNOWN)
    Assertions.assertThat(visitDtoResponse.updatedBy).isEqualTo(lastUpdatedBy)
    Assertions.assertThat(visitDtoResponse.updatedByFullName).isEqualTo(VisitDetailsClient.NOT_KNOWN)
    Assertions.assertThat(visitDtoResponse.cancelledBy).isEqualTo(cancelledBy)
    Assertions.assertThat(visitDtoResponse.cancelledByFullName).isEqualTo(VisitDetailsClient.NOT_KNOWN)
  }

  @Test
  fun `when visit exists but userid is null search by reference returns the visit and names as null`() {
    // Given
    val reference = "aa-bb-cc-dd"
    val createdBy = "invalid-user"
    val visitDto = createVisitDto(reference = reference, createdBy = createdBy, updatedBy = null, cancelledBy = null)
    visitSchedulerMockServer.stubGetVisit(reference, visitDto)

    // When
    val responseSpec = callVisitDetailsByReference(webTestClient, reference, roleVisitSchedulerHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val visitDtoResponse = objectMapper.readValue(responseSpec.expectBody().returnResult().responseBody, VisitDto::class.java)
    Assertions.assertThat(visitDtoResponse.reference).isEqualTo(visitDto.reference)
    Assertions.assertThat(visitDtoResponse.createdBy).isEqualTo(createdBy)
    Assertions.assertThat(visitDtoResponse.createdByFullName).isNull()
    Assertions.assertThat(visitDtoResponse.updatedBy).isNull()
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
    val responseSpec = callVisitDetailsByReference(webTestClient, reference, roleVisitSchedulerHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
  }
}
