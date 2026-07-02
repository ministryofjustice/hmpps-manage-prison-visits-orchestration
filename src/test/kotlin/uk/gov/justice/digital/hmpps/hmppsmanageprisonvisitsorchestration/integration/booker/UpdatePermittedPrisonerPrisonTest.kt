package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.booker

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller.PUBLIC_BOOKER_UPDATE_PERMITTED_PRISONER_PRISON_CONTROLLER_PATH
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.PermittedPrisonerForBookerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.UpdateRegisteredPrisonerPrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.TestObjectMapper

@DisplayName("$PUBLIC_BOOKER_UPDATE_PERMITTED_PRISONER_PRISON_CONTROLLER_PATH - Update a permitted prisoner's registered prison")
class UpdatePermittedPrisonerPrisonTest : IntegrationTestBase() {
  @Test
  fun `when update permitted prisoner prison is successful HTTP status OK and updated prisoner is returned`() {
    // Given
    val bookerReference = "booker-reference"
    val prisonerId = "AA12345"
    val updateRegisteredPrisonerPrisonDto = UpdateRegisteredPrisonerPrisonDto("MDI")
    val response = PermittedPrisonerForBookerDto(
      prisonerId = prisonerId,
      prisonCode = updateRegisteredPrisonerPrisonDto.prisonCode,
      permittedVisitors = emptyList(),
    )

    prisonVisitBookerRegistryMockServer.stubUpdatePermittedPrisonerPrison(
      bookerReference = bookerReference,
      prisonerId = prisonerId,
      updateRegisteredPrisonerPrisonDto = updateRegisteredPrisonerPrisonDto,
      response = response,
    )

    // When
    val responseSpec = callUpdatePermittedPrisonerPrison(bookerReference, prisonerId, updateRegisteredPrisonerPrisonDto, webTestClient, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val updatedPrisoner = getResponse(responseSpec)
    assertThat(updatedPrisoner.prisonerId).isEqualTo(prisonerId)
    assertThat(updatedPrisoner.prisonCode).isEqualTo(updateRegisteredPrisonerPrisonDto.prisonCode)
    verify(prisonVisitBookerRegistryClientSpy, times(1)).updatePermittedPrisonerPrison(bookerReference, prisonerId, updateRegisteredPrisonerPrisonDto)
  }

  @Test
  fun `when booker registry returns a bad request then it is returned to caller`() {
    // Given
    val bookerReference = "booker-reference"
    val prisonerId = "AA12345"
    val updateRegisteredPrisonerPrisonDto = UpdateRegisteredPrisonerPrisonDto("MDI")

    prisonVisitBookerRegistryMockServer.stubUpdatePermittedPrisonerPrison(
      bookerReference = bookerReference,
      prisonerId = prisonerId,
      updateRegisteredPrisonerPrisonDto = updateRegisteredPrisonerPrisonDto,
      httpStatus = HttpStatus.BAD_REQUEST,
    )

    // When
    val responseSpec = callUpdatePermittedPrisonerPrison(bookerReference, prisonerId, updateRegisteredPrisonerPrisonDto, webTestClient, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isBadRequest
    verify(prisonVisitBookerRegistryClientSpy, times(1)).updatePermittedPrisonerPrison(bookerReference, prisonerId, updateRegisteredPrisonerPrisonDto)
  }

  @Test
  fun `when booker registry returns not found then it is returned to caller`() {
    // Given
    val bookerReference = "booker-reference"
    val prisonerId = "AA12345"
    val updateRegisteredPrisonerPrisonDto = UpdateRegisteredPrisonerPrisonDto("MDI")

    prisonVisitBookerRegistryMockServer.stubUpdatePermittedPrisonerPrison(
      bookerReference = bookerReference,
      prisonerId = prisonerId,
      updateRegisteredPrisonerPrisonDto = updateRegisteredPrisonerPrisonDto,
      httpStatus = HttpStatus.NOT_FOUND,
    )

    // When
    val responseSpec = callUpdatePermittedPrisonerPrison(bookerReference, prisonerId, updateRegisteredPrisonerPrisonDto, webTestClient, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
    verify(prisonVisitBookerRegistryClientSpy, times(1)).updatePermittedPrisonerPrison(bookerReference, prisonerId, updateRegisteredPrisonerPrisonDto)
  }

  @Test
  fun `when update permitted prisoner prison is called with an invalid role request is rejected`() {
    // Given
    val invalidRoleHttpHeaders = setAuthorisation(roles = listOf("ROLE_INVALID"))

    // When
    val responseSpec = callUpdatePermittedPrisonerPrison(
      "booker-reference",
      "AA12345",
      UpdateRegisteredPrisonerPrisonDto("MDI"),
      webTestClient,
      invalidRoleHttpHeaders,
    )

    // Then
    responseSpec.expectStatus().isForbidden
    verify(prisonVisitBookerRegistryClientSpy, times(0)).updatePermittedPrisonerPrison(any(), any(), any())
  }

  private fun getResponse(responseSpec: WebTestClient.ResponseSpec): PermittedPrisonerForBookerDto = TestObjectMapper.mapper.readValue(responseSpec.expectBody().returnResult().responseBody, PermittedPrisonerForBookerDto::class.java)

  private fun callUpdatePermittedPrisonerPrison(
    bookerReference: String,
    prisonerId: String,
    updateRegisteredPrisonerPrisonDto: UpdateRegisteredPrisonerPrisonDto,
    webTestClient: WebTestClient,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    val uri = PUBLIC_BOOKER_UPDATE_PERMITTED_PRISONER_PRISON_CONTROLLER_PATH
      .replace("{bookerReference}", bookerReference)
      .replace("{prisonerId}", prisonerId)

    return webTestClient.put().uri(uri)
      .headers(authHttpHeaders)
      .body(BodyInserters.fromValue(updateRegisteredPrisonerPrisonDto))
      .exchange()
  }
}
