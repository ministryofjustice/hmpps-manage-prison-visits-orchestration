package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.visit

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller.GET_VISIT_FULL_DETAILS_BY_VISIT_REFERENCE
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.alerts.api.AlertDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.alerts.api.AlertResponseDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.AddressDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.PrisonerContactDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.EventAuditOrchestrationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.PrisonerDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.VisitBookingDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.VisitContactDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.VisitNotificationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.VisitorDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.OffenderRestrictionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.OffenderRestrictionsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.register.PrisonRegisterPrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.ActionedByDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.ContactDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.EventAuditDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.ApplicationMethodType.EMAIL
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.ApplicationMethodType.NOT_APPLICABLE
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.EventAuditType.BOOKED_VISIT
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.EventAuditType.CANCELLED_VISIT
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.EventAuditType.PRISONER_RESTRICTION_CHANGE_EVENT
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.EventAuditType.UPDATED_VISIT
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.NotificationEventAttributeType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType.PUBLIC
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType.STAFF
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType.SYSTEM
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.NotificationEventType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.VisitNotificationEventAttributeDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.VisitNotificationEventDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import java.time.LocalDate

@DisplayName("Test for $GET_VISIT_FULL_DETAILS_BY_VISIT_REFERENCE")
class GetVisitBookingDetailsTest : IntegrationTestBase() {
  private val prisonCode = "MDI"
  private val prisonerId = "prisoner-id"

  private lateinit var prisonerDto: PrisonerDto

  private lateinit var offenderRestrictions: OffenderRestrictionsDto

  private lateinit var visitor1: PrisonerContactDto
  private lateinit var visitor1PrimaryAddress: AddressDto
  private lateinit var visitor2: PrisonerContactDto
  private lateinit var visitor3: PrisonerContactDto

  private lateinit var prison: PrisonRegisterPrisonDto
  private lateinit var alert1: AlertResponseDto
  private lateinit var alert2: AlertResponseDto
  private lateinit var alert3: AlertResponseDto
  private lateinit var restriction1: OffenderRestrictionDto

  private lateinit var actionedBy1: ActionedByDto
  private lateinit var actionedBy2: ActionedByDto
  private lateinit var actionedBy3: ActionedByDto
  private lateinit var actionedBy4: ActionedByDto
  private lateinit var actionedBy5: ActionedByDto

  private lateinit var eventAudit1: EventAuditDto
  private lateinit var eventAudit2: EventAuditDto
  private lateinit var eventAudit3: EventAuditDto
  private lateinit var eventAudit4: EventAuditDto
  private lateinit var eventAudit5: EventAuditDto

  private lateinit var notification1: VisitNotificationEventDto
  private lateinit var notification2: VisitNotificationEventDto

  private lateinit var eventAttribute1: VisitNotificationEventAttributeDto
  private lateinit var eventAttribute2: VisitNotificationEventAttributeDto

  @BeforeEach
  internal fun setup() {
    prisonerDto = createPrisoner(
      prisonerId = prisonerId,
      firstName = "FirstName",
      lastName = "LastName",
      dateOfBirth = LocalDate.of(2000, 1, 31),
      prisonId = prisonCode,
      convictedStatus = "Convicted",
    )

    visitor1PrimaryAddress = createAddressDto(street = "ABC Street", primary = true)
    val visitor1SecondaryAddress = createAddressDto(street = "XYZ Street", primary = false)
    val visitor2SecondaryAddress = createAddressDto(street = "ABC Street", primary = false)

    // visitor 1 has both primary and secondary address
    visitor1 = createContactDto(1, "First", "VisitorA", addresses = listOf(visitor1PrimaryAddress, visitor1SecondaryAddress))

    // visitor2 has only secondary address
    visitor2 = createContactDto(2, "Second", "VisitorB", addresses = listOf(visitor2SecondaryAddress))

    // visitor 3 has no addresses
    visitor3 = createContactDto(3, "Third", "VisitorC", addresses = emptyList())

    prison = PrisonRegisterPrisonDto(prisonCode, "Prison-MDI", true)

    alert1 = createAlertResponseDto(alertTypeCode = "T", code = "C1")
    alert2 = createAlertResponseDto(alertTypeCode = "T1", code = "C2")
    // this alert code is not relevant for visits
    alert3 = createAlertResponseDto(alertTypeCode = "T1", code = "TEST")

    restriction1 = OffenderRestrictionDto(restrictionId = 1, restrictionType = "CLOSED", restrictionTypeDescription = "", startDate = LocalDate.now(), expiryDate = LocalDate.now(), active = true)
    offenderRestrictions = OffenderRestrictionsDto(bookingId = 1, listOf(restriction1))

    actionedBy1 = ActionedByDto(bookerReference = null, userName = "abcd", userType = PUBLIC)
    actionedBy2 = ActionedByDto(bookerReference = null, userName = null, userType = SYSTEM)
    actionedBy3 = ActionedByDto(bookerReference = null, userName = "test-user", userType = STAFF)
    actionedBy4 = ActionedByDto(bookerReference = null, userName = "test-user1", userType = STAFF)
    actionedBy5 = ActionedByDto(bookerReference = null, userName = "test-user2", userType = STAFF)

    eventAudit1 = EventAuditDto(type = BOOKED_VISIT, actionedBy = actionedBy1, applicationMethodType = EMAIL)
    eventAudit2 = EventAuditDto(type = PRISONER_RESTRICTION_CHANGE_EVENT, actionedBy = actionedBy2, applicationMethodType = NOT_APPLICABLE)
    eventAudit3 = EventAuditDto(type = UPDATED_VISIT, actionedBy = actionedBy3, applicationMethodType = EMAIL)
    eventAudit4 = EventAuditDto(type = UPDATED_VISIT, actionedBy = actionedBy4, applicationMethodType = EMAIL)
    eventAudit5 = EventAuditDto(type = CANCELLED_VISIT, actionedBy = actionedBy5, applicationMethodType = EMAIL)

    notification1 = createNotificationEvent(NotificationEventType.NON_ASSOCIATION_EVENT)

    eventAttribute1 = VisitNotificationEventAttributeDto(NotificationEventAttributeType.VISITOR_ID, "10001")
    eventAttribute2 = VisitNotificationEventAttributeDto(NotificationEventAttributeType.VISITOR_RESTRICTION, "BAN")
    notification2 = createNotificationEvent(NotificationEventType.VISITOR_RESTRICTION_UPSERTED_EVENT, additionalData = listOf(eventAttribute1, eventAttribute2))
  }

