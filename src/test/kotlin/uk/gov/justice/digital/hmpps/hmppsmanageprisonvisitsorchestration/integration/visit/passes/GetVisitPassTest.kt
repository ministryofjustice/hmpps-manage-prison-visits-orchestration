package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.visit.passes

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller.VISIT_PASS_BY_VISIT_REFERENCE_CONTROLLER_PATH
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.ContactWithOptionalPrisonerRelationshipDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.manage.users.UserExtendedDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionScheduleWithDateExclusionsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.StaffUsernameDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitorDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitStatus
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.passes.VisitPassDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.passes.VisitPassVisitorDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.prisons.ExcludeDateDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.TestObjectMapper
import java.time.LocalDate
import java.time.LocalTime

@DisplayName("Get Visit pass for an individual visit")
class GetVisitPassTest : IntegrationTestBase() {
  val prisonCode = "HEI"

  val prisoner = createPrisoner(prisonerId = "prisoneBB1234EFrId", firstName = "John", lastName = "Smith", dateOfBirth = LocalDate.of(2000, 1, 1), prisonId = prisonCode, convictedStatus = "Convicted")

  val contact1 = createContactWithOptionalPrisonerRelationshipDto(firstName = "Jim", lastName = "Bim", dateOfBirth = LocalDate.of(1980, 3, 11), address = createAddressDto(street = "ABX Lane", primary = true))
  val contact2 = createContactWithOptionalPrisonerRelationshipDto(firstName = "Abc", lastName = "Res", dateOfBirth = null, address = null)

  lateinit var visit: VisitDto

  @BeforeEach
  fun setupData() {
    // visit with 2 contacts (ids 1 and 2)
    val visitors = createVisitors(listOf(contact1.contactId, contact2.contactId))
    visit = createVisitDto(reference = "visit-1", prisonerId = prisoner.prisonerNumber, visitors = visitors, prisonCode = prisonCode, startTimestamp = LocalDate.now().atTime(10, 0), endTimestamp = LocalDate.now().atTime(11, 0))

    visitSchedulerMockServer.stubGetExcludeDates(prisonCode, emptyList())
    visitSchedulerMockServer.stubGetSessionSchedulesWithDateExclusions(prisonCode, emptyList())
  }

  @Test
  fun `when a visit reference is found for the prison then a visit pass is returned with details populated`() {
    // Given
    val visitContacts = listOf(contact1, contact2)

    visitSchedulerMockServer.stubGetVisit(visit.reference, visit)
    prisonerContactRegistryMockServer.stubSearchContacts(contactIds = visitContacts.map { it.contactId }.distinct(), contactsList = visitContacts)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId = prisoner.prisonerNumber, prisoner = prisoner)

    // When
    val responseSpec = callGetVisitPass(webTestClient, prisonCode, visit.reference, "STAFF_USER", roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val visitPass = getResult(responseSpec.expectBody())

    assertVisitPassDetails(visit, prisoner, listOf(contact1, contact2), visitPass)
    verify(visitSchedulerClientSpy, times(1)).getVisitByReference(any())
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(any())
    verify(prisonerContactRegistryClientSpy, times(1)).searchContactsAsMono(any(), anyOrNull(), any())

    verify(telemetryClientSpy).trackEvent(
      eq("print-visit-pass"),
      check {
        assertThat(it["prisonCode"]).isEqualTo(prisonCode)
        assertThat(it["visitReference"]).isEqualTo(visit.reference)
        assertThat(it["actionedBy"]).isEqualTo("STAFF_USER")
      },
      isNull(),
    )
  }

