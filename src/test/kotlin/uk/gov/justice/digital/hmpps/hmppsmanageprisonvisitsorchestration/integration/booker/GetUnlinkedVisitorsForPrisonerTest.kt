package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.booker

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonVisitBookerRegistryClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonerContactRegistryClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller.PUBLIC_BOOKER_GET_UNLINKED_VISITORS_BY_PRISONER_PATH
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.management.UnlinkedVisitorDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.PermittedPrisonerForBookerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.PermittedVisitorsForPermittedPrisonerBookerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.admin.BookerInfoDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("Get unlinked visitors for for booker and prisoner")
class GetUnlinkedVisitorsForPrisonerTest : IntegrationTestBase() {
  @MockitoSpyBean
  lateinit var prisonVisitBookerRegistryClientSpy: PrisonVisitBookerRegistryClient

  @MockitoSpyBean
  lateinit var prisonerContactRegistryClientSpy: PrisonerContactRegistryClient

  private final val prisonCode = "HEI"
  private final val bookerReference = "booker-1"
  private final val prisonerId = "AA112233B"

  private val visitor1 = createVisitor(
    firstName = "First",
    lastName = "VisitorA",
    dateOfBirth = LocalDate.of(1980, 1, 1),
  )

  private val visitor2 = createVisitor(
    firstName = "Second",
    lastName = "VisitorB",
    dateOfBirth = LocalDate.of(1990, 1, 1),
  )

  private val visitor3 = createVisitor(
    firstName = "Third",
    lastName = "VisitorC",
    dateOfBirth = LocalDate.of(1985, 1, 1),
  )

  private val visitor4 = createVisitor(
    firstName = "Fourth",
    lastName = "VisitorD",
    dateOfBirth = null,
  )

  private val visitor5 = createVisitor(
    firstName = "Fifth",
    lastName = "VisitorE",
    dateOfBirth = null,
  )