  fun callGetVisitFullDetailsByReference(
    webTestClient: WebTestClient,
    reference: String,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec = webTestClient.get().uri("/visits/$reference/detailed")
    .headers(authHttpHeaders)
    .exchange()

  @Test
  fun `when visit exists search by reference returns the full booking details for that visit`() {
    // Given
    val reference = "aa-bb-cc-dd"
    val visitors = listOf(createVisitorDto(visitor1, false), createVisitorDto(visitor2, false), createVisitorDto(visitor3, true))
    val visit = createVisitDto(reference = reference, prisonCode = prisonCode, prisonerId = prisonerId, visitors = visitors)
    val contactsList = listOf(visitor1, visitor2, visitor3)
    val eventList = listOf(eventAudit1, eventAudit2, eventAudit3, eventAudit4, eventAudit5)
    val expectedEventActionedByFullNames = listOf("abcd", null, "Test User", "test-user1", "test-user2")
    val notifications = listOf(notification1, notification2)

    visitSchedulerMockServer.stubGetVisit(reference, visit)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisonerDto)
    prisonRegisterMockServer.stubGetPrison(prisonCode, prison)
    // alert 3's alert code is not relevant for visits so should be ignored
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, listOf(alert1, alert2, alert3))
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, offenderRestrictions)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId, withAddress = true, approvedVisitorsOnly = false, null, null, contactsList)
    visitSchedulerMockServer.stubGetVisitHistory(visit.reference, eventList)
    visitSchedulerMockServer.stubGetVisitNotificationEvents(visit.reference, notifications)
    manageUsersApiMockServer.stubGetUserDetails("test-user", "Test User")

    // When
    val responseSpec = callGetVisitFullDetailsByReference(webTestClient, reference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val visitBookingResponse = getResult(responseSpec.expectBody())
    val expectedVisitContact = VisitContactDto(
      contactDto = visit.visitContact!!,
      visitContactId = visitor3.personId,
    )
    assertVisitBookingDetails(visitBookingResponse, visit, prison, prisonerDto, listOf(alert1, alert2), offenderRestrictions, contactsList, expectedVisitContact, eventList, expectedEventActionedByFullNames, notifications)
  }

  @Test
  fun `when a visit's visitor is no longer in contact list that visitor is not returned for that visit`() {
    // Given
    val reference = "aa-bb-cc-dd"
    val prisonerId = "prisoner-id"
    val visitors = listOf(createVisitorDto(visitor1, false), createVisitorDto(visitor2, false), createVisitorDto(visitor3, true))
    val visit = createVisitDto(reference = reference, prisonCode = prisonCode, prisonerId = prisonerId, visitors = visitors)
    // contacts returned does not have visitor 3
    val contactsList = listOf(visitor1, visitor2)
    val eventList = mutableListOf(eventAudit1, eventAudit2, eventAudit3)
    val expectedEventActionedByFullNames = listOf("abcd", null, "Test User A")

    val notifications = emptyList<VisitNotificationEventDto>()

    visitSchedulerMockServer.stubGetVisit(reference, visit)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisonerDto)
    prisonRegisterMockServer.stubGetPrison(prisonCode, prison)
    // alert 3's alert code is not relevant for visits so should be ignored
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, listOf(alert1, alert2, alert3))
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, offenderRestrictions)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId = prisonerId, withAddress = true, approvedVisitorsOnly = false, personId = null, hasDateOfBirth = null, contactsList = contactsList)
    visitSchedulerMockServer.stubGetVisitHistory(visit.reference, eventList)
    visitSchedulerMockServer.stubGetVisitNotificationEvents(visit.reference, notifications)
    manageUsersApiMockServer.stubGetUserDetails("test-user", "Test User A")

    // When
    val responseSpec = callGetVisitFullDetailsByReference(webTestClient, reference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val visitBookingResponse = getResult(responseSpec.expectBody())
    val expectedVisitContact = VisitContactDto(
      contactDto = visit.visitContact!!,
      visitContactId = visitor3.personId,
    )

    assertVisitBookingDetails(visitBookingResponse, visit, prison, prisonerDto, listOf(alert1, alert2), offenderRestrictions, contactsList, expectedVisitContact, eventList, expectedEventActionedByFullNames, notifications)
  }

  @Test
  fun `when a visit's contact details are null then visitContactDetails are also returned as null for that visit`() {
    // Given
    val reference = "aa-bb-cc-dd"
    val prisonerId = "prisoner-id"
    val visitors = listOf(createVisitorDto(visitor1, false), createVisitorDto(visitor2, false), createVisitorDto(visitor3, true))

    // contact for the visit is null
    val visit = createVisitDto(reference = reference, prisonCode = prisonCode, prisonerId = prisonerId, visitors = visitors, contact = null)
    val contactsList = listOf(visitor1, visitor2, visitor3)
    val eventList = mutableListOf(eventAudit1, eventAudit2, eventAudit3)
    val expectedEventActionedByFullNames = listOf("abcd", null, "Test User A")

    val notifications = emptyList<VisitNotificationEventDto>()

    visitSchedulerMockServer.stubGetVisit(reference, visit)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisonerDto)
    prisonRegisterMockServer.stubGetPrison(prisonCode, prison)
    // alert 3's alert code is not relevant for visits so should be ignored
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, listOf(alert1, alert2, alert3))
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, offenderRestrictions)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId = prisonerId, withAddress = true, approvedVisitorsOnly = false, personId = null, hasDateOfBirth = null, contactsList = contactsList)
    visitSchedulerMockServer.stubGetVisitHistory(visit.reference, eventList)
    visitSchedulerMockServer.stubGetVisitNotificationEvents(visit.reference, notifications)
    manageUsersApiMockServer.stubGetUserDetails("test-user", "Test User A")

    // When
    val responseSpec = callGetVisitFullDetailsByReference(webTestClient, reference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val visitBookingResponse = getResult(responseSpec.expectBody())
    assertVisitBookingDetails(visitBookingResponse, visit, prison, prisonerDto, listOf(alert1, alert2), offenderRestrictions, contactsList, expectedVisitContact = null, eventList, expectedEventActionedByFullNames, notifications)
  }

  @Test
  fun `when a visit's contact is not from the contact list then visitContactDetails are populated without contactId for that visit`() {
    // Given
    val reference = "aa-bb-cc-dd"
    val prisonerId = "prisoner-id"

    // none of the 3 visitors are main contacts
    val visitors = listOf(createVisitorDto(visitor1, false), createVisitorDto(visitor2, false), createVisitorDto(visitor3, false))

    // main contact details
    val contact = ContactDto("Johnny Doe", "01234567890", "email@example.com")
    val visit = createVisitDto(reference = reference, prisonCode = prisonCode, prisonerId = prisonerId, visitors = visitors, contact = contact)
    // contacts returned does not have visitor 3
    val contactsList = listOf(visitor1, visitor2)
    val eventList = mutableListOf(eventAudit1, eventAudit2, eventAudit3)
    val expectedEventActionedByFullNames = listOf("abcd", null, "Test User A")

    val notifications = emptyList<VisitNotificationEventDto>()

    visitSchedulerMockServer.stubGetVisit(reference, visit)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisonerDto)
    prisonRegisterMockServer.stubGetPrison(prisonCode, prison)
    // alert 3's alert code is not relevant for visits so should be ignored
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, listOf(alert1, alert2, alert3))
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, offenderRestrictions)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId = prisonerId, withAddress = true, approvedVisitorsOnly = false, personId = null, hasDateOfBirth = null, contactsList = contactsList)
    visitSchedulerMockServer.stubGetVisitHistory(visit.reference, eventList)
    visitSchedulerMockServer.stubGetVisitNotificationEvents(visit.reference, notifications)
    manageUsersApiMockServer.stubGetUserDetails("test-user", "Test User A")

    // When
    val responseSpec = callGetVisitFullDetailsByReference(webTestClient, reference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val visitBookingResponse = getResult(responseSpec.expectBody())
    val expectedVisitContact = VisitContactDto(
      contactDto = visit.visitContact!!,
      visitContactId = null,
    )
    assertVisitBookingDetails(visitBookingResponse, visit, prison, prisonerDto, listOf(alert1, alert2), offenderRestrictions, contactsList, expectedVisitContact, eventList, expectedEventActionedByFullNames, notifications)
  }

  @Test
  fun `when prisoner search returns a 404 then an exception is thrown and a 404 is returned as response `() {
    // Given
    val reference = "aa-bb-cc-dd"
    val prisonerId = "prisoner-id"
    val visitors = listOf(createVisitorDto(visitor1, false), createVisitorDto(visitor2, false), createVisitorDto(visitor3, true))
    val visit = createVisitDto(reference = reference, prisonCode = prisonCode, prisonerId = prisonerId, visitors = visitors)
    val contactsList = listOf(visitor1, visitor2, visitor3)
    val eventList = mutableListOf(eventAudit3)

    val notifications = emptyList<VisitNotificationEventDto>()

    visitSchedulerMockServer.stubGetVisit(reference, visit)
    // prisoner search returns a 404
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, null, HttpStatus.NOT_FOUND)
    prisonRegisterMockServer.stubGetPrison(prisonCode, prison)
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, listOf(alert1, alert2, alert3))
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, offenderRestrictions)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId, withAddress = true, approvedVisitorsOnly = false, null, null, contactsList)
    visitSchedulerMockServer.stubGetVisitHistory(visit.reference, eventList)
    visitSchedulerMockServer.stubGetVisitNotificationEvents(visit.reference, notifications)

    // When
    val responseSpec = callGetVisitFullDetailsByReference(webTestClient, reference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
  }

  @Test
  fun `when prisoner search returns a 500 then an exception is thrown and a 500 is returned as response`() {
    // Given
    val reference = "aa-bb-cc-dd"
    val prisonerId = "prisoner-id"
    val visitors = listOf(createVisitorDto(visitor1, false), createVisitorDto(visitor2, false), createVisitorDto(visitor3, true))
    val visit = createVisitDto(reference = reference, prisonCode = prisonCode, prisonerId = prisonerId, visitors = visitors)
    val contactsList = listOf(visitor1, visitor2, visitor3)
    val eventList = mutableListOf(eventAudit1)
    val notifications = emptyList<VisitNotificationEventDto>()

    visitSchedulerMockServer.stubGetVisit(reference, visit)
    // prisoner search returns a 500
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, null, HttpStatus.INTERNAL_SERVER_ERROR)
    prisonRegisterMockServer.stubGetPrison(prisonCode, prison)
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, listOf(alert1, alert2, alert3))
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, offenderRestrictions)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId, withAddress = true, approvedVisitorsOnly = false, null, null, contactsList)
    visitSchedulerMockServer.stubGetVisitHistory(visit.reference, eventList)
    visitSchedulerMockServer.stubGetVisitNotificationEvents(visit.reference, notifications)

    // When
    val responseSpec = callGetVisitFullDetailsByReference(webTestClient, reference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().is5xxServerError
  }

  @Test
  fun `when prison register search returns a 404 then no exception is thrown and prison code and name both come back with prison code`() {
    // Given
    val reference = "aa-bb-cc-dd"
    val prisonerId = "prisoner-id"
    val visitors = listOf(createVisitorDto(visitor1, false), createVisitorDto(visitor2, false), createVisitorDto(visitor3, true))
    val visit = createVisitDto(reference = reference, prisonCode = prisonCode, prisonerId = prisonerId, visitors = visitors)
    val contactsList = listOf(visitor1, visitor2, visitor3)
    val eventList = mutableListOf(eventAudit1)
    val expectedEventActionedByFullNames = listOf("abcd")

    val notifications = listOf(notification1, notification2)

    visitSchedulerMockServer.stubGetVisit(reference, visit)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisonerDto)
    prisonRegisterMockServer.stubGetPrison(prisonCode, null, HttpStatus.NOT_FOUND)
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, listOf(alert1, alert2, alert3))
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, offenderRestrictions)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId, withAddress = true, approvedVisitorsOnly = false, null, null, contactsList)
    visitSchedulerMockServer.stubGetVisitHistory(visit.reference, eventList)
    visitSchedulerMockServer.stubGetVisitNotificationEvents(visit.reference, notifications)

    // When
    val responseSpec = callGetVisitFullDetailsByReference(webTestClient, reference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val visitBookingResponse = getResult(responseSpec.expectBody())

    // as prison register search returned a 404 we expect both prison code and name to have the same value of prison code
    val expectedPrison = PrisonRegisterPrisonDto(prisonCode, prisonCode, true)
    val expectedVisitContact = VisitContactDto(
      contactDto = visit.visitContact!!,
      visitContactId = visitor3.personId,
    )

    assertVisitBookingDetails(visitBookingResponse, visit, expectedPrison, prisonerDto, listOf(alert1, alert2), offenderRestrictions, contactsList, expectedVisitContact, eventList, expectedEventActionedByFullNames, notifications)
  }

  @Test
  fun `when prison register search returns a 500 an exception is thrown and a 500 is returned as response`() {
    // Given
    val reference = "aa-bb-cc-dd"
    val visitors = listOf(createVisitorDto(visitor1, false), createVisitorDto(visitor2, false), createVisitorDto(visitor3, true))
    val visit = createVisitDto(reference = reference, prisonCode = prisonCode, prisonerId = prisonerId, visitors = visitors)
    val contactsList = listOf(visitor1, visitor2, visitor3)
    val eventList = mutableListOf(eventAudit1)
    val notifications = listOf(notification1, notification2)
    visitSchedulerMockServer.stubGetVisitNotificationEvents(visit.reference, notifications)

    visitSchedulerMockServer.stubGetVisit(reference, visit)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisonerDto)
    prisonRegisterMockServer.stubGetPrison(prisonCode, null, HttpStatus.INTERNAL_SERVER_ERROR)
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, listOf(alert1, alert2, alert3))
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, offenderRestrictions)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId, withAddress = true, approvedVisitorsOnly = false, null, null, contactsList)
    visitSchedulerMockServer.stubGetVisitHistory(visit.reference, eventList)

    // When
    val responseSpec = callGetVisitFullDetailsByReference(webTestClient, reference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().is5xxServerError
  }

  @Test
  fun `when alert API returns returns a 500 an exception is thrown and a 500 is returned as response`() {
    // Given
    val reference = "aa-bb-cc-dd"
    val visitors = listOf(createVisitorDto(visitor1, false), createVisitorDto(visitor2, false), createVisitorDto(visitor3, true))
    val visit = createVisitDto(reference = reference, prisonCode = prisonCode, prisonerId = prisonerId, visitors = visitors)
    val contactsList = listOf(visitor1, visitor2, visitor3)
    val eventList = mutableListOf(eventAudit1)
    val notifications = listOf(notification1, notification2)

    visitSchedulerMockServer.stubGetVisit(reference, visit)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisonerDto)
    prisonRegisterMockServer.stubGetPrison(prisonCode, prison)
    // alert API returns a 500
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, null, HttpStatus.INTERNAL_SERVER_ERROR)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, offenderRestrictions)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId, withAddress = true, approvedVisitorsOnly = false, null, null, contactsList)
    visitSchedulerMockServer.stubGetVisitHistory(visit.reference, eventList)
    visitSchedulerMockServer.stubGetVisitNotificationEvents(visit.reference, notifications)

    // When
    val responseSpec = callGetVisitFullDetailsByReference(webTestClient, reference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().is5xxServerError
  }

  @Test
  fun `when prison API get restrictions call returns returns a 404 an exception is thrown and a 404 is returned as response`() {
    // Given
    val reference = "aa-bb-cc-dd"
    val visitors = listOf(createVisitorDto(visitor1, false), createVisitorDto(visitor2, false), createVisitorDto(visitor3, true))
    val visit = createVisitDto(reference = reference, prisonCode = prisonCode, prisonerId = prisonerId, visitors = visitors)
    val contactsList = listOf(visitor1, visitor2, visitor3)
    val eventList = mutableListOf(eventAudit1)
    val notifications = listOf(notification1, notification2)

    visitSchedulerMockServer.stubGetVisit(reference, visit)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisonerDto)
    prisonRegisterMockServer.stubGetPrison(prisonCode, prison)
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, listOf(alert1, alert2, alert3))
    // prison API returns a 404
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, null, HttpStatus.NOT_FOUND)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId, withAddress = true, approvedVisitorsOnly = false, null, null, contactsList)
    visitSchedulerMockServer.stubGetVisitHistory(visit.reference, eventList)
    visitSchedulerMockServer.stubGetVisitNotificationEvents(visit.reference, notifications)

    // When
    val responseSpec = callGetVisitFullDetailsByReference(webTestClient, reference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
  }

  @Test
  fun `when prison API get restrictions call returns returns a 500 an exception is thrown and a 500 is returned as response`() {
    // Given
    val reference = "aa-bb-cc-dd"
    val visitors = listOf(createVisitorDto(visitor1, false), createVisitorDto(visitor2, false), createVisitorDto(visitor3, true))
    val visit = createVisitDto(reference = reference, prisonCode = prisonCode, prisonerId = prisonerId, visitors = visitors)
    val contactsList = listOf(visitor1, visitor2, visitor3)
    val eventList = mutableListOf(eventAudit1)
    val notifications = listOf(notification1, notification2)

    visitSchedulerMockServer.stubGetVisit(reference, visit)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisonerDto)
    prisonRegisterMockServer.stubGetPrison(prisonCode, prison)
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, listOf(alert1, alert2, alert3))
    // prison API returns a 404
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, null, HttpStatus.INTERNAL_SERVER_ERROR)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId, withAddress = true, approvedVisitorsOnly = false, null, null, contactsList)
    visitSchedulerMockServer.stubGetVisitHistory(visit.reference, eventList)
    visitSchedulerMockServer.stubGetVisitNotificationEvents(visit.reference, notifications)

    // When
    val responseSpec = callGetVisitFullDetailsByReference(webTestClient, reference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().is5xxServerError
  }

  @Test
  fun `when prisoner contact registry API call returns returns a 404 an exception is thrown and a 404 is returned as response`() {
    // Given
    val reference = "aa-bb-cc-dd"
    val visitors = listOf(createVisitorDto(visitor1, false), createVisitorDto(visitor2, false), createVisitorDto(visitor3, true))
    val visit = createVisitDto(reference = reference, prisonCode = prisonCode, prisonerId = prisonerId, visitors = visitors)
    val eventList = mutableListOf(eventAudit1)
    val notifications = listOf(notification1, notification2)

    visitSchedulerMockServer.stubGetVisit(reference, visit)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisonerDto)
    prisonRegisterMockServer.stubGetPrison(prisonCode, prison)
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, listOf(alert1, alert2, alert3))
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, offenderRestrictions)
    // prisoner contact registry API returns a 404
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId, withAddress = true, approvedVisitorsOnly = false, null, null, null, HttpStatus.NOT_FOUND)
    visitSchedulerMockServer.stubGetVisitHistory(visit.reference, eventList)
    visitSchedulerMockServer.stubGetVisitNotificationEvents(visit.reference, notifications)

    // When
    val responseSpec = callGetVisitFullDetailsByReference(webTestClient, reference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
  }

  @Test
  fun `when prisoner contact registry API call returns returns a 500 an exception is thrown and a 500 is returned as response`() {
    // Given
    val reference = "aa-bb-cc-dd"
    val visitors = listOf(createVisitorDto(visitor1, false), createVisitorDto(visitor2, false), createVisitorDto(visitor3, true))
    val visit = createVisitDto(reference = reference, prisonCode = prisonCode, prisonerId = prisonerId, visitors = visitors)
    val eventList = mutableListOf(eventAudit1)
    val notifications = listOf(notification1, notification2)
    visitSchedulerMockServer.stubGetVisitNotificationEvents(visit.reference, notifications)

    visitSchedulerMockServer.stubGetVisit(reference, visit)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisonerDto)
    prisonRegisterMockServer.stubGetPrison(prisonCode, prison)
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, listOf(alert1, alert2, alert3))
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, offenderRestrictions)
    // prisoner contact registry API returns a 404
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId, withAddress = true, approvedVisitorsOnly = false, null, null, null, HttpStatus.INTERNAL_SERVER_ERROR)
    visitSchedulerMockServer.stubGetVisitHistory(visit.reference, eventList)

    // When
    val responseSpec = callGetVisitFullDetailsByReference(webTestClient, reference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().is5xxServerError
  }

  @Test
  fun `when visit history API call returns returns a 404 an exception is thrown and a 404 is returned as response`() {
    // Given
    val reference = "aa-bb-cc-dd"
    val visitors = listOf(createVisitorDto(visitor1, false), createVisitorDto(visitor2, false), createVisitorDto(visitor3, true))
    val visit = createVisitDto(reference = reference, prisonCode = prisonCode, prisonerId = prisonerId, visitors = visitors)
    val contactsList = listOf(visitor1, visitor2, visitor3)
    val notifications = listOf(notification1, notification2)

    visitSchedulerMockServer.stubGetVisit(reference, visit)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisonerDto)
    prisonRegisterMockServer.stubGetPrison(prisonCode, prison)
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, listOf(alert1, alert2, alert3))
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, offenderRestrictions)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId, withAddress = true, approvedVisitorsOnly = false, null, null, contactsList)
    // visits get history - returns a 404
    visitSchedulerMockServer.stubGetVisitHistory(visit.reference, emptyList(), HttpStatus.NOT_FOUND)
    visitSchedulerMockServer.stubGetVisitNotificationEvents(visit.reference, notifications)

    // When
    val responseSpec = callGetVisitFullDetailsByReference(webTestClient, reference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
  }

  @Test
  fun `when visit history API call returns returns a 500 an exception is thrown and a 500 is returned as response`() {
    // Given
    val reference = "aa-bb-cc-dd"
    val visitors = listOf(createVisitorDto(visitor1, false), createVisitorDto(visitor2, false), createVisitorDto(visitor3, true))
    val visit = createVisitDto(reference = reference, prisonCode = prisonCode, prisonerId = prisonerId, visitors = visitors)
    val contactsList = listOf(visitor1, visitor2, visitor3)
    val notifications = listOf(notification1, notification2)

    visitSchedulerMockServer.stubGetVisit(reference, visit)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisonerDto)
    prisonRegisterMockServer.stubGetPrison(prisonCode, prison)
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, listOf(alert1, alert2, alert3))
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, offenderRestrictions)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId, withAddress = true, approvedVisitorsOnly = false, null, null, contactsList)
    // visits get history - returns a 500
    visitSchedulerMockServer.stubGetVisitHistory(visit.reference, emptyList(), HttpStatus.INTERNAL_SERVER_ERROR)
    visitSchedulerMockServer.stubGetVisitNotificationEvents(visit.reference, notifications)

    // When
    val responseSpec = callGetVisitFullDetailsByReference(webTestClient, reference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().is5xxServerError
  }

  @Test
  fun `when visit notifications API call returns returns a 404 an exception is thrown and a 404 is returned as response`() {
    // Given
    val reference = "aa-bb-cc-dd"
    val visitors = listOf(createVisitorDto(visitor1, false), createVisitorDto(visitor2, false), createVisitorDto(visitor3, true))
    val visit = createVisitDto(reference = reference, prisonCode = prisonCode, prisonerId = prisonerId, visitors = visitors)
    val contactsList = listOf(visitor1, visitor2, visitor3)
    val eventList = mutableListOf(eventAudit1)

    visitSchedulerMockServer.stubGetVisit(reference, visit)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisonerDto)
    prisonRegisterMockServer.stubGetPrison(prisonCode, prison)
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, listOf(alert1, alert2, alert3))
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, offenderRestrictions)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId, withAddress = true, approvedVisitorsOnly = false, null, null, contactsList)
    visitSchedulerMockServer.stubGetVisitHistory(visit.reference, eventList)
    // visits get notifications - returns a 404
    visitSchedulerMockServer.stubGetVisitNotificationEvents(visit.reference, null, HttpStatus.NOT_FOUND)

    // When
    val responseSpec = callGetVisitFullDetailsByReference(webTestClient, reference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
  }

  @Test
  fun `when visit notifications API call returns returns a 500 an exception is thrown and a 500 is returned as response`() {
    // Given
    val reference = "aa-bb-cc-dd"
    val visitors = listOf(createVisitorDto(visitor1, false), createVisitorDto(visitor2, false), createVisitorDto(visitor3, true))
    val visit = createVisitDto(reference = reference, prisonCode = prisonCode, prisonerId = prisonerId, visitors = visitors)
    val contactsList = listOf(visitor1, visitor2, visitor3)
    val eventList = mutableListOf(eventAudit1)

    visitSchedulerMockServer.stubGetVisit(reference, visit)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisonerDto)
    prisonRegisterMockServer.stubGetPrison(prisonCode, prison)
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, listOf(alert1, alert2, alert3))
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, offenderRestrictions)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId, withAddress = true, approvedVisitorsOnly = false, null, null, contactsList)
    visitSchedulerMockServer.stubGetVisitHistory(visit.reference, eventList)
    // visits get notifications - returns a 404
    visitSchedulerMockServer.stubGetVisitNotificationEvents(visit.reference, null, HttpStatus.INTERNAL_SERVER_ERROR)

    // When
    val responseSpec = callGetVisitFullDetailsByReference(webTestClient, reference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().is5xxServerError
  }

  @Test
  fun `when visit exists by reference but manage user search returns a 404 the full booking details for that visit is still returned`() {
    // Given
    val reference = "aa-bb-cc-dd"
    val visitors = listOf(createVisitorDto(visitor1, false), createVisitorDto(visitor2, false), createVisitorDto(visitor3, true))
    val visit = createVisitDto(reference = reference, prisonCode = prisonCode, prisonerId = prisonerId, visitors = visitors)
    val contactsList = listOf(visitor1, visitor2, visitor3)
    val eventList = listOf(eventAudit1, eventAudit2, eventAudit3, eventAudit4, eventAudit5)

    // test-user returns 404 so the userId is returned as name
    val expectedEventActionedByFullNames = listOf("abcd", null, "test-user", "test-user1", "test-user2")
    val notifications = listOf(notification1, notification2)

    visitSchedulerMockServer.stubGetVisit(reference, visit)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisonerDto)
    prisonRegisterMockServer.stubGetPrison(prisonCode, prison)
    // alert 3's alert code is not relevant for visits so should be ignored
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, listOf(alert1, alert2, alert3))
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, offenderRestrictions)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId, withAddress = true, approvedVisitorsOnly = false, null, null, contactsList)
    visitSchedulerMockServer.stubGetVisitHistory(visit.reference, eventList)
    visitSchedulerMockServer.stubGetVisitNotificationEvents(visit.reference, notifications)
    manageUsersApiMockServer.stubGetUserDetailsFailure("test-user", HttpStatus.NOT_FOUND)

    // When
    val responseSpec = callGetVisitFullDetailsByReference(webTestClient, reference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val visitBookingResponse = getResult(responseSpec.expectBody())
    val expectedVisitContact = VisitContactDto(
      contactDto = visit.visitContact!!,
      visitContactId = visitor3.personId,
    )

    assertVisitBookingDetails(visitBookingResponse, visit, prison, prisonerDto, listOf(alert1, alert2), offenderRestrictions, contactsList, expectedVisitContact, eventList, expectedEventActionedByFullNames, notifications)
  }

  @Test
  fun `when visit exists by reference but manage user search returns a 500 the full booking details for that visit is still returned`() {
    // Given
    val reference = "aa-bb-cc-dd"
    val visitors = listOf(createVisitorDto(visitor1, false), createVisitorDto(visitor2, false), createVisitorDto(visitor3, true))
    val visit = createVisitDto(reference = reference, prisonCode = prisonCode, prisonerId = prisonerId, visitors = visitors)
    val contactsList = listOf(visitor1, visitor2, visitor3)
    val eventList = listOf(eventAudit1, eventAudit2, eventAudit3, eventAudit4, eventAudit5)

    // test-user returns 500 so the userId is returned as name
    val expectedEventActionedByFullNames = listOf("abcd", null, "test-user", "test-user1", "test-user2")
    val notifications = listOf(notification1, notification2)

    visitSchedulerMockServer.stubGetVisit(reference, visit)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisonerDto)
    prisonRegisterMockServer.stubGetPrison(prisonCode, prison)
    // alert 3's alert code is not relevant for visits so should be ignored
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, listOf(alert1, alert2, alert3))
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, offenderRestrictions)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId, withAddress = true, approvedVisitorsOnly = false, null, null, contactsList)
    visitSchedulerMockServer.stubGetVisitHistory(visit.reference, eventList)
    visitSchedulerMockServer.stubGetVisitNotificationEvents(visit.reference, notifications)
    manageUsersApiMockServer.stubGetUserDetailsFailure("test-user", HttpStatus.INTERNAL_SERVER_ERROR)

    // When
    val responseSpec = callGetVisitFullDetailsByReference(webTestClient, reference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val visitBookingResponse = getResult(responseSpec.expectBody())
    val expectedVisitContact = VisitContactDto(
      contactDto = visit.visitContact!!,
      visitContactId = visitor3.personId,
    )

    assertVisitBookingDetails(visitBookingResponse, visit, prison, prisonerDto, listOf(alert1, alert2), offenderRestrictions, contactsList, expectedVisitContact, eventList, expectedEventActionedByFullNames, notifications)
  }

  private fun getResult(bodyContentSpec: WebTestClient.BodyContentSpec): VisitBookingDetailsDto = objectMapper.readValue(bodyContentSpec.returnResult().responseBody, VisitBookingDetailsDto::class.java)

  private fun assertVisitBookingDetails(
    visitBookingDetailsDto: VisitBookingDetailsDto,
    visitDto: VisitDto,
    prisonDto: PrisonRegisterPrisonDto,
    prisonerDto: PrisonerDto,
    relevantPrisonerAlerts: List<AlertResponseDto>,
    prisonerRestrictions: OffenderRestrictionsDto,
    visitors: List<PrisonerContactDto>,
    expectedVisitContact: VisitContactDto?,
    events: List<EventAuditDto>,
    expectedEventActionedByFullNames: List<String?>,
    notifications: List<VisitNotificationEventDto>,
  ) {
    assertVisitDetails(visitBookingDetailsDto, visitDto, expectedVisitContact)
    assertPrisonDetails(visitBookingDetailsDto, prisonDto)
    assertPrisonerDetails(visitBookingDetailsDto, visitDto, prisonerDto, relevantPrisonerAlerts, prisonerRestrictions.offenderRestrictions)
    assertVisitors(visitBookingDetailsDto, visitors)
    assertEvents(visitBookingDetailsDto, events, expectedEventActionedByFullNames)
    assertVisitNotifications(visitBookingDetailsDto, notifications)
  }

  private fun assertVisitDetails(
    visitBookingDetailsDto: VisitBookingDetailsDto,
    visitDto: VisitDto,
    visitContact: VisitContactDto?,
  ) {
    assertThat(visitBookingDetailsDto.reference).isEqualTo(visitDto.reference)
    assertThat(visitBookingDetailsDto.visitRoom).isEqualTo(visitDto.visitRoom)
    assertThat(visitBookingDetailsDto.visitStatus).isEqualTo(visitDto.visitStatus)
    assertThat(visitBookingDetailsDto.outcomeStatus).isEqualTo(visitDto.outcomeStatus)
    assertThat(visitBookingDetailsDto.visitRestriction).isEqualTo(visitDto.visitRestriction)
    assertThat(visitBookingDetailsDto.endTimestamp).isEqualTo(visitDto.endTimestamp)
    assertThat(visitBookingDetailsDto.startTimestamp).isEqualTo(visitDto.startTimestamp)
    assertThat(visitBookingDetailsDto.visitorSupport).isEqualTo(visitDto.visitorSupport)
    assertThat(visitBookingDetailsDto.visitNotes).isEqualTo(visitDto.visitNotes)
    assertThat(visitBookingDetailsDto.visitContact).isEqualTo(visitContact)
  }

  private fun assertPrisonDetails(
    visitBookingDetailsDto: VisitBookingDetailsDto,
    prisonDto: PrisonRegisterPrisonDto,
  ) {
    assertThat(visitBookingDetailsDto.prison.prisonId).isEqualTo(prisonDto.prisonId)
    assertThat(visitBookingDetailsDto.prison.prisonName).isEqualTo(prisonDto.prisonName)
  }

  private fun assertPrisonerDetails(
    visitBookingDetailsDto: VisitBookingDetailsDto,
    visitDto: VisitDto,
    prisonerDto: PrisonerDto,
    relevantPrisonerAlerts: List<AlertResponseDto>,
    prisonerRestrictions: List<OffenderRestrictionDto>?,
  ) {
    assertThat(visitBookingDetailsDto.prisoner.prisonId).isEqualTo(visitDto.prisonCode)
    assertThat(visitBookingDetailsDto.prisoner.prisonerNumber).isEqualTo(prisonerDto.prisonerNumber)
    assertThat(visitBookingDetailsDto.prisoner.firstName).isEqualTo(prisonerDto.firstName)
    assertThat(visitBookingDetailsDto.prisoner.lastName).isEqualTo(prisonerDto.lastName)
    assertThat(visitBookingDetailsDto.prisoner.dateOfBirth).isEqualTo(prisonerDto.dateOfBirth)
    assertThat(visitBookingDetailsDto.prisoner.cellLocation).isEqualTo(prisonerDto.cellLocation)
    assertThat(visitBookingDetailsDto.prisoner.locationDescription).isEqualTo(prisonerDto.locationDescription)
    assertPrisonerAlerts(visitBookingDetailsDto.prisoner, relevantPrisonerAlerts)
    assertPrisonerRestrictions(visitBookingDetailsDto.prisoner, prisonerRestrictions)
  }

  private fun assertPrisonerAlerts(
    prisonerDetailsDto: PrisonerDetailsDto,
    relevantPrisonerAlerts: List<AlertResponseDto>,
  ) {
    assertThat(prisonerDetailsDto.prisonerAlerts.size).isEqualTo(relevantPrisonerAlerts.size)
    for (i in prisonerDetailsDto.prisonerAlerts.indices) {
      assertAlerts(prisonerDetailsDto.prisonerAlerts[i], relevantPrisonerAlerts[i])
    }
  }

  private fun assertPrisonerRestrictions(
    prisonerDetailsDto: PrisonerDetailsDto,
    prisonerRestrictions: List<OffenderRestrictionDto>?,
  ) {
    assertThat(prisonerDetailsDto.prisonerRestrictions).isEqualTo(prisonerRestrictions)
  }

  private fun assertAlerts(
    alertDto: AlertDto,
    alertResponseDto: AlertResponseDto,
  ) {
    assertThat(alertDto.alertCode).isEqualTo(alertResponseDto.alertCode.code)
    assertThat(alertDto.alertTypeDescription).isEqualTo(alertResponseDto.alertCode.alertTypeDescription)
    assertThat(alertDto.alertType).isEqualTo(alertResponseDto.alertCode.alertTypeCode)
    assertThat(alertDto.active).isEqualTo(alertResponseDto.active)
    assertThat(alertDto.alertCodeDescription).isEqualTo(alertResponseDto.alertCode.description)
    assertThat(alertDto.comment).isEqualTo(alertResponseDto.description)
    assertThat(alertDto.dateCreated).isEqualTo(alertResponseDto.createdAt)
    assertThat(alertDto.dateExpires).isEqualTo(alertResponseDto.activeTo)
  }

  private fun assertVisitors(
    visitBookingDetailsDto: VisitBookingDetailsDto,
    contacts: List<PrisonerContactDto>,
  ) {
    for (i in visitBookingDetailsDto.visitors.indices) {
      assertVisitor(visitBookingDetailsDto.visitors[i], contacts[i], contacts[i].addresses.firstOrNull { it.primary })
    }
  }

  private fun assertVisitor(
    visitorDetailsDto: VisitorDetailsDto,
    contactDto: PrisonerContactDto,
    primaryAddressDto: AddressDto?,
  ) {
    assertThat(visitorDetailsDto.lastName).isEqualTo(contactDto.lastName)
    assertThat(visitorDetailsDto.dateOfBirth).isEqualTo(contactDto.dateOfBirth)
    assertThat(visitorDetailsDto.firstName).isEqualTo(contactDto.firstName)
    assertThat(visitorDetailsDto.personId).isEqualTo(contactDto.personId)
    assertThat(visitorDetailsDto.primaryAddress).isEqualTo(primaryAddressDto)
    assertThat(visitorDetailsDto.relationshipDescription).isEqualTo(contactDto.relationshipDescription)
    assertThat(visitorDetailsDto.restrictions).isEqualTo(contactDto.restrictions)
  }

  private fun assertEvents(
    visitBookingDetailsDto: VisitBookingDetailsDto,
    events: List<EventAuditDto>,
    fullNames: List<String?>,
  ) {
    for (i in visitBookingDetailsDto.events.indices) {
      assertEvent(visitBookingDetailsDto.events[i], events[i], fullNames[i])
    }
  }

  private fun assertEvent(
    eventAuditOrchestrationDto: EventAuditOrchestrationDto,
    eventAuditDto: EventAuditDto,
    expectedActionedByFullName: String?,
  ) {
    assertThat(eventAuditOrchestrationDto.type).isEqualTo(eventAuditDto.type)
    assertThat(eventAuditOrchestrationDto.userType).isEqualTo(eventAuditDto.actionedBy.userType)
    assertThat(eventAuditOrchestrationDto.actionedByFullName).isEqualTo(expectedActionedByFullName)
    assertThat(eventAuditOrchestrationDto.text).isEqualTo(eventAuditDto.text)
    assertThat(eventAuditOrchestrationDto.applicationMethodType).isEqualTo(eventAuditDto.applicationMethodType)
    assertThat(eventAuditOrchestrationDto.createTimestamp).isEqualTo(eventAuditDto.createTimestamp)
  }

  private fun assertVisitNotifications(
    visitBookingDetailsDto: VisitBookingDetailsDto,
    visitNotificationEvents: List<VisitNotificationEventDto>,
  ) {
    assertThat(visitBookingDetailsDto.notifications.size).isEqualTo(visitNotificationEvents.size)
    for (i in visitBookingDetailsDto.notifications.indices) {
      assertVisitNotification(visitBookingDetailsDto.notifications[i], visitNotificationEvents[i])
    }
  }

  private fun assertVisitNotification(
    notification: VisitNotificationDto,
    visitNotificationEventDto: VisitNotificationEventDto,
  ) {
    assertThat(notification.type).isEqualTo(visitNotificationEventDto.type)
    assertThat(notification.createdDateTime).isEqualTo(visitNotificationEventDto.createdDateTime)
    assertThat(notification.additionalData).isEqualTo(visitNotificationEventDto.additionalData)
  }
}