  @Test
  fun `when a visit reference is found but is not in the same prison then BAD_REQUEST error is thrown`() {
    // Given
    val visitors = createVisitors(listOf(contact1.contactId, contact2.contactId))
    val visitContacts = listOf(contact1, contact2)

    // visit is in a different prison to the prison code in the request
    val visit = createVisitDto(reference = "visit-1", prisonerId = prisoner.prisonerNumber, visitors = visitors, prisonCode = "XYZ", startTimestamp = LocalDate.now().atTime(10, 0), endTimestamp = LocalDate.now().atTime(11, 0))

    visitSchedulerMockServer.stubGetVisit(visit.reference, visit)
    prisonerContactRegistryMockServer.stubSearchContacts(contactIds = visitContacts.map { it.contactId }.distinct(), contactsList = visitContacts)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId = prisoner.prisonerNumber, prisoner = prisoner)

    // When
    val responseSpec = callGetVisitPass(webTestClient, prisonCode, visit.reference, "STAFF_USER", roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isBadRequest

    verify(visitSchedulerClientSpy, times(1)).getVisitByReference(any())
    verify(prisonerSearchClientSpy, times(0)).getPrisonerByIdAsMono(any())
    verify(prisonerContactRegistryClientSpy, times(0)).searchContactsAsMono(any(), anyOrNull(), any())
    verify(telemetryClientSpy, times(0)).trackEvent(any(), anyOrNull(), anyOrNull())
  }

  @Test
  fun `when a visit reference is found but is not BOOKED then BAD_REQUEST error is thrown`() {
    // Given
    val visitors = createVisitors(listOf(contact1.contactId, contact2.contactId))
    val visitContacts = listOf(contact1, contact2)

    // visit is not BOOKED
    val visit = createVisitDto(reference = "visit-1", prisonerId = prisoner.prisonerNumber, visitors = visitors, prisonCode = prisonCode, startTimestamp = LocalDate.now().atTime(10, 0), endTimestamp = LocalDate.now().atTime(11, 0), visitStatus = VisitStatus.CANCELLED)

    visitSchedulerMockServer.stubGetVisit(visit.reference, visit)
    prisonerContactRegistryMockServer.stubSearchContacts(contactIds = visitContacts.map { it.contactId }.distinct(), contactsList = visitContacts)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId = prisoner.prisonerNumber, prisoner = prisoner)

    // When
    val responseSpec = callGetVisitPass(webTestClient, prisonCode, visit.reference, "STAFF_USER1", roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isBadRequest

    verify(visitSchedulerClientSpy, times(1)).getVisitByReference(any())
    verify(prisonerSearchClientSpy, times(0)).getPrisonerByIdAsMono(any())
    verify(prisonerContactRegistryClientSpy, times(0)).searchContactsAsMono(any(), anyOrNull(), any())
    verify(telemetryClientSpy, times(0)).trackEvent(any(), anyOrNull(), anyOrNull())
  }

