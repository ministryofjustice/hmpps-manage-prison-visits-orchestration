package uk.gov.justice.digital.hmpps.prison.visits.orchestration.integration.visit

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.controller.VISIT_REQUESTS_REJECT_VISIT_BY_REFERENCE_PATH
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.orchestration.OrchestrationApproveRejectVisitRequestResponseDto
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.visit.scheduler.RejectVisitRequestBodyDto
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.visit.scheduler.VisitSubStatus
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.visit.scheduler.enums.VisitStatus
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.integration.IntegrationTestBase
import java.time.LocalDate

@DisplayName("GET $VISIT_REQUESTS_REJECT_VISIT_BY_REFERENCE_PATH")
class RejectVisitRequestByReferenceTest : IntegrationTestBase() {

  @BeforeEach
  fun resetStubs() {
    visitSchedulerMockServer.resetAll()
  }

  val prisonCode = "ABC"

  @Test
  fun `when reject visit request is called then success response is returned`() {
    // Given
    val visitReference = "ab-cd-ef-gh"
    val prisonerDto = createPrisoner(
      prisonerId = "AB12345DS",
      firstName = "Prisoner",
      lastName = "Name",
      dateOfBirth = LocalDate.of(1980, 1, 1),
      convictedStatus = "Convicted",
    )
    val rejectVisitRequestBodyDto = RejectVisitRequestBodyDto(visitReference, "user_1")

    visitSchedulerMockServer.stubRejectVisitRequestByReference(visitReference, createVisitDto(reference = visitReference, visitStatus = VisitStatus.CANCELLED, visitSubStatus = VisitSubStatus.REJECTED))

    // When
    prisonOffenderSearchMockServer.stubGetPrisonerById("AB12345DS", prisonerDto)
    val responseSpec = callRejectVisitRequestByReference(webTestClient, rejectVisitRequestBodyDto, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val rejectionResponse = getRejectVisitRequestByReferenceResult(responseSpec)
    Assertions.assertThat(rejectionResponse.visitReference).isEqualTo(visitReference)
    Assertions.assertThat(rejectionResponse.prisonerFirstName).isEqualTo("Prisoner")
    Assertions.assertThat(rejectionResponse.prisonerLastName).isEqualTo("Name")
  }

  @Test
  fun `when reject visit request is called but prisoner response fails, then success response is returned with placeholder`() {
    // Given
    val visitReference = "ab-cd-ef-gh"
    val rejectVisitRequestBodyDto = RejectVisitRequestBodyDto(visitReference, "user_1")

    visitSchedulerMockServer.stubRejectVisitRequestByReference(visitReference, createVisitDto(reference = visitReference, visitStatus = VisitStatus.CANCELLED, visitSubStatus = VisitSubStatus.REJECTED))

    // When
    prisonOffenderSearchMockServer.stubGetPrisonerById("AB12345DS", null, HttpStatus.INTERNAL_SERVER_ERROR)
    val responseSpec = callRejectVisitRequestByReference(webTestClient, rejectVisitRequestBodyDto, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val rejectionResponse = getRejectVisitRequestByReferenceResult(responseSpec)
    Assertions.assertThat(rejectionResponse.visitReference).isEqualTo(visitReference)
    Assertions.assertThat(rejectionResponse.prisonerFirstName).isEqualTo("AB12345DS")
    Assertions.assertThat(rejectionResponse.prisonerLastName).isEqualTo("AB12345DS")
  }

  @Test
  fun `when reject visit request is called but fails, then error response is returned up to caller`() {
    // Given
    val visitReference = "ab-cd-ef-gh"
    val rejectVisitRequestBodyDto = RejectVisitRequestBodyDto(visitReference, "user_1")

    visitSchedulerMockServer.stubRejectVisitRequestByReference(visitReference, null, HttpStatus.BAD_REQUEST)

    // When
    prisonOffenderSearchMockServer.stubGetPrisonerById("AB12345DS", null, HttpStatus.INTERNAL_SERVER_ERROR)
    val responseSpec = callRejectVisitRequestByReference(webTestClient, rejectVisitRequestBodyDto, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isBadRequest
  }

  @Test
  fun `when no role specified then access forbidden status is returned`() {
    // Given
    val authHttpHeaders = setAuthorisation(roles = listOf())
    val rejectVisitRequestBodyDto = RejectVisitRequestBodyDto("ab-cd-ef-gh", "user_1")

    // When
    val responseSpec = callRejectVisitRequestByReference(webTestClient, rejectVisitRequestBodyDto, authHttpHeaders)

    // Then
    responseSpec.expectStatus().isForbidden
  }

  @Test
  fun `when no token passed then unauthorized status is returned`() {
    // Given

    // When
    val responseSpec = webTestClient.get().uri(VISIT_REQUESTS_REJECT_VISIT_BY_REFERENCE_PATH.replace("{reference}", "ab-cd-ef-gh")).exchange()

    // Then
    responseSpec.expectStatus().isUnauthorized
  }

  fun getRejectVisitRequestByReferenceResult(responseSpec: ResponseSpec): OrchestrationApproveRejectVisitRequestResponseDto = objectMapper.readValue(responseSpec.expectBody().returnResult().responseBody, OrchestrationApproveRejectVisitRequestResponseDto::class.java)

  fun callRejectVisitRequestByReference(
    webTestClient: WebTestClient,
    dto: RejectVisitRequestBodyDto,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): ResponseSpec {
    val url = VISIT_REQUESTS_REJECT_VISIT_BY_REFERENCE_PATH.replace("{reference}", dto.visitReference)

    return webTestClient.put().uri(url)
      .headers(authHttpHeaders)
      .body(BodyInserters.fromValue(dto))
      .exchange()
  }
}
