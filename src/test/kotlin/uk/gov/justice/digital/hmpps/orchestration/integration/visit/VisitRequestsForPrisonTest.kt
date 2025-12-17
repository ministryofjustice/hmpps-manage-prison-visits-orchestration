package uk.gov.justice.digital.hmpps.orchestration.integration.visit

import com.fasterxml.jackson.core.type.TypeReference
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec
import uk.gov.justice.digital.hmpps.orchestration.controller.VISIT_REQUESTS_VISITS_FOR_PRISON_PATH
import uk.gov.justice.digital.hmpps.orchestration.dto.visit.scheduler.VisitRequestSummaryDto
import uk.gov.justice.digital.hmpps.orchestration.integration.IntegrationTestBase
import java.time.LocalDate

@DisplayName("GET $VISIT_REQUESTS_VISITS_FOR_PRISON_PATH")
class VisitRequestsForPrisonTest : IntegrationTestBase() {

  @BeforeEach
  fun resetStubs() {
    visitSchedulerMockServer.resetAll()
  }

  val prisonCode = "ABC"

  @Test
  fun `when visit requests exist then appropriate results are returned`() {
    // Given
    val visitRequestsList = listOf(
      VisitRequestSummaryDto(
        visitReference = "ab-cd-ef-gh",
        visitDate = LocalDate.now().plusDays(1),
        requestedOnDate = LocalDate.now().minusDays(3),
        prisonerFirstName = "Prisoner",
        prisonerLastName = "Name",
        prisonNumber = "AA123456",
        mainContact = "Main Contact",
      ),
    )

    visitSchedulerMockServer.stubGetVisitRequestsForPrison(prisonCode, visitRequests = visitRequestsList)

    // When
    val responseSpec = callVisitRequestsForPrison(webTestClient, prisonCode, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val visitRequestList = getVisitRequestsForPrisonResult(responseSpec)
    Assertions.assertThat(visitRequestList.size).isEqualTo(1)
    Assertions.assertThat(visitRequestList[0].visitReference).isEqualTo("ab-cd-ef-gh")
    Assertions.assertThat(visitRequestList[0].visitDate).isEqualTo(LocalDate.now().plusDays(1))
    Assertions.assertThat(visitRequestList[0].requestedOnDate).isEqualTo(LocalDate.now().minusDays(3))
    Assertions.assertThat(visitRequestList[0].prisonerFirstName).isEqualTo("Prisoner")
    Assertions.assertThat(visitRequestList[0].prisonerLastName).isEqualTo("Name")
    Assertions.assertThat(visitRequestList[0].prisonNumber).isEqualTo("AA123456")
    Assertions.assertThat(visitRequestList[0].mainContact).isEqualTo("Main Contact")
  }

  @Test
  fun `when no role specified then access forbidden status is returned`() {
    // Given
    val authHttpHeaders = setAuthorisation(roles = listOf())

    // When
    val responseSpec = callVisitRequestsForPrison(webTestClient, prisonCode, authHttpHeaders)

    // Then
    responseSpec.expectStatus().isForbidden
  }

  @Test
  fun `when no token passed then unauthorized status is returned`() {
    // Given

    // When
    val responseSpec = webTestClient.get().uri(VISIT_REQUESTS_VISITS_FOR_PRISON_PATH.replace("{prisonCode}", prisonCode)).exchange()

    // Then
    responseSpec.expectStatus().isUnauthorized
  }

  fun getVisitRequestsForPrisonResult(responseSpec: ResponseSpec): List<VisitRequestSummaryDto> {
    val responseBody = responseSpec.expectBody().returnResult().responseBody
    return objectMapper.readValue(responseBody, object : TypeReference<List<VisitRequestSummaryDto>>() {})
  }

  fun callVisitRequestsForPrison(
    webTestClient: WebTestClient,
    prisonCode: String,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): ResponseSpec {
    val url = VISIT_REQUESTS_VISITS_FOR_PRISON_PATH.replace("{prisonCode}", prisonCode)

    return webTestClient.get().uri(url)
      .headers(authHttpHeaders)
      .exchange()
  }
}
