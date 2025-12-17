package uk.gov.justice.digital.hmpps.orchestration.integration.booker

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.isNull
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.orchestration.client.PrisonVisitBookerRegistryClient
import uk.gov.justice.digital.hmpps.orchestration.client.PrisonerContactRegistryClient
import uk.gov.justice.digital.hmpps.orchestration.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.orchestration.controller.PUBLIC_BOOKER_GET_SINGLE_VISITOR_REQUEST
import uk.gov.justice.digital.hmpps.orchestration.dto.booker.registry.PermittedPrisonerForBookerDto
import uk.gov.justice.digital.hmpps.orchestration.dto.booker.registry.PrisonVisitorRequestDto
import uk.gov.justice.digital.hmpps.orchestration.dto.booker.registry.SingleVisitorRequestForReviewDto
import uk.gov.justice.digital.hmpps.orchestration.dto.booker.registry.admin.BookerInfoDto
import uk.gov.justice.digital.hmpps.orchestration.dto.booker.registry.enums.VisitorRequestsStatus.APPROVED
import uk.gov.justice.digital.hmpps.orchestration.dto.booker.registry.enums.VisitorRequestsStatus.REJECTED
import uk.gov.justice.digital.hmpps.orchestration.dto.booker.registry.enums.VisitorRequestsStatus.REQUESTED
import uk.gov.justice.digital.hmpps.orchestration.dto.visit.scheduler.visitor.VisitorLastApprovedDatesDto
import uk.gov.justice.digital.hmpps.orchestration.integration.IntegrationTestBase
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("Get single visitor request for review - $PUBLIC_BOOKER_GET_SINGLE_VISITOR_REQUEST")
class GetSingleVisitorRequestForReviewTest : IntegrationTestBase() {

  @MockitoSpyBean
  lateinit var prisonVisitBookerRegistryClientSpy: PrisonVisitBookerRegistryClient

  @MockitoSpyBean
  lateinit var prisonerContactRegistryClient: PrisonerContactRegistryClient

  @MockitoSpyBean
  lateinit var prisonerSearchClient: PrisonerSearchClient

  @Test
  fun `when call to get single visitor request for review then request returned with prisoners approved contacts list`() {
    // Given
    val bookerReference = "booker-ref"
    val prisonerId = "AA123456"
    val requestReference = "abc-def-ghi"

    val prisoner1Dto = PermittedPrisonerForBookerDto(
      prisonerId,
      "HEI",
      emptyList(),
    )

    val contact1 = createVisitor(
      firstName = "First",
      lastName = "VisitorA",
      dateOfBirth = LocalDate.of(1980, 1, 1),
    )

    val contact2 = createVisitor(
      firstName = "Second",
      lastName = "VisitorB",
      dateOfBirth = LocalDate.of(1990, 1, 1),
    )

    val booker = BookerInfoDto(
      reference = bookerReference,
      email = "test@test.com",
      createdTimestamp = LocalDateTime.now().minusMonths(1),
      permittedPrisoners = listOf(prisoner1Dto),
    )

    // visitor 4 is not on the list returned
    val lastApprovedDatesList = mapOf(
      contact1.personId to LocalDate.now().minusMonths(1),
      contact2.personId to LocalDate.now().minusMonths(2),
    ).map { VisitorLastApprovedDatesDto(it.key, it.value) }

    prisonVisitBookerRegistryMockServer.stubGetSingleVisitorRequest(
      requestReference,
      visitorRequest = PrisonVisitorRequestDto(requestReference, booker.reference, booker.email, prisonerId, "firstName", "lastName", LocalDate.now().minusYears(21), LocalDate.now(), status = REQUESTED),
    )

    prisonVisitBookerRegistryMockServer.stubGetBookerByBookerReference(booker.reference, booker = booker)

    prisonerContactRegistryMockServer.stubGetApprovedPrisonerContacts(
      prisonerId,
      withAddress = false,
      hasDateOfBirth = null,
      contactsList = createContactsList(listOf(contact1, contact2)),
    )

    visitSchedulerMockServer.stubGetVisitorsLastApprovedDates(prisonerId, listOf(contact1.personId, contact2.personId), lastApprovedDatesList)

    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, createPrisoner(prisonerId, firstName = "First", lastName = "Last", dateOfBirth = LocalDate.now(), convictedStatus = "Convicted"))

    // When
    val responseSpec = callGetSingleVisitorRequestForReview(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, requestReference)

