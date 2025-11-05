package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.booker

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller.PUBLIC_BOOKER_VISITORS_CONTROLLER_PATH
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.PermittedVisitorsForPermittedPrisonerBookerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.RegisterVisitorForBookerPrisonerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase

@DisplayName("$PUBLIC_BOOKER_VISITORS_CONTROLLER_PATH - Register a visitor for a public booker's prisoner")
class RegisterPermittedVisitorForBookerPrisonerTest : IntegrationTestBase() {
  @Test
  fun `when register prisoner for booker is successful HTTP status OK is returned`() {
    // Given
    val bookerReference = "booker-reference"
    val prisonerId = "AA12345"
    val visitorId = 123L
    val active = true
    val registerVisitorForBookerPrisonerDto = RegisterVisitorForBookerPrisonerDto(
      visitorId,
      active,
      notifyBookerFlag = true,
    )
    val registerResponse = PermittedVisitorsForPermittedPrisonerBookerDto(visitorId, active)

    prisonVisitBookerRegistryMockServer.stubRegisterVisitorForBookerPrisoner(bookerReference = bookerReference, prisonerId, registerVisitorForBookerPrisonerDto, registerResponse, HttpStatus.OK)

    // When
    val responseSpec = callRegisterVisitorForBookerPrisoner(bookerReference, prisonerId, registerVisitorForBookerPrisonerDto, webTestClient, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val response = getResponse(responseSpec)
    assertThat(response.visitorId).isEqualTo(visitorId)
    assertThat(response.active).isEqualTo(active)
  }

  @Test
  fun `when register prisoner for booker is returns an error then it is returned to caller`() {
    // Given
    val bookerReference = "booker-reference"
    val prisonerId = "AA12345"
    val visitorId = 123L
    val active = true
    val registerVisitorForBookerPrisonerDto = RegisterVisitorForBookerPrisonerDto(
      visitorId,
      active,
      notifyBookerFlag = true,
    )
    val registerResponse = PermittedVisitorsForPermittedPrisonerBookerDto(visitorId, active)

    prisonVisitBookerRegistryMockServer.stubRegisterVisitorForBookerPrisoner(bookerReference = bookerReference, prisonerId, registerVisitorForBookerPrisonerDto, registerResponse, HttpStatus.INTERNAL_SERVER_ERROR)

    // When
    val responseSpec = callRegisterVisitorForBookerPrisoner(bookerReference, prisonerId, registerVisitorForBookerPrisonerDto, webTestClient, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().is5xxServerError
  }

  @Test
  fun `when register visitor for booker prisoner is called with no auth, request is rejected`() {
    // Given
    val invalidRoleHttpHeaders = setAuthorisation(roles = listOf("ROLE_INVALID"))
    val bookerReference = "booker-reference"
    val prisonerId = "AA12345"
    val visitorId = 123L
    val active = true
    val registerVisitorForBookerPrisonerDto = RegisterVisitorForBookerPrisonerDto(
      visitorId,
      active,
      notifyBookerFlag = true,
    )

    // When
    val responseSpec = callRegisterVisitorForBookerPrisoner(bookerReference, prisonerId, registerVisitorForBookerPrisonerDto, webTestClient, invalidRoleHttpHeaders)

    // Then
    responseSpec.expectStatus().isForbidden
  }

  fun getResponse(responseSpec: WebTestClient.ResponseSpec): PermittedVisitorsForPermittedPrisonerBookerDto = objectMapper.readValue(responseSpec.expectBody().returnResult().responseBody, PermittedVisitorsForPermittedPrisonerBookerDto::class.java)

  fun callRegisterVisitorForBookerPrisoner(
    bookerReference: String,
    prisonerId: String,
    registerVisitorForBookerPrisonerDto: RegisterVisitorForBookerPrisonerDto,
    webTestClient: WebTestClient,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    val uri = PUBLIC_BOOKER_VISITORS_CONTROLLER_PATH
      .replace("{bookerReference}", bookerReference)
      .replace("{prisonerId}", prisonerId)

    return webTestClient.post().uri(uri)
      .headers(authHttpHeaders)
      .body(BodyInserters.fromValue(registerVisitorForBookerPrisonerDto))
      .exchange()
  }
}
