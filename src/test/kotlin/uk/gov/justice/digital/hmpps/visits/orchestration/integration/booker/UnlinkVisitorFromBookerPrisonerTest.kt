package uk.gov.justice.digital.hmpps.visits.orchestration.integration.booker

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.visits.orchestration.client.PrisonVisitBookerRegistryClient
import uk.gov.justice.digital.hmpps.visits.orchestration.client.UNLINK_VISITOR
import uk.gov.justice.digital.hmpps.visits.orchestration.controller.PUBLIC_BOOKER_UNLINK_VISITOR_CONTROLLER_PATH
import uk.gov.justice.digital.hmpps.visits.orchestration.integration.IntegrationTestBase

@DisplayName("Delete - Unlink visitor from booker prisoner - $UNLINK_VISITOR")
class UnlinkVisitorFromBookerPrisonerTest : IntegrationTestBase() {
  @MockitoSpyBean
  lateinit var prisonVisitBookerRegistryClientSpy: PrisonVisitBookerRegistryClient

  val bookerReference = "reference"
  val prisonerId = "A1234BC"
  val visitorId = "123"

  @Test
  fun `when call to unlink visitor, then call is successfully forwarded to booker registry`() {
    // Given

    // When
    val responseSpec = callUnlinkVisitor(bookerReference, prisonerId, visitorId, webTestClient, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk

    verify(prisonVisitBookerRegistryClientSpy, times(1)).unlinkBookerPrisonerVisitor(bookerReference, prisonerId, visitorId)
  }

  @Test
  fun `when booker registry returns a internal server error, then internal server error is thrown upwards to caller`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubUnlinkVisitor(bookerReference, prisonerId, visitorId, HttpStatus.INTERNAL_SERVER_ERROR)

    // When
    val responseSpec = callUnlinkVisitor(bookerReference, prisonerId, visitorId, webTestClient, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().is5xxServerError

    verify(prisonVisitBookerRegistryClientSpy, times(1)).unlinkBookerPrisonerVisitor(bookerReference, prisonerId, visitorId)
  }

  @Test
  fun `when booker registry returns a 404, then 200 is returned to caller`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubUnlinkVisitor(bookerReference, prisonerId, visitorId, HttpStatus.NOT_FOUND)

    // When
    val responseSpec = callUnlinkVisitor(bookerReference, prisonerId, visitorId, webTestClient, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk

    verify(prisonVisitBookerRegistryClientSpy, times(1)).unlinkBookerPrisonerVisitor(bookerReference, prisonerId, visitorId)
  }

  @Test
  fun `when booker registry is called without token then UNAUTHORIZED status is returned`() {
    // Given

    val uri = PUBLIC_BOOKER_UNLINK_VISITOR_CONTROLLER_PATH
      .replace("{bookerReference}", bookerReference)
      .replace("{prisonerId}", prisonerId)
      .replace("{visitorId}", visitorId)

    // When
    val responseSpec = webTestClient.delete().uri(uri).exchange()

    // Then
    responseSpec.expectStatus().isUnauthorized
  }

  fun callUnlinkVisitor(
    bookerReference: String,
    prisonerNumber: String,
    visitorId: String,
    webTestClient: WebTestClient,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    val uri = PUBLIC_BOOKER_UNLINK_VISITOR_CONTROLLER_PATH
      .replace("{bookerReference}", bookerReference)
      .replace("{prisonerId}", prisonerNumber)
      .replace("{visitorId}", visitorId)

    return webTestClient.delete().uri(uri)
      .headers(authHttpHeaders)
      .exchange()
  }
}