    responseSpec.expectStatus().isOk
    val response = getResults(responseSpec.expectBody())

    assertThat(response.reference).isEqualTo(requestReference)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getSingleVisitorRequest(any())
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getBookerByBookerReference(any())
    verify(prisonerContactRegistryClient, times(1)).getPrisonersApprovedSocialContacts(any(), any(), isNull())
  }

  @Test
  fun `when prisoner search returns INTERNAL_ERROR then INTERNAL_ERROR is returned`() {
    // Given
    val bookerReference = "booker-ref"
    val prisonerId = "AA123456"
    val requestReference = "abc-def-ghi"

    val prisoner1Dto = PermittedPrisonerForBookerDto(
      prisonerId,
      "HEI",
      emptyList(),
    )

    val contact1 = createVisitor(
      firstName = "First",
      lastName = "VisitorA",
      dateOfBirth = LocalDate.of(1980, 1, 1),
    )

    val contact2 = createVisitor(
      firstName = "Second",
      lastName = "VisitorB",
      dateOfBirth = LocalDate.of(1990, 1, 1),
    )

    val booker = BookerInfoDto(
      reference = bookerReference,
      email = "test@test.com",
      createdTimestamp = LocalDateTime.now().minusMonths(1),
      permittedPrisoners = listOf(prisoner1Dto),
    )

    // visitor 4 is not on the list returned
    val lastApprovedDatesList = mapOf(
      contact1.personId to LocalDate.now().minusMonths(1),
      contact2.personId to LocalDate.now().minusMonths(2),
    ).map { VisitorLastApprovedDatesDto(it.key, it.value) }

    prisonVisitBookerRegistryMockServer.stubGetSingleVisitorRequest(
      requestReference,
      visitorRequest = PrisonVisitorRequestDto(requestReference, booker.reference, booker.email, prisonerId, "firstName", "lastName", LocalDate.now().minusYears(21), LocalDate.now(), status = APPROVED),
    )

    prisonVisitBookerRegistryMockServer.stubGetBookerByBookerReference(booker.reference, booker = booker)

    prisonerContactRegistryMockServer.stubGetApprovedPrisonerContacts(
      prisonerId,
      withAddress = false,
      hasDateOfBirth = null,
      contactsList = createContactsList(listOf(contact1, contact2)),
    )

    visitSchedulerMockServer.stubGetVisitorsLastApprovedDates(prisonerId, listOf(contact1.personId, contact2.personId), lastApprovedDatesList)

    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, null, HttpStatus.INTERNAL_SERVER_ERROR)

    // When
    val responseSpec = callGetSingleVisitorRequestForReview(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, requestReference)

    responseSpec.expectStatus().is5xxServerError

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getSingleVisitorRequest(any())
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getBookerByBookerReference(any())
    verify(prisonerContactRegistryClient, times(1)).getPrisonersApprovedSocialContacts(any(), any(), isNull())
    verify(prisonerSearchClient, times(1)).getPrisonerById(any())
  }

  @Test
  fun `when prisoner-contact-registry returns INTERNAL_ERROR then INTERNAL_ERROR is returned`() {
    // Given
    val bookerReference = "booker-ref"
    val prisonerId = "AA123456"
    val requestReference = "abc-def-ghi"

    val prisoner1Dto = PermittedPrisonerForBookerDto(
      prisonerId,
      "HEI",
      emptyList(),
    )

    val booker = BookerInfoDto(
      reference = bookerReference,
      email = "test@test.com",
      createdTimestamp = LocalDateTime.now().minusMonths(1),
      permittedPrisoners = listOf(prisoner1Dto),
    )

    prisonVisitBookerRegistryMockServer.stubGetSingleVisitorRequest(
      requestReference,
      visitorRequest = PrisonVisitorRequestDto(requestReference, booker.reference, booker.email, prisonerId, "firstName", "lastName", LocalDate.now().minusYears(21), LocalDate.now(), status = REJECTED),
    )

    prisonVisitBookerRegistryMockServer.stubGetBookerByBookerReference(booker.reference, booker = booker)

    prisonerContactRegistryMockServer.stubGetApprovedPrisonerContacts(
      prisonerId,
      withAddress = false,
      hasDateOfBirth = null,
      contactsList = null,
      httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
    )

    // When
    val responseSpec = callGetSingleVisitorRequestForReview(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, requestReference)

    responseSpec.expectStatus().is5xxServerError

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getSingleVisitorRequest(any())
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getBookerByBookerReference(any())
    verify(prisonerContactRegistryClient, times(1)).getPrisonersApprovedSocialContacts(any(), any(), isNull())
    verify(prisonerSearchClient, times(0)).getPrisonerById(any())
  }

  @Test
  fun `when booker registry call returns NOT_FOUND then NOT_FOUND is returned`() {
    // Given
    val bookerReference = "booker-ref"
    val prisonerId = "AA123456"
    val requestReference = "abc-def-ghi"

    //
    prisonVisitBookerRegistryMockServer.stubGetSingleVisitorRequest(requestReference, null, HttpStatus.NOT_FOUND)

    // When
    val responseSpec = callGetSingleVisitorRequestForReview(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, requestReference)

    responseSpec.expectStatus().isNotFound

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getSingleVisitorRequest(any())
    verify(prisonVisitBookerRegistryClientSpy, times(0)).getBookerByBookerReference(any())
    verify(prisonerContactRegistryClient, times(0)).getPrisonersApprovedSocialContacts(any(), any(), any())
    verify(prisonerSearchClient, times(0)).getPrisonerById(any())
  }

  @Test
  fun `when booker registry call returns INTERNAL_SERVER_ERROR then INTERNAL_SERVER_ERROR is returned`() {
    // Given
    val bookerReference = "booker-ref"
    val prisonerId = "AA123456"
    val requestReference = "abc-def-ghi"

    prisonVisitBookerRegistryMockServer.stubGetSingleVisitorRequest(requestReference, null, HttpStatus.INTERNAL_SERVER_ERROR)

    // When
    val responseSpec = callGetSingleVisitorRequestForReview(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, requestReference)

    responseSpec.expectStatus().is5xxServerError

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getSingleVisitorRequest(any())
    verify(prisonVisitBookerRegistryClientSpy, times(0)).getBookerByBookerReference(any())
    verify(prisonerContactRegistryClient, times(0)).getPrisonersApprovedSocialContacts(any(), any(), any())
    verify(prisonerSearchClient, times(0)).getPrisonerById(any())
  }

  @Test
  fun `when get single visitor request for review is called without correct role then FORBIDDEN status is returned`() {
    // When
    val invalidRoleHeaders = setAuthorisation(roles = listOf("ROLE_INVALID"))
    val responseSpec = callGetSingleVisitorRequestForReview(webTestClient, invalidRoleHeaders, "test")

    // Then
    responseSpec.expectStatus().isForbidden

    // And
    verify(prisonVisitBookerRegistryClientSpy, times(0)).getSingleVisitorRequest(any())
    verify(prisonVisitBookerRegistryClientSpy, times(0)).getBookerByBookerReference(any())
    verify(prisonerContactRegistryClient, times(0)).getPrisonersApprovedSocialContacts(any(), any(), any())
    verify(prisonerSearchClient, times(0)).getPrisonerById(any())
  }

  @Test
  fun `when get a single visitor request is called without correct role then UNAUTHORIZED status is returned`() {
    // When
    val url = PUBLIC_BOOKER_GET_SINGLE_VISITOR_REQUEST.replace("{requestReference}", "requestReference")

    val responseSpec = webTestClient.put().uri(url).exchange()

    // Then
    responseSpec.expectStatus().isUnauthorized

    // And
    verify(prisonVisitBookerRegistryClientSpy, times(0)).getSingleVisitorRequest(any())
    verify(prisonVisitBookerRegistryClientSpy, times(0)).getBookerByBookerReference(any())
    verify(prisonerContactRegistryClient, times(0)).getPrisonersApprovedSocialContacts(any(), any(), any())
    verify(prisonerSearchClient, times(0)).getPrisonerById(any())
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): SingleVisitorRequestForReviewDto = objectMapper.readValue(returnResult.returnResult().responseBody, SingleVisitorRequestForReviewDto::class.java)

  fun callGetSingleVisitorRequestForReview(
    webTestClient: WebTestClient,
    authHttpHeaders: (HttpHeaders) -> Unit,
    requestReference: String,
  ): WebTestClient.ResponseSpec = webTestClient.get().uri(
    PUBLIC_BOOKER_GET_SINGLE_VISITOR_REQUEST.replace("{requestReference}", requestReference),
  )
    .headers(authHttpHeaders)
    .exchange()
}
