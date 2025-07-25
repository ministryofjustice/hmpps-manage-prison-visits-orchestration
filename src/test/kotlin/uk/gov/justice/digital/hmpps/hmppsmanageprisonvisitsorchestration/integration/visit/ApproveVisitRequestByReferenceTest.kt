package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.visit

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller.VISIT_REQUESTS_APPROVE_VISIT_BY_REFERENCE_PATH
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.OrchestrationApproveRejectVisitRequestResponseDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.ApproveVisitRequestBodyDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSubStatus
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitStatus
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import java.time.LocalDate

@DisplayName("GET $VISIT_REQUESTS_APPROVE_VISIT_BY_REFERENCE_PATH")
class ApproveVisitRequestByReferenceTest : IntegrationTestBase() {

  @BeforeEach
  fun resetStubs() {
    visitSchedulerMockServer.resetAll()
  }

  val prisonCode = "ABC"

  @Test
  fun `when approve visit request is called then success response is returned`() {
    // Given
    val visitReference = "ab-cd-ef-gh"
    val prisonerDto = createPrisoner(
      prisonerId = "AB12345DS",
      firstName = "Prisoner",
      lastName = "Name",
      dateOfBirth = LocalDate.of(1980, 1, 1),
      convictedStatus = "Convicted",
    )
    val approveVisitRequestBodyDto = ApproveVisitRequestBodyDto(visitReference, "user_1")

    visitSchedulerMockServer.stubApproveVisitRequestByReference(visitReference, createVisitDto(reference = visitReference, visitStatus = VisitStatus.BOOKED, visitSubStatus = VisitSubStatus.APPROVED))

    // When
    prisonOffenderSearchMockServer.stubGetPrisonerById("AB12345DS", prisonerDto)
    val responseSpec = callApproveVisitRequestByReference(webTestClient, approveVisitRequestBodyDto, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val approvalResponse = getApproveVisitRequestByReferenceResult(responseSpec)
    Assertions.assertThat(approvalResponse.visitReference).isEqualTo(visitReference)
    Assertions.assertThat(approvalResponse.prisonerFirstName).isEqualTo("Prisoner")
    Assertions.assertThat(approvalResponse.prisonerLastName).isEqualTo("Name")
  }

  @Test
  fun `when approve visit request is called but prisoner response fails, then success response is returned with placeholder`() {
    // Given
    val visitReference = "ab-cd-ef-gh"
    val approveVisitRequestBodyDto = ApproveVisitRequestBodyDto(visitReference, "user_1")

    visitSchedulerMockServer.stubApproveVisitRequestByReference(visitReference, createVisitDto(reference = visitReference, visitStatus = VisitStatus.BOOKED, visitSubStatus = VisitSubStatus.APPROVED))

    // When
    prisonOffenderSearchMockServer.stubGetPrisonerById("AB12345DS", null, HttpStatus.INTERNAL_SERVER_ERROR)
    val responseSpec = callApproveVisitRequestByReference(webTestClient, approveVisitRequestBodyDto, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val approvalResponse = getApproveVisitRequestByReferenceResult(responseSpec)
    Assertions.assertThat(approvalResponse.visitReference).isEqualTo(visitReference)
    Assertions.assertThat(approvalResponse.prisonerFirstName).isEqualTo("AB12345DS")
    Assertions.assertThat(approvalResponse.prisonerLastName).isEqualTo("AB12345DS")
  }

  @Test
  fun `when approve visit request is called but fails, then error response is returned up to caller`() {
    // Given
    val visitReference = "ab-cd-ef-gh"
    val approveVisitRequestBodyDto = ApproveVisitRequestBodyDto(visitReference, "user_1")

    visitSchedulerMockServer.stubApproveVisitRequestByReference(visitReference, null, HttpStatus.BAD_REQUEST)

    // When
    prisonOffenderSearchMockServer.stubGetPrisonerById("AB12345DS", null, HttpStatus.INTERNAL_SERVER_ERROR)
    val responseSpec = callApproveVisitRequestByReference(webTestClient, approveVisitRequestBodyDto, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isBadRequest
  }

  @Test
  fun `when no role specified then access forbidden status is returned`() {
    // Given
    val authHttpHeaders = setAuthorisation(roles = listOf())
    val approveVisitRequestBodyDto = ApproveVisitRequestBodyDto("ab-cd-ef-gh", "user_1")

    // When
    val responseSpec = callApproveVisitRequestByReference(webTestClient, approveVisitRequestBodyDto, authHttpHeaders)

    // Then
    responseSpec.expectStatus().isForbidden
  }

  @Test
  fun `when no token passed then unauthorized status is returned`() {
    // Given

    // When
    val responseSpec = webTestClient.get().uri(VISIT_REQUESTS_APPROVE_VISIT_BY_REFERENCE_PATH.replace("{reference}", "ab-cd-ef-gh")).exchange()

    // Then
    responseSpec.expectStatus().isUnauthorized
  }

  fun getApproveVisitRequestByReferenceResult(responseSpec: ResponseSpec): OrchestrationApproveRejectVisitRequestResponseDto = objectMapper.readValue(responseSpec.expectBody().returnResult().responseBody, OrchestrationApproveRejectVisitRequestResponseDto::class.java)

  fun callApproveVisitRequestByReference(
    webTestClient: WebTestClient,
    dto: ApproveVisitRequestBodyDto,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): ResponseSpec {
    val url = VISIT_REQUESTS_APPROVE_VISIT_BY_REFERENCE_PATH.replace("{reference}", dto.visitReference)

    return webTestClient.put().uri(url)
      .headers(authHttpHeaders)
      .body(BodyInserters.fromValue(dto))
      .exchange()
  }
}