  @Test
  fun `when a visit reference is found but is on a blocked date then BAD_REQUEST error is thrown`() {
    // Given
    val blockedDate = ExcludeDateDto(visit.startTimestamp.toLocalDate(), "user-1")

    visitSchedulerMockServer.stubGetVisit(visit.reference, visit)
    visitSchedulerMockServer.stubGetExcludeDates(prisonCode, listOf(blockedDate))
    manageUsersApiMockServer.stubGetMultipleUserDetails(
      listOf("user-1"),
      userDetails = mapOf("user-1" to UserExtendedDetailsDto("user-1", "User", "One")),
    )

    // When
    val responseSpec = callGetVisitPass(webTestClient, prisonCode, visit.reference, "STAFF_USER1", roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isBadRequest

    verify(visitSchedulerClientSpy, times(1)).getVisitByReference(any())
    verify(prisonerSearchClientSpy, times(0)).getPrisonerByIdAsMono(any())
    verify(prisonerContactRegistryClientSpy, times(0)).searchContactsAsMono(any(), anyOrNull(), any())
    verify(telemetryClientSpy, times(0)).trackEvent(any(), anyOrNull(), anyOrNull())
  }

  @Test
  fun `when a visit reference is found but is on a blocked session then BAD_REQUEST error is thrown`() {
    // Given
    val blockedSessionTemplateReference = "blocked-session"
    val visitors = createVisitors(listOf(contact1.contactId, contact2.contactId))
    val visit = createVisitDto(
      reference = "visit-1",
      prisonerId = prisoner.prisonerNumber,
      visitors = visitors,
      prisonCode = prisonCode,
      startTimestamp = LocalDate.now().atTime(10, 0),
      endTimestamp = LocalDate.now().atTime(11, 0),
      sessionTemplateReference = blockedSessionTemplateReference,
    )
    val blockedSessionExcludeDate = ExcludeDateDto(visit.startTimestamp.toLocalDate(), "user-1")
    val blockedSessionSchedule = createSessionScheduleDto(
      reference = blockedSessionTemplateReference,
      startTime = LocalTime.of(10, 0),
      endTime = LocalTime.of(11, 0),
      validFromDate = visit.startTimestamp.toLocalDate().minusWeeks(1),
      areLocationGroupsInclusive = true,
      areCategoryGroupsInclusive = true,
      areIncentiveGroupsInclusive = true,
      visitRoom = "Visit Room",
    )
    val sessionScheduleWithDateExclusions = SessionScheduleWithDateExclusionsDto(
      sessionSchedule = blockedSessionSchedule,
      excludeDates = listOf(blockedSessionExcludeDate),
    )

    visitSchedulerMockServer.stubGetVisit(visit.reference, visit)
    visitSchedulerMockServer.stubGetSessionSchedulesWithDateExclusions(prisonCode, listOf(sessionScheduleWithDateExclusions))
    manageUsersApiMockServer.stubGetMultipleUserDetails(
      listOf("user-1"),
      userDetails = mapOf("user-1" to UserExtendedDetailsDto("user-1", "User", "One")),
    )

    // When
    val responseSpec = callGetVisitPass(webTestClient, prisonCode, visit.reference, "STAFF_USER1", roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isBadRequest

    verify(visitSchedulerClientSpy, times(1)).getVisitByReference(any())
    verify(prisonerSearchClientSpy, times(0)).getPrisonerByIdAsMono(any())
    verify(prisonerContactRegistryClientSpy, times(0)).searchContactsAsMono(any(), anyOrNull(), any())
    verify(telemetryClientSpy, times(0)).trackEvent(any(), anyOrNull(), anyOrNull())
  }

  @Test
  fun `when prison exclude date lookup returns an INTERNAL_SERVER_ERROR then an INTERNAL_SERVER_ERROR error is returned`() {
    // Given
    visitSchedulerMockServer.stubGetVisit(visit.reference, visit)
    visitSchedulerMockServer.stubGetExcludeDates(prisonCode, null, HttpStatus.INTERNAL_SERVER_ERROR)

    // When
    val responseSpec = callGetVisitPass(webTestClient, prisonCode, visit.reference, "STAFF_USER1", roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().is5xxServerError

    verify(visitSchedulerClientSpy, times(1)).getVisitByReference(any())
    verify(prisonerSearchClientSpy, times(0)).getPrisonerByIdAsMono(any())
    verify(prisonerContactRegistryClientSpy, times(0)).searchContactsAsMono(any(), anyOrNull(), any())
    verify(telemetryClientSpy, times(0)).trackEvent(any(), anyOrNull(), anyOrNull())
  }

  @Test
  fun `when session exclude date lookup returns an INTERNAL_SERVER_ERROR then an INTERNAL_SERVER_ERROR error is returned`() {
    // Given
    visitSchedulerMockServer.stubGetVisit(visit.reference, visit)
    visitSchedulerMockServer.stubGetSessionSchedulesWithDateExclusions(prisonCode, null, HttpStatus.INTERNAL_SERVER_ERROR)

    // When
    val responseSpec = callGetVisitPass(webTestClient, prisonCode, visit.reference, "STAFF_USER1", roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().is5xxServerError

    verify(visitSchedulerClientSpy, times(1)).getVisitByReference(any())
    verify(prisonerSearchClientSpy, times(0)).getPrisonerByIdAsMono(any())
    verify(prisonerContactRegistryClientSpy, times(0)).searchContactsAsMono(any(), anyOrNull(), any())
    verify(telemetryClientSpy, times(0)).trackEvent(any(), anyOrNull(), anyOrNull())
  }

  @Test
  fun `when a visit reference is not found for the given visit reference then a NOT_FOUND error is returned`() {
    // Given
    val visitReference = "visit-1-ref"

    // visit not found
    visitSchedulerMockServer.stubGetVisit(visitReference, null)

    // When
    val responseSpec = callGetVisitPass(webTestClient, prisonCode, visitReference, "STAFF_USER1", roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
    verify(visitSchedulerClientSpy, times(1)).getVisitByReference(any())
    verify(prisonerSearchClientSpy, times(0)).getPrisonerByIdAsMono(any())
    verify(prisonerContactRegistryClientSpy, times(0)).searchContactsAsMono(any(), anyOrNull(), any())
    verify(telemetryClientSpy, times(0)).trackEvent(any(), anyOrNull(), anyOrNull())
  }

  @Test
  fun `when visit found but not all contact details are returned then an INTERNAL_SERVER_ERROR is returned`() {
    // Given
    val contacts = listOf(contact1, contact2)

    visitSchedulerMockServer.stubGetVisit(visit.reference, visit)

    // only 1 contact's details are returned
    prisonerContactRegistryMockServer.stubSearchContacts(contactIds = contacts.map { it.contactId }.distinct(), contactsList = listOf(contact1))
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId = prisoner.prisonerNumber, prisoner = prisoner)

    // When
    val responseSpec = callGetVisitPass(webTestClient, prisonCode, visit.reference, "STAFF_USER", roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().is5xxServerError
    verify(visitSchedulerClientSpy, times(1)).getVisitByReference(any())
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(any())
    verify(prisonerContactRegistryClientSpy, times(1)).searchContactsAsMono(any(), anyOrNull(), any())
    verify(telemetryClientSpy, times(0)).trackEvent(any(), anyOrNull(), anyOrNull())
  }

  @Test
  fun `when visit scheduler throws a NOT_FOUND error then a NOT_FOUND status is returned`() {
    // Given
    val visitContacts = listOf(contact1, contact2)
    val visitReference = "visit-1-ref"

    // NOT_FOUND thrown by visit scheduler
    visitSchedulerMockServer.stubGetVisit(visitReference, null, HttpStatus.NOT_FOUND)
    prisonerContactRegistryMockServer.stubSearchContacts(contactIds = visitContacts.map { it.contactId }.distinct(), contactsList = visitContacts)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId = prisoner.prisonerNumber, prisoner = prisoner)

    // When
    val responseSpec = callGetVisitPass(webTestClient, prisonCode, visitReference, "STAFF_USER", roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound

    verify(visitSchedulerClientSpy, times(1)).getVisitByReference(any())
    verify(prisonerSearchClientSpy, times(0)).getPrisonerByIdAsMono(any())
    verify(prisonerContactRegistryClientSpy, times(0)).searchContactsAsMono(any(), anyOrNull(), any())
    verify(telemetryClientSpy, times(0)).trackEvent(any(), anyOrNull(), anyOrNull())
  }

  @Test
  fun `when visit scheduler throws an INTERNAL_SERVER_ERROR error then an INTERNAL_SERVER_ERROR status is returned`() {
    // Given
    val visitContacts = listOf(contact1, contact2)

    // INTERNAL_SERVER_ERROR thrown by visit scheduler
    visitSchedulerMockServer.stubGetVisit(visit.reference, null, HttpStatus.INTERNAL_SERVER_ERROR)
    prisonerContactRegistryMockServer.stubSearchContacts(contactIds = visitContacts.map { it.contactId }.distinct(), contactsList = visitContacts)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId = prisoner.prisonerNumber, prisoner = prisoner)

    // When
    val responseSpec = callGetVisitPass(webTestClient, prisonCode, visit.reference, "STAFF_USER", roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().is5xxServerError

    verify(visitSchedulerClientSpy, times(1)).getVisitByReference(any())
    verify(prisonerSearchClientSpy, times(0)).getPrisonerByIdAsMono(any())
    verify(prisonerContactRegistryClientSpy, times(0)).searchContactsAsMono(any(), anyOrNull(), any())
    verify(telemetryClientSpy, times(0)).trackEvent(any(), anyOrNull(), anyOrNull())
  }

  @Test
  fun `when prisoner contact registry throws a NOT_FOUND error then a NOT_FOUND status is returned`() {
    // Given
    val visitContacts = listOf(contact1, contact2)

    visitSchedulerMockServer.stubGetVisit(visit.reference, visit)
    // NOT_FOUND thrown by prisoner contact registry
    prisonerContactRegistryMockServer.stubSearchContacts(contactIds = visitContacts.map { it.contactId }.distinct(), null, true, null, HttpStatus.NOT_FOUND)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId = prisoner.prisonerNumber, prisoner = prisoner)

    // When
    val responseSpec = callGetVisitPass(webTestClient, prisonCode, visit.reference, "STAFF_USER", roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound

    verify(visitSchedulerClientSpy, times(1)).getVisitByReference(any())
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(any())
    verify(prisonerContactRegistryClientSpy, times(1)).searchContactsAsMono(any(), anyOrNull(), any())
    verify(telemetryClientSpy, times(0)).trackEvent(any(), anyOrNull(), anyOrNull())
  }

  @Test
  fun `when prisoner contact registry throws a INTERNAL_SERVER_ERROR error then a INTERNAL_SERVER_ERROR status is returned`() {
    // Given
    val visitContacts = listOf(contact1, contact2)

    visitSchedulerMockServer.stubGetVisit(visit.reference, visit)
    // INTERNAL_SERVER_ERROR thrown by prisoner contact registry
    prisonerContactRegistryMockServer.stubSearchContacts(contactIds = visitContacts.map { it.contactId }.distinct(), null, true, null, HttpStatus.INTERNAL_SERVER_ERROR)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId = prisoner.prisonerNumber, prisoner = prisoner)

    // When
    val responseSpec = callGetVisitPass(webTestClient, prisonCode, visit.reference, "STAFF_USER", roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().is5xxServerError

    verify(visitSchedulerClientSpy, times(1)).getVisitByReference(any())
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(any())
    verify(prisonerContactRegistryClientSpy, times(1)).searchContactsAsMono(any(), anyOrNull(), any())
    verify(telemetryClientSpy, times(0)).trackEvent(any(), anyOrNull(), anyOrNull())
  }

  @Test
  fun `when prisoner search throws a NOT_FOUND error then a NOT_FOUND status is returned`() {
    // Given
    val visitContacts = listOf(contact1, contact2)

    visitSchedulerMockServer.stubGetVisit(visit.reference, visit)
    prisonerContactRegistryMockServer.stubSearchContacts(contactIds = visitContacts.map { it.contactId }.distinct(), null, true, visitContacts)
    // NOT_FOUND thrown by prisoner search
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId = prisoner.prisonerNumber, prisoner = null, HttpStatus.NOT_FOUND)

    // When
    val responseSpec = callGetVisitPass(webTestClient, prisonCode, visit.reference, "STAFF_USER", roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound

    verify(visitSchedulerClientSpy, times(1)).getVisitByReference(any())
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(any())
    verify(prisonerContactRegistryClientSpy, times(1)).searchContactsAsMono(any(), anyOrNull(), any())
    verify(telemetryClientSpy, times(0)).trackEvent(any(), anyOrNull(), anyOrNull())
  }

  @Test
  fun `when prisoner search throws an INTERNAL_SERVER_ERROR error then an INTERNAL_SERVER_ERROR status is returned`() {
    // Given
    val visitContacts = listOf(contact1, contact2)

    visitSchedulerMockServer.stubGetVisit(visit.reference, visit)
    prisonerContactRegistryMockServer.stubSearchContacts(contactIds = visitContacts.map { it.contactId }.distinct(), null, true, visitContacts)
    // INTERNAL_SERVER_ERROR thrown by prisoner search
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId = prisoner.prisonerNumber, null, HttpStatus.INTERNAL_SERVER_ERROR)

    // When
    val responseSpec = callGetVisitPass(webTestClient, prisonCode, visit.reference, "STAFF_USER", roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().is5xxServerError

    verify(visitSchedulerClientSpy, times(1)).getVisitByReference(any())
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(any())
    verify(prisonerContactRegistryClientSpy, times(1)).searchContactsAsMono(any(), anyOrNull(), any())
    verify(telemetryClientSpy, times(0)).trackEvent(any(), anyOrNull(), anyOrNull())
  }

  private fun callGetVisitPass(
    webTestClient: WebTestClient,
    prisonCode: String,
    visitReference: String,
    actionedBy: String,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    val actionedByDto = StaffUsernameDto(actionedBy)
    return webTestClient.post()
      .uri(VISIT_PASS_BY_VISIT_REFERENCE_CONTROLLER_PATH.replace("{prisonId}", prisonCode).replace("{visitReference}", visitReference))
      .body(BodyInserters.fromValue(actionedByDto))
      .headers(authHttpHeaders)
      .exchange()
  }

  private fun createVisitors(visitorId: List<Long>): List<VisitorDto> = visitorId.map { createVisitor(it) }

  private fun createVisitor(
    visitorId: Long,
  ): VisitorDto = VisitorDto(visitorId, false)

  private fun getResult(returnResult: WebTestClient.BodyContentSpec): VisitPassDto = TestObjectMapper.mapper.readValue(returnResult.returnResult().responseBody, VisitPassDto::class.java)

  private fun assertVisitPassDetails(
    visitDto: VisitDto,
    prisonerDto: PrisonerDto,
    contactDtoList: List<ContactWithOptionalPrisonerRelationshipDto>,
    visitPassDto: VisitPassDto,
  ) {
    assertThat(visitPassDto.visitDate).isEqualTo(visitDto.startTimestamp.toLocalDate())
    assertThat(visitPassDto.startTime).isEqualTo(visitDto.startTimestamp.toLocalTime())
    assertThat(visitPassDto.endTime).isEqualTo(visitDto.endTimestamp.toLocalTime())
    assertThat(visitPassDto.prisonerId).isEqualTo(visitDto.prisonerId)
    assertThat(visitPassDto.prisonerId).isEqualTo(prisonerDto.prisonerNumber)
    assertThat(visitPassDto.prisonerFirstName).isEqualTo(prisonerDto.firstName)
    assertThat(visitPassDto.prisonerLastName).isEqualTo(prisonerDto.lastName)
    assertThat(visitPassDto.visitRestriction).isEqualTo(visitDto.visitRestriction)
    assertThat(visitPassDto.visitors.size).isEqualTo(contactDtoList.size)
    contactDtoList.forEach { contactDto ->
      assertThat(visitPassDto.visitors.any { it.nomisPersonId == contactDto.contactId }).isTrue
      assertVisitors(contactDto, visitPassDto.visitors.first { it.nomisPersonId == contactDto.contactId })
    }
  }

  private fun assertVisitors(
    contactDto: ContactWithOptionalPrisonerRelationshipDto,
    visitPassVisitorDto: VisitPassVisitorDto,
  ) {
    assertThat(visitPassVisitorDto.firstName).isEqualTo(contactDto.firstName)
    assertThat(visitPassVisitorDto.lastName).isEqualTo(contactDto.lastName)
    assertThat(visitPassVisitorDto.dateOfBirth).isEqualTo(contactDto.dateOfBirth)
    assertThat(visitPassVisitorDto.address).isEqualTo(contactDto.address)
  }
}
