package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.booker

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.UNLINK_VISITOR
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller.PUBLIC_BOOKER_UNLINK_VISITOR_CONTROLLER_PATH
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.StaffUsernameDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase

@DisplayName("POST - Unlink visitor from booker prisoner - $PUBLIC_BOOKER_UNLINK_VISITOR_CONTROLLER_PATH")
class UnlinkVisitorFromBookerPrisonerTest : IntegrationTestBase() {

  val bookerReference = "reference"
  val prisonerId = "A1234BC"
  val visitorId = "123"

  @Test
  fun `when call to unlink visitor, then call is successfully forwarded to booker registry`() {
    // Given
    val staffUsername = StaffUsernameDto("TEST_USER")

    // When
    val responseSpec = callUnlinkVisitor(bookerReference, prisonerId, visitorId, staffUsername, webTestClient, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk

    verify(prisonVisitBookerRegistryClientSpy, times(1)).unlinkBookerPrisonerVisitor(bookerReference, prisonerId, visitorId, staffUsername)
  }

  @Test
  fun `when booker registry returns a internal server error, then internal server error is thrown upwards to caller`() {
    // Given
    val staffUsername = StaffUsernameDto("TEST_USER")
    prisonVisitBookerRegistryMockServer.stubUnlinkVisitor(bookerReference, prisonerId, visitorId, staffUsername, HttpStatus.INTERNAL_SERVER_ERROR)

    // When
    val responseSpec = callUnlinkVisitor(bookerReference, prisonerId, visitorId, staffUsername, webTestClient, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().is5xxServerError

    verify(prisonVisitBookerRegistryClientSpy, times(1)).unlinkBookerPrisonerVisitor(bookerReference, prisonerId, visitorId, staffUsername)
  }

  @Test
  fun `when booker registry returns a 404, then 200 is returned to caller`() {
    // Given
    val staffUsername = StaffUsernameDto("TEST_USER")

    prisonVisitBookerRegistryMockServer.stubUnlinkVisitor(bookerReference, prisonerId, visitorId, staffUsername, HttpStatus.NOT_FOUND)

    // When
    val responseSpec = callUnlinkVisitor(bookerReference, prisonerId, visitorId, staffUsername, webTestClient, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk

    verify(prisonVisitBookerRegistryClientSpy, times(1)).unlinkBookerPrisonerVisitor(bookerReference, prisonerId, visitorId, staffUsername)
  }

  @Test
  fun `when booker registry is called without token then UNAUTHORIZED status is returned`() {
    // Given
    val staffUsername = StaffUsernameDto("TEST_USER")

    val uri = PUBLIC_BOOKER_UNLINK_VISITOR_CONTROLLER_PATH
      .replace("{bookerReference}", bookerReference)
      .replace("{prisonerId}", prisonerId)
      .replace("{visitorId}", visitorId)

    // When
    val responseSpec = webTestClient.post().uri(uri).body(BodyInserters.fromValue(staffUsername)).exchange()

    // Then
    responseSpec.expectStatus().isUnauthorized
  }

  fun callUnlinkVisitor(
    bookerReference: String,
    prisonerNumber: String,
    visitorId: String,
    staffUsername: StaffUsernameDto,
    webTestClient: WebTestClient,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    val uri = PUBLIC_BOOKER_UNLINK_VISITOR_CONTROLLER_PATH
      .replace("{bookerReference}", bookerReference)
      .replace("{prisonerId}", prisonerNumber)
      .replace("{visitorId}", visitorId)

    return webTestClient.post().uri(uri)
      .headers(authHttpHeaders)
      .bodyValue(staffUsername)
      .exchange()
  }
}
