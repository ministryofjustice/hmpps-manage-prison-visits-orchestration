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
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller.PUBLIC_BOOKER_GET_SOCIAL_CONTACTS_BY_PRISONER_PATH
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.management.SocialContactsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.PermittedPrisonerForBookerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.PermittedVisitorsForPermittedPrisonerBookerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.admin.BookerInfoDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitor.VisitorLastApprovedDatesDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.TestObjectMapper
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("Get social contacts not registered for booker and prisoner")
class GetSocialContactsForPrisonerTest : IntegrationTestBase() {

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
      prisonCode,
      listOf(
        PermittedVisitorsForPermittedPrisonerBookerDto(visitor3.personId),
        PermittedVisitorsForPermittedPrisonerBookerDto(visitor4.personId),
      ),
    )

    val booker = BookerInfoDto(
      reference = bookerReference,
      email = "test@test.com",
      createdTimestamp = LocalDateTime.now().minusMonths(1),
      permittedPrisoners = listOf(prisoner1Dto),
    )

    val contactsList = createContactsList(listOf(visitor1, visitor2, visitor3, visitor4, visitor5))
    val lastApprovedDatesList = mapOf(
      visitor1.personId to LocalDate.now().minusMonths(1),
      visitor2.personId to LocalDate.now().minusMonths(2),
      visitor5.personId to null,
    ).map { VisitorLastApprovedDatesDto(it.key, it.value) }

    prisonVisitBookerRegistryMockServer.stubGetBookerByBookerReference(bookerReference, booker)
    prisonerContactRegistryMockServer.stubGetApprovedPrisonerContacts(prisonerId, withAddress = false, hasDateOfBirth = null, contactsList)
    visitSchedulerMockServer.stubGetVisitorsLastApprovedDates(prisonerId, listOf(visitor1.personId, visitor2.personId, visitor5.personId), lastApprovedDatesList)

    // When
    val responseSpec = callGetSocialContactsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, bookerReference, prisonerId)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    // only the unlinked visitors will be returned
    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(3)
    assertSocialContacts(prisonerDetailsList[0], visitor1, LocalDate.now().minusMonths(1))
    assertSocialContacts(prisonerDetailsList[1], visitor2, LocalDate.now().minusMonths(2))
    assertSocialContacts(prisonerDetailsList[2], visitor5, null)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getBookerByBookerReference(bookerReference)
    verify(prisonerContactRegistryClientSpy, times(1)).getPrisonersApprovedSocialContacts(prisonerId, withAddress = false, hasDateOfBirth = null)
    verify(visitSchedulerClientSpy, times(1)).findLastApprovedDateForVisitor(prisonerId, listOf(visitor1.personId, visitor2.personId, visitor5.personId))
  }

  @Test
  fun `when booker's prisoners has no linked visitors then all other approved visitors are returned as unlinked visitors`() {
    // Given
    val prisoner1Dto = PermittedPrisonerForBookerDto(
      prisonerId,
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

    // visitor 4 is not on the list returned
    val lastApprovedDatesList = mapOf(
      visitor1.personId to LocalDate.now().minusMonths(1),
      visitor2.personId to LocalDate.now().minusMonths(2),
      visitor3.personId to LocalDate.now().minusMonths(3),
      visitor5.personId to null,
    ).map { VisitorLastApprovedDatesDto(it.key, it.value) }

    prisonVisitBookerRegistryMockServer.stubGetBookerByBookerReference(bookerReference, booker)
    prisonerContactRegistryMockServer.stubGetApprovedPrisonerContacts(prisonerId, withAddress = false, hasDateOfBirth = null, contactsList)
    visitSchedulerMockServer.stubGetVisitorsLastApprovedDates(prisonerId, listOf(visitor1.personId, visitor2.personId, visitor3.personId, visitor4.personId, visitor5.personId), lastApprovedDatesList)

    // When
    val responseSpec = callGetSocialContactsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, bookerReference, prisonerId)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    // all visitors will be returned as none of them are linked so far
    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(5)
    assertSocialContacts(prisonerDetailsList[0], visitor1, LocalDate.now().minusMonths(1))
    assertSocialContacts(prisonerDetailsList[1], visitor2, LocalDate.now().minusMonths(2))
    assertSocialContacts(prisonerDetailsList[2], visitor3, LocalDate.now().minusMonths(3))
    assertSocialContacts(prisonerDetailsList[3], visitor4, null)
    assertSocialContacts(prisonerDetailsList[4], visitor5, null)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getBookerByBookerReference(bookerReference)
    verify(prisonerContactRegistryClientSpy, times(1)).getPrisonersApprovedSocialContacts(prisonerId, withAddress = false, hasDateOfBirth = null)
    verify(visitSchedulerClientSpy, times(1)).findLastApprovedDateForVisitor(prisonerId, listOf(visitor1.personId, visitor2.personId, visitor3.personId, visitor4.personId, visitor5.personId))
  }

  @Test
  fun `when booker not found on booker registry then a NOT_FOUND error is returned`() {
    // Given
    val incorrectBookerReference = "incorrect-booker-reference"

    prisonVisitBookerRegistryMockServer.stubGetBookerByBookerReference(bookerReference, null, HttpStatus.NOT_FOUND)

    // When
    val responseSpec = callGetSocialContactsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, incorrectBookerReference, prisonerId)

    // Then
    responseSpec.expectStatus().isNotFound
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getBookerByBookerReference(incorrectBookerReference)
    verify(prisonerContactRegistryClientSpy, times(0)).getPrisonersApprovedSocialContacts(any(), any(), anyOrNull())
    verify(visitSchedulerClientSpy, times(0)).findLastApprovedDateForVisitor(any(), any())

    assertErrorResult(responseSpec, HttpStatus.NOT_FOUND, "booker not found on booker-registry for booker reference - $incorrectBookerReference")
  }

  @Test
  fun `when prisoner not associated with booker on booker registry then a NOT_FOUND error is returned`() {
    // Given
    val incorrectPrisonerId = "incorrect-prisoner-id"

    val prisoner1Dto = PermittedPrisonerForBookerDto(
      prisonerId,
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
    val responseSpec = callGetSocialContactsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, bookerReference, incorrectPrisonerId)

    // Then
    responseSpec.expectStatus().isNotFound
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getBookerByBookerReference(bookerReference)
    verify(prisonerContactRegistryClientSpy, times(0)).getPrisonersApprovedSocialContacts(any(), any(), anyOrNull())
    verify(visitSchedulerClientSpy, times(0)).findLastApprovedDateForVisitor(any(), any())
    assertErrorResult(responseSpec, HttpStatus.NOT_FOUND, "Prisoner with number - $incorrectPrisonerId not found for booker reference - $bookerReference")
  }

  @Test
  fun `when booker registry returns an internal server error, then internal server error is thrown upwards to caller`() {
    // Given
    val bookerReference = "abc-def-ghi"

    prisonVisitBookerRegistryMockServer.stubGetBookerByBookerReference(bookerReference, null, HttpStatus.INTERNAL_SERVER_ERROR)

    // When
    val responseSpec = callGetSocialContactsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, bookerReference, prisonerId)

    // Then
    responseSpec.expectStatus().is5xxServerError
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getBookerByBookerReference(bookerReference)
    verify(prisonerContactRegistryClientSpy, times(0)).getPrisonersApprovedSocialContacts(any(), any(), anyOrNull())
    verify(visitSchedulerClientSpy, times(0)).findLastApprovedDateForVisitor(any(), any())
  }

  @Test
  fun `when prisoner contact registry returns NOT_FOUND error then no visitor details are returned`() {
    // Given
    val prisoner1Dto = PermittedPrisonerForBookerDto(
      prisonerId,
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
    val responseSpec = callGetSocialContactsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, bookerReference, prisonerId)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(0)
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getBookerByBookerReference(bookerReference)
    verify(prisonerContactRegistryClientSpy, times(1)).getPrisonersApprovedSocialContacts(prisonerId, withAddress = false, hasDateOfBirth = null)
    verify(visitSchedulerClientSpy, times(0)).findLastApprovedDateForVisitor(any(), any())
  }

  @Test
  fun `when prisoner contact registry returns INTERNAL_SERVER_ERROR then INTERNAL_SERVER_ERROR is thrown upwards to caller`() {
    // Given
    // Given
    val prisoner1Dto = PermittedPrisonerForBookerDto(
      prisonerId,
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
    val responseSpec = callGetSocialContactsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, bookerReference, prisonerId)

    // Then
    responseSpec.expectStatus().is5xxServerError
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getBookerByBookerReference(bookerReference)
    verify(prisonerContactRegistryClientSpy, times(1)).getPrisonersApprovedSocialContacts(any(), any(), anyOrNull())
    verify(visitSchedulerClientSpy, times(0)).findLastApprovedDateForVisitor(any(), any())
  }

  @Test
  fun `when visit scheduler returns NOT_FOUND error then no visitor details are returned`() {
    // Given
    val prisoner1Dto = PermittedPrisonerForBookerDto(prisonerId, prisonCode, emptyList())

    val booker = BookerInfoDto(
      reference = bookerReference,
      email = "test@test.com",
      createdTimestamp = LocalDateTime.now().minusMonths(1),
      permittedPrisoners = listOf(prisoner1Dto),
    )

    val contactsList = createContactsList(listOf(visitor1, visitor2, visitor3, visitor4, visitor5))
    val lastApprovedDatesList = null

    prisonVisitBookerRegistryMockServer.stubGetBookerByBookerReference(bookerReference, booker)
    prisonerContactRegistryMockServer.stubGetApprovedPrisonerContacts(prisonerId, withAddress = false, hasDateOfBirth = null, contactsList)
    // call to visit-scheduler returns a 404
    visitSchedulerMockServer.stubGetVisitorsLastApprovedDates(prisonerId, listOf(visitor1.personId, visitor2.personId, visitor3.personId, visitor4.personId, visitor5.personId), lastApprovedDatesList, HttpStatus.NOT_FOUND)

    // When
    val responseSpec = callGetSocialContactsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, bookerReference, prisonerId)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    // only the unlinked visitors will be returned
    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(5)
    assertSocialContacts(prisonerDetailsList[0], visitor1, null)
    assertSocialContacts(prisonerDetailsList[1], visitor2, null)
    assertSocialContacts(prisonerDetailsList[2], visitor3, null)
    assertSocialContacts(prisonerDetailsList[3], visitor4, null)
    assertSocialContacts(prisonerDetailsList[4], visitor5, null)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getBookerByBookerReference(bookerReference)
    verify(prisonerContactRegistryClientSpy, times(1)).getPrisonersApprovedSocialContacts(prisonerId, withAddress = false, hasDateOfBirth = null)
    verify(visitSchedulerClientSpy, times(1)).findLastApprovedDateForVisitor(prisonerId, listOf(visitor1.personId, visitor2.personId, visitor3.personId, visitor4.personId, visitor5.personId))
  }

  @Test
  fun `when visit scheduler returns INTERNAL_SERVER_ERROR then INTERNAL_SERVER_ERROR is thrown upwards to caller`() {
    // Given
    // Given
    val prisoner1Dto = PermittedPrisonerForBookerDto(
      prisonerId,
      prisonCode,
      emptyList(),
    )

    val booker = BookerInfoDto(
      reference = bookerReference,
      email = "test@test.com",
      createdTimestamp = LocalDateTime.now().minusMonths(1),
      permittedPrisoners = listOf(prisoner1Dto),
    )

    // When
    val contactsList = createContactsList(listOf(visitor1, visitor2, visitor3, visitor4, visitor5))
    val lastApprovedDatesList = null

    prisonVisitBookerRegistryMockServer.stubGetBookerByBookerReference(bookerReference, booker)
    prisonerContactRegistryMockServer.stubGetApprovedPrisonerContacts(prisonerId, withAddress = false, hasDateOfBirth = null, contactsList)
    // call to visit-scheduler returns a 500 error
    visitSchedulerMockServer.stubGetVisitorsLastApprovedDates(prisonerId, listOf(visitor1.personId, visitor2.personId, visitor3.personId, visitor4.personId, visitor5.personId), lastApprovedDatesList, HttpStatus.INTERNAL_SERVER_ERROR)

    val responseSpec = callGetSocialContactsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, bookerReference, prisonerId)

    // Then
    responseSpec.expectStatus().is5xxServerError
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getBookerByBookerReference(bookerReference)
    verify(prisonerContactRegistryClientSpy, times(1)).getPrisonersApprovedSocialContacts(any(), any(), anyOrNull())
    verify(visitSchedulerClientSpy, times(1)).findLastApprovedDateForVisitor(any(), any())
  }

  @Test
  fun `when endpoint is called without token then UNAUTHORIZED status is returned`() {
    // Given
    val bookerReference = "abc-def-ghi"

    // When
    val responseSpec = webTestClient.get().uri(PUBLIC_BOOKER_GET_SOCIAL_CONTACTS_BY_PRISONER_PATH.replace("{bookerReference}", bookerReference).replace("{prisonerId}", prisonerId))
      .exchange()

    // Then
    responseSpec.expectStatus().isUnauthorized
  }

  private fun assertSocialContacts(socialContactsDto: SocialContactsDto, visitor: VisitorDetails, lastApprovedVisitDate: LocalDate?) {
    Assertions.assertThat(socialContactsDto.visitorId).isEqualTo(visitor.personId)
    Assertions.assertThat(socialContactsDto.firstName).isEqualTo(visitor.firstName)
    Assertions.assertThat(socialContactsDto.lastName).isEqualTo(visitor.lastName)
    Assertions.assertThat(socialContactsDto.dateOfBirth).isEqualTo(visitor.dateOfBirth)
    Assertions.assertThat(socialContactsDto.lastApprovedForVisitDate).isEqualTo(lastApprovedVisitDate)
  }

  private fun assertErrorResult(
    responseSpec: WebTestClient.ResponseSpec,
    httpStatusCode: HttpStatusCode = HttpStatusCode.valueOf(org.apache.http.HttpStatus.SC_BAD_REQUEST),
    errorMessage: String? = null,
  ) {
    responseSpec.expectStatus().isEqualTo(httpStatusCode)
    errorMessage?.let {
      val errorResponse =
        TestObjectMapper.mapper.readValue(responseSpec.expectBody().returnResult().responseBody, ErrorResponse::class.java)
      Assertions.assertThat(errorResponse.developerMessage).isEqualTo(errorMessage)
    }
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): List<SocialContactsDto> = TestObjectMapper.mapper.readValue(returnResult.returnResult().responseBody, Array<SocialContactsDto>::class.java).toList()

  fun callGetSocialContactsByBookersPrisoner(
    webTestClient: WebTestClient,
    authHttpHeaders: (HttpHeaders) -> Unit,
    bookerReference: String,
    prisonerId: String,
  ): WebTestClient.ResponseSpec = webTestClient.get().uri(PUBLIC_BOOKER_GET_SOCIAL_CONTACTS_BY_PRISONER_PATH.replace("{bookerReference}", bookerReference).replace("{prisonerId}", prisonerId))
    .headers(authHttpHeaders)
    .exchange()
}