  @Test
  fun `when booker's prisoners has some linked visitors then all other approved visitors are returned as unlinked visitors`() {
    // Given
    val prisoner1Dto = PermittedPrisonerForBookerDto(
      prisonerId,
      true,
      prisonCode,
      listOf(
        PermittedVisitorsForPermittedPrisonerBookerDto(visitor3.personId, true),
        PermittedVisitorsForPermittedPrisonerBookerDto(visitor4.personId, true),
      ),
    )

    val booker = BookerInfoDto(
      reference = bookerReference,
      email = "test@test.com",
      createdTimestamp = LocalDateTime.now().minusMonths(1),
      permittedPrisoners = listOf(prisoner1Dto),
    )

    val contactsList = createContactsList(listOf(visitor1, visitor2, visitor3, visitor4, visitor5))

    prisonVisitBookerRegistryMockServer.stubGetBookerByBookerReference(bookerReference, booker)
    prisonerContactRegistryMockServer.stubGetApprovedPrisonerContacts(prisonerId, withAddress = false, hasDateOfBirth = null, contactsList)

    // When
    val responseSpec = callGetUnlinkedVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, bookerReference, prisonerId)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    // only the unlinked visitors will be returned
    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(3)
    assertUnlinkedVisitors(prisonerDetailsList[0], visitor1, null)
    assertUnlinkedVisitors(prisonerDetailsList[1], visitor2, null)
    assertUnlinkedVisitors(prisonerDetailsList[2], visitor5, null)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getBookerByBookerReference(bookerReference)
    verify(prisonerContactRegistryClientSpy, times(1)).getPrisonersApprovedSocialContacts(prisonerId, withAddress = false, hasDateOfBirth = null)
  }

  @Test
  fun `when booker's prisoners has no linked visitors then all other approved visitors are returned as unlinked visitors`() {
    // Given
    val prisoner1Dto = PermittedPrisonerForBookerDto(
      prisonerId,
      true,
      prisonCode,
      emptyList(),
    )

    val booker = BookerInfoDto(
      reference = bookerReference,
      email = "test@test.com",
      createdTimestamp = LocalDateTime.now().minusMonths(1),
      permittedPrisoners = listOf(prisoner1Dto),
    )

    val contactsList = createContactsList(listOf(visitor1, visitor2, visitor3, visitor4, visitor5))

    prisonVisitBookerRegistryMockServer.stubGetBookerByBookerReference(bookerReference, booker)
    prisonerContactRegistryMockServer.stubGetApprovedPrisonerContacts(prisonerId, withAddress = false, hasDateOfBirth = null, contactsList)

    // When
    val responseSpec = callGetUnlinkedVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, bookerReference, prisonerId)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    // all visitors will be returned as none of them are linked so far
    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(5)
    assertUnlinkedVisitors(prisonerDetailsList[0], visitor1, null)
    assertUnlinkedVisitors(prisonerDetailsList[1], visitor2, null)
    assertUnlinkedVisitors(prisonerDetailsList[2], visitor3, null)
    assertUnlinkedVisitors(prisonerDetailsList[3], visitor4, null)
    assertUnlinkedVisitors(prisonerDetailsList[4], visitor5, null)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getBookerByBookerReference(bookerReference)
    verify(prisonerContactRegistryClientSpy, times(1)).getPrisonersApprovedSocialContacts(prisonerId, withAddress = false, hasDateOfBirth = null)
  }

  @Test
  fun `when booker not found on booker registry then a NOT_FOUND error is returned`() {
    // Given
    val incorrectBookerReference = "incorrect-booker-reference"

    prisonVisitBookerRegistryMockServer.stubGetBookerByBookerReference(bookerReference, null, HttpStatus.NOT_FOUND)

    // When
    val responseSpec = callGetUnlinkedVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, incorrectBookerReference, prisonerId)

    // Then
    responseSpec.expectStatus().isNotFound
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getBookerByBookerReference(incorrectBookerReference)
    verify(prisonerContactRegistryClientSpy, times(0)).getPrisonersApprovedSocialContacts(any(), any(), anyOrNull())
    assertErrorResult(responseSpec, HttpStatus.NOT_FOUND, "booker not found on booker-registry for booker reference - $incorrectBookerReference")
  }

  @Test
  fun `when prisoner not associated with booker on booker registry then a NOT_FOUND error is returned`() {
    // Given
    val incorrectPrisonerId = "incorrect-prisoner-id"

    val prisoner1Dto = PermittedPrisonerForBookerDto(
      prisonerId,
      true,
      prisonCode,
      emptyList(),
    )

    val booker = BookerInfoDto(
      reference = bookerReference,
      email = "test@test.com",
      createdTimestamp = LocalDateTime.now().minusMonths(1),
      permittedPrisoners = listOf(prisoner1Dto),
    )

    val contactsList = createContactsList(listOf(visitor1, visitor2, visitor3, visitor4, visitor5))

    prisonVisitBookerRegistryMockServer.stubGetBookerByBookerReference(bookerReference, booker)
    prisonerContactRegistryMockServer.stubGetApprovedPrisonerContacts(prisonerId, withAddress = false, hasDateOfBirth = null, contactsList)

    // When
    val responseSpec = callGetUnlinkedVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, bookerReference, incorrectPrisonerId)

    // Then
    responseSpec.expectStatus().isNotFound
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getBookerByBookerReference(bookerReference)
    verify(prisonerContactRegistryClientSpy, times(0)).getPrisonersApprovedSocialContacts(any(), any(), anyOrNull())
    assertErrorResult(responseSpec, HttpStatus.NOT_FOUND, "Prisoner with number - $incorrectPrisonerId not found for booker reference - $bookerReference")
  }

  @Test
  fun `when booker registry returns an internal server error, then internal server error is thrown upwards to caller`() {
    // Given
    val bookerReference = "abc-def-ghi"

    prisonVisitBookerRegistryMockServer.stubGetBookerByBookerReference(bookerReference, null, HttpStatus.INTERNAL_SERVER_ERROR)

    // When
    val responseSpec = callGetUnlinkedVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, bookerReference, prisonerId)

    // Then
    responseSpec.expectStatus().is5xxServerError
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getBookerByBookerReference(bookerReference)
    verify(prisonerContactRegistryClientSpy, times(0)).getPrisonersApprovedSocialContacts(any(), any(), anyOrNull())
  }

  @Test
  fun `when prisoner contact registry returns NOT_FOUND error then no visitor details are returned`() {
    // Given
    val prisoner1Dto = PermittedPrisonerForBookerDto(
      prisonerId,
      true,
      prisonCode,
      emptyList(),
    )

    val booker = BookerInfoDto(
      reference = bookerReference,
      email = "test@test.com",
      createdTimestamp = LocalDateTime.now().minusMonths(1),
      permittedPrisoners = listOf(prisoner1Dto),
    )

    prisonVisitBookerRegistryMockServer.stubGetBookerByBookerReference(bookerReference, booker)
    prisonerContactRegistryMockServer.stubGetApprovedPrisonerContacts(prisonerId, withAddress = false, hasDateOfBirth = null, null, HttpStatus.NOT_FOUND)

    // When
    val responseSpec = callGetUnlinkedVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, bookerReference, prisonerId)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(0)
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getBookerByBookerReference(bookerReference)
    verify(prisonerContactRegistryClientSpy, times(1)).getPrisonersApprovedSocialContacts(prisonerId, withAddress = false, hasDateOfBirth = null)
  }

  @Test
  fun `when prisoner contact registry returns INTERNAL_SERVER_ERROR then INTERNAL_SERVER_ERROR is thrown upwards to caller`() {
    // Given
    // Given
    val prisoner1Dto = PermittedPrisonerForBookerDto(
      prisonerId,
      true,
      prisonCode,
      emptyList(),
    )

    val booker = BookerInfoDto(
      reference = bookerReference,
      email = "test@test.com",
      createdTimestamp = LocalDateTime.now().minusMonths(1),
      permittedPrisoners = listOf(prisoner1Dto),
    )

    prisonVisitBookerRegistryMockServer.stubGetBookerByBookerReference(bookerReference, booker)
    prisonerContactRegistryMockServer.stubGetApprovedPrisonerContacts(prisonerId, withAddress = false, hasDateOfBirth = null, null, HttpStatus.INTERNAL_SERVER_ERROR)

    // When
    val responseSpec = callGetUnlinkedVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, bookerReference, prisonerId)

    // Then
    responseSpec.expectStatus().is5xxServerError
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getBookerByBookerReference(bookerReference)
    verify(prisonerContactRegistryClientSpy, times(1)).getPrisonersApprovedSocialContacts(any(), any(), anyOrNull())
  }

  @Test
  fun `when endpoint is called without token then UNAUTHORIZED status is returned`() {
    // Given
    val bookerReference = "abc-def-ghi"

    // When
    val responseSpec = webTestClient.get().uri(PUBLIC_BOOKER_GET_UNLINKED_VISITORS_BY_PRISONER_PATH.replace("{bookerReference}", bookerReference).replace("{prisonerId}", prisonerId))
      .exchange()

    // Then
    responseSpec.expectStatus().isUnauthorized
  }

  private fun assertUnlinkedVisitors(unlinkedVisitorDto: UnlinkedVisitorDto, visitor: VisitorDetails, lastApprovedVisitDate: LocalDate?) {
    Assertions.assertThat(unlinkedVisitorDto.visitorId).isEqualTo(visitor.personId)
    Assertions.assertThat(unlinkedVisitorDto.firstName).isEqualTo(visitor.firstName)
    Assertions.assertThat(unlinkedVisitorDto.lastName).isEqualTo(visitor.lastName)
    Assertions.assertThat(unlinkedVisitorDto.dateOfBirth).isEqualTo(visitor.dateOfBirth)
    Assertions.assertThat(unlinkedVisitorDto.lastApprovedForVisitDate).isEqualTo(lastApprovedVisitDate)
  }

  private fun assertErrorResult(
    responseSpec: WebTestClient.ResponseSpec,
    httpStatusCode: HttpStatusCode = HttpStatusCode.valueOf(org.apache.http.HttpStatus.SC_BAD_REQUEST),
    errorMessage: String? = null,
  ) {
    responseSpec.expectStatus().isEqualTo(httpStatusCode)
    errorMessage?.let {
      val errorResponse =
        objectMapper.readValue(responseSpec.expectBody().returnResult().responseBody, ErrorResponse::class.java)
      Assertions.assertThat(errorResponse.developerMessage).isEqualTo(errorMessage)
    }
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): List<UnlinkedVisitorDto> = objectMapper.readValue(returnResult.returnResult().responseBody, Array<UnlinkedVisitorDto>::class.java).toList()

  fun callGetUnlinkedVisitorsByBookersPrisoner(
    webTestClient: WebTestClient,
    authHttpHeaders: (HttpHeaders) -> Unit,
    bookerReference: String,
    prisonerId: String,
  ): WebTestClient.ResponseSpec = webTestClient.get().uri(PUBLIC_BOOKER_GET_UNLINKED_VISITORS_BY_PRISONER_PATH.replace("{bookerReference}", bookerReference).replace("{prisonerId}", prisonerId))
    .headers(authHttpHeaders)
    .exchange()
}
