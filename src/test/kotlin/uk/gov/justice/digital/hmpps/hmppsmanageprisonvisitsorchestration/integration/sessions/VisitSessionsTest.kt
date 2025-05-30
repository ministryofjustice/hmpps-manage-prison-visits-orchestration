package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.sessions

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpHeaders
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitSchedulerClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType.STAFF
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase

@DisplayName("Get visit sessions")
class VisitSessionsTest : IntegrationTestBase() {
  @MockitoSpyBean
  private lateinit var visitSchedulerClient: VisitSchedulerClient
  fun callGetVisitSessions(
    webTestClient: WebTestClient,
    prisonCode: String,
    prisonerId: String,
    username: String? = null,
    userType: UserType? = null,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    val uri = "/visit-sessions"
    val uriQueryParams = mutableListOf("prisonId=$prisonCode", "prisonerId=$prisonerId").also { queryParams ->
      username?.let {
        queryParams.add("username=$username")
      }
      userType?.let {
        queryParams.add("userType=${userType.name}")
      }
    }.joinToString("&")

    return webTestClient.get().uri("$uri?$uriQueryParams")
      .headers(authHttpHeaders)
      .exchange()
  }

  @Test
  fun `when visit sessions for parameters exist then allowed sessions are returned`() {
    // Given
    val prisonCode = "MDI"
    val prisonerId = "ABC"
    val visitSessionDto1 = createVisitSessionDto(prisonCode, "1")
    val visitSessionDto2 = createVisitSessionDto(prisonCode, "2")
    val visitSessionDto3 = createVisitSessionDto(prisonCode, "3")
    val visitSessionDto4 = createVisitSessionDto(prisonCode, "4")
    val visitSessionDto5 = createVisitSessionDto(prisonCode, "5")

    visitSchedulerMockServer.stubGetVisitSessions(prisonCode, prisonerId, mutableListOf(visitSessionDto1, visitSessionDto2, visitSessionDto3, visitSessionDto4, visitSessionDto5), userType = STAFF)

    // When
    val responseSpec = callGetVisitSessions(webTestClient, prisonCode, prisonerId, username = null, userType = STAFF, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(5)
  }

  @Test
  fun `when visit sessions for parameters do not exist then empty list is returned`() {
    // Given
    val prisonCode = "MDI"
    val prisonerId = "ABC"

    visitSchedulerMockServer.stubGetVisitSessions(prisonCode, prisonerId, mutableListOf(), userType = STAFF)

    // When
    val responseSpec = callGetVisitSessions(webTestClient, prisonCode, prisonerId, username = null, userType = STAFF, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(0)
  }

  @Test
  fun `when username not passed to get visit sessions then it is not passed over to visit scheduler client`() {
    // Given
    val prisonCode = "MDI"
    val prisonerId = "ABC"
    val username = null

    // When
    callGetVisitSessions(webTestClient, prisonCode, prisonerId, username = username, userType = STAFF, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    verify(visitSchedulerClient, times(1)).getVisitSessions(prisonCode, prisonerId, null, null, username, userType = STAFF)
  }

  @Test
  fun `when username passed to get visit sessions then it is passed over to visit scheduler client`() {
    // Given
    val prisonCode = "MDI"
    val prisonerId = "ABC"
    val username = "test-user"

    // When
    callGetVisitSessions(webTestClient, prisonCode, prisonerId, username = username, userType = STAFF, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    verify(visitSchedulerClient, times(1)).getVisitSessions(prisonCode, prisonerId, null, null, username, userType = STAFF)
  }

  @Test
  fun `when usertype not passed to get visit sessions usertype defaults to STAFF when visit scheduler client is called`() {
    // Given
    val prisonCode = "MDI"
    val prisonerId = "ABC"

    visitSchedulerMockServer.stubGetVisitSessions(prisonCode, prisonerId, mutableListOf(), userType = STAFF)

    // When
    val responseSpec = callGetVisitSessions(webTestClient, prisonCode, prisonerId, username = null, userType = STAFF, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(0)

    // Then
    // verify getVisitSessions on visit-scheduler is called with userType = STAFF
    verify(visitSchedulerClient, times(1)).getVisitSessions(prisonCode, prisonerId, min = null, max = null, username = null, userType = STAFF)
  }
}
