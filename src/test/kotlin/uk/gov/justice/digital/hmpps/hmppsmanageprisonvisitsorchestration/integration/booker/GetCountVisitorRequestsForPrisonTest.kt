package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.booker

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
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller.PUBLIC_BOOKER_GET_VISITOR_REQUESTS_COUNT_BY_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.VisitorRequestsCountByPrisonCodeDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase

@DisplayName("GET active visitor requests count for prison - $PUBLIC_BOOKER_GET_VISITOR_REQUESTS_COUNT_BY_PRISON_CODE")
class GetCountVisitorRequestsForPrisonTest : IntegrationTestBase() {

  @MockitoSpyBean
  lateinit var prisonVisitBookerRegistryClientSpy: PrisonVisitBookerRegistryClient

  @Test
  fun `when call to get count of visitor requests for prison, then count is returned`() {
    // Given
    val prisonCode = "HEI"
    prisonVisitBookerRegistryMockServer.stubGetCountVisitorRequestsForPrison(prisonCode, VisitorRequestsCountByPrisonCodeDto(5))

    // When
    val responseSpec = callGetVisitorRequestsCountByPrisonCode(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, prisonCode)
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val responseDto = getResults(returnResult)

    assertThat(responseDto.count).isEqualTo(5)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getVisitorRequestsCountByPrisonCode(prisonCode)
  }

  @Test
  fun `when booker registry call returns INTERNAL_SERVER_ERROR then INTERNAL_SERVER_ERROR is returned`() {
    // Given
    val prisonCode = "HEI"
    prisonVisitBookerRegistryMockServer.stubGetCountVisitorRequestsForPrison(prisonCode, null, HttpStatus.INTERNAL_SERVER_ERROR)

    // When
    val responseSpec = callGetVisitorRequestsCountByPrisonCode(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, prisonCode)
    responseSpec.expectStatus().is5xxServerError
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getVisitorRequestsCountByPrisonCode(prisonCode)
  }

  @Test
  fun `when get count visitor requests for prison is called without correct role then FORBIDDEN status is returned`() {
    // When
    val invalidRoleHeaders = setAuthorisation(roles = listOf("ROLE_INVALID"))
    val responseSpec = callGetVisitorRequestsCountByPrisonCode(webTestClient, invalidRoleHeaders, "test")

    // Then
    responseSpec.expectStatus().isForbidden

    // And
    verify(prisonVisitBookerRegistryClientSpy, times(0)).getVisitorRequestsCountByPrisonCode(any())
  }

  @Test
  fun `when get count visitor requests for prison is called without role then UNAUTHORIZED status is returned`() {
    // When
    val url = PUBLIC_BOOKER_GET_VISITOR_REQUESTS_COUNT_BY_PRISON_CODE.replace("{prisonCode}", "HEI")
    val responseSpec = webTestClient.get().uri(url).exchange()

    // Then
    responseSpec.expectStatus().isUnauthorized

    // And
    verify(prisonVisitBookerRegistryClientSpy, times(0)).getVisitorRequestsCountByPrisonCode(any())
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): VisitorRequestsCountByPrisonCodeDto = objectMapper.readValue(returnResult.returnResult().responseBody, VisitorRequestsCountByPrisonCodeDto::class.java)

  fun callGetVisitorRequestsCountByPrisonCode(
    webTestClient: WebTestClient,
    authHttpHeaders: (HttpHeaders) -> Unit,
    prisonCode: String,
  ): WebTestClient.ResponseSpec = webTestClient.get().uri(PUBLIC_BOOKER_GET_VISITOR_REQUESTS_COUNT_BY_PRISON_CODE.replace("{prisonCode}", prisonCode))
    .headers(authHttpHeaders)
    .exchange()
}
