package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.booker

import com.fasterxml.jackson.core.type.TypeReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonVisitBookerRegistryClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller.PUBLIC_BOOKER_GET_VISITOR_REQUESTS_BY_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller.PUBLIC_BOOKER_GET_VISITOR_REQUESTS_COUNT_BY_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.PrisonVisitorRequestDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import java.time.LocalDate

@DisplayName("Get active visitor requests for booker")
class GetVisitorRequestsForPrisonTest : IntegrationTestBase() {

  @MockitoSpyBean
  lateinit var prisonVisitBookerRegistryClientSpy: PrisonVisitBookerRegistryClient

  @Test
  fun `when call to get list of active visitor requests for prison, then count is returned`() {
    // Given
    val prisonCode = "HEI"
    prisonVisitBookerRegistryMockServer.stubGetVisitorRequestsForPrison(prisonCode, listOf(PrisonVisitorRequestDto(reference = "abc-def-ghi", bookerReference = "xyz-rhf-sjd", bookerEmail = "test@test.com", prisonerId = "A11", firstName = "VisitorTwo", lastName = "Second", dateOfBirth = LocalDate.of(1990, 2, 2), requestedOn = LocalDate.now())))

    // When
    val responseSpec = callGetVisitorRequestsByPrisonCode(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, prisonCode)
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val responseDto = getResults(returnResult)

    assertThat(responseDto.size).isEqualTo(1)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getVisitorRequestsByPrisonCode(prisonCode)
  }

  @Test
  fun `when booker registry call returns INTERNAL_SERVER_ERROR then INTERNAL_SERVER_ERROR is returned`() {
    // Given
    val prisonCode = "HEI"
    prisonVisitBookerRegistryMockServer.stubGetVisitorRequestsForPrison(prisonCode, null, HttpStatus.INTERNAL_SERVER_ERROR)

    // When
    val responseSpec = callGetVisitorRequestsByPrisonCode(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, prisonCode)
    responseSpec.expectStatus().is5xxServerError
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getVisitorRequestsByPrisonCode(prisonCode)
  }

  @Test
  fun `when get list of active visitor requests for prison is called without correct role then FORBIDDEN status is returned`() {
    // When
    val invalidRoleHeaders = setAuthorisation(roles = listOf("ROLE_INVALID"))
    val responseSpec = callGetVisitorRequestsByPrisonCode(webTestClient, invalidRoleHeaders, "test")

    // Then
    responseSpec.expectStatus().isForbidden

    // And
    verify(prisonVisitBookerRegistryClientSpy, times(0)).getVisitorRequestsByPrisonCode(any())
  }

  @Test
  fun `when get list of active visitor requests for prison is called without role then UNAUTHORIZED status is returned`() {
    // When
    val url = PUBLIC_BOOKER_GET_VISITOR_REQUESTS_COUNT_BY_PRISON_CODE.replace("{prisonCode}", "HEI")
    val responseSpec = webTestClient.put().uri(url).exchange()

    // Then
    responseSpec.expectStatus().isUnauthorized

    // And
    verify(prisonVisitBookerRegistryClientSpy, times(0)).getVisitorRequestsByPrisonCode(any())
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): List<PrisonVisitorRequestDto> = objectMapper.readValue(returnResult.returnResult().responseBody, object : TypeReference<List<PrisonVisitorRequestDto>>() {})

  fun callGetVisitorRequestsByPrisonCode(
    webTestClient: WebTestClient,
    authHttpHeaders: (HttpHeaders) -> Unit,
    prisonCode: String,
  ): WebTestClient.ResponseSpec = webTestClient.get().uri(PUBLIC_BOOKER_GET_VISITOR_REQUESTS_BY_PRISON_CODE.replace("{prisonCode}", prisonCode))
    .headers(authHttpHeaders)
    .exchange()
}
