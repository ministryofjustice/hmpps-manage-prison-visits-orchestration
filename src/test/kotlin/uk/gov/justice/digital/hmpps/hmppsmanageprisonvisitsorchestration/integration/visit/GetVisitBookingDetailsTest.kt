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
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.PrisonerDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.VisitBookingDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.VisitorDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.OffenderRestrictionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.OffenderRestrictionsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.register.PrisonRegisterPrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.ActionedByDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.EventAuditDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.ApplicationMethodType.EMAIL
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.EventAuditType.BOOKED_VISIT
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType.STAFF
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

  private lateinit var actionedBy: ActionedByDto
  private lateinit var eventAudit: EventAuditDto

  @BeforeEach
  internal fun setup() {
    prisonerDto = createPrisoner(
      prisonerId = prisonerId,
      firstName = "FirstName",
      lastName = "LastName",
      dateOfBirth = LocalDate.of(2000, 1, 31),
      prisonId = prisonCode,
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

    actionedBy = ActionedByDto(bookerReference = null, userName = "test-user", userType = STAFF)
    eventAudit = EventAuditDto(type = BOOKED_VISIT, actionedBy = actionedBy, applicationMethodType = EMAIL)
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
    val visitors = listOf(createVisitorDto(visitor1, true), createVisitorDto(visitor2, false), createVisitorDto(visitor3, true))
    val visit = createVisitDto(reference = reference, prisonCode = prisonCode, prisonerId = prisonerId, visitors = visitors)
    val contactsList = listOf(visitor1, visitor2, visitor3)
    val eventList = mutableListOf(eventAudit)

    visitSchedulerMockServer.stubGetVisit(reference, visit)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisonerDto)
    prisonRegisterMockServer.stubGetPrison(prisonCode, prison)
    // alert 3's alert code is not relevant for visits so should be ignored
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, listOf(alert1, alert2, alert3))
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, offenderRestrictions)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId, withAddress = true, approvedVisitorsOnly = false, null, null, contactsList)

    visitSchedulerMockServer.stubGetVisitHistory(visit.reference, eventList)

    // When
    val responseSpec = callGetVisitFullDetailsByReference(webTestClient, reference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val visitBookingResponse = getResult(responseSpec.expectBody())
    assertVisitBookingDetails(visitBookingResponse, visit, prison, prisonerDto, listOf(alert1, alert2), offenderRestrictions, contactsList, eventList)
  }

  @Test
  fun `when a visit's visitor is no longer in contact list that visitor is not returned for that visit`() {
    // Given
    val reference = "aa-bb-cc-dd"
    val prisonerId = "prisoner-id"
    val visitors = listOf(createVisitorDto(visitor1, true), createVisitorDto(visitor2, false), createVisitorDto(visitor3, true))
    val visit = createVisitDto(reference = reference, prisonCode = prisonCode, prisonerId = prisonerId, visitors = visitors)
    // contacts returned does not have visitor 3
    val contactsList = listOf(visitor1, visitor2)
    val eventList = mutableListOf(eventAudit)

    visitSchedulerMockServer.stubGetVisit(reference, visit)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisonerDto)
    prisonRegisterMockServer.stubGetPrison(prisonCode, prison)
    // alert 3's alert code is not relevant for visits so should be ignored
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, listOf(alert1, alert2, alert3))
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, offenderRestrictions)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId = prisonerId, withAddress = true, approvedVisitorsOnly = false, personId = null, hasDateOfBirth = null, contactsList = contactsList)
    visitSchedulerMockServer.stubGetVisitHistory(visit.reference, eventList)

    // When
    val responseSpec = callGetVisitFullDetailsByReference(webTestClient, reference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val visitBookingResponse = getResult(responseSpec.expectBody())
    assertVisitBookingDetails(visitBookingResponse, visit, prison, prisonerDto, listOf(alert1, alert2), offenderRestrictions, contactsList, eventList)
  }

  @Test
  fun `when prisoner search returns a 404 then an exception is thrown and a 404 is returned as response `() {
    // Given
    val reference = "aa-bb-cc-dd"
    val prisonerId = "prisoner-id"
    val visitors = listOf(createVisitorDto(visitor1, true), createVisitorDto(visitor2, false), createVisitorDto(visitor3, true))
    val visit = createVisitDto(reference = reference, prisonCode = prisonCode, prisonerId = prisonerId, visitors = visitors)
    val contactsList = listOf(visitor1, visitor2, visitor3)
    val eventList = mutableListOf(eventAudit)

    visitSchedulerMockServer.stubGetVisit(reference, visit)
    // prisoner search returns a 404
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, null, HttpStatus.NOT_FOUND)
    prisonRegisterMockServer.stubGetPrison(prisonCode, prison)
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, listOf(alert1, alert2, alert3))
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, offenderRestrictions)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId, withAddress = true, approvedVisitorsOnly = false, null, null, contactsList)
    visitSchedulerMockServer.stubGetVisitHistory(visit.reference, eventList)

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
    val visitors = listOf(createVisitorDto(visitor1, true), createVisitorDto(visitor2, false), createVisitorDto(visitor3, true))
    val visit = createVisitDto(reference = reference, prisonCode = prisonCode, prisonerId = prisonerId, visitors = visitors)
    val contactsList = listOf(visitor1, visitor2, visitor3)
    val eventList = mutableListOf(eventAudit)

    visitSchedulerMockServer.stubGetVisit(reference, visit)
    // prisoner search returns a 500
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, null, HttpStatus.INTERNAL_SERVER_ERROR)
    prisonRegisterMockServer.stubGetPrison(prisonCode, prison)
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
  fun `when prison register search returns a 404 then no exception is thrown and prison code and name both come back with prison code`() {
    // Given
    val reference = "aa-bb-cc-dd"
    val prisonerId = "prisoner-id"
    val visitors = listOf(createVisitorDto(visitor1, true), createVisitorDto(visitor2, false), createVisitorDto(visitor3, true))
    val visit = createVisitDto(reference = reference, prisonCode = prisonCode, prisonerId = prisonerId, visitors = visitors)
    val contactsList = listOf(visitor1, visitor2, visitor3)
    val eventList = mutableListOf(eventAudit)

    visitSchedulerMockServer.stubGetVisit(reference, visit)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisonerDto)
    prisonRegisterMockServer.stubGetPrison(prisonCode, null, HttpStatus.NOT_FOUND)
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, listOf(alert1, alert2, alert3))
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, offenderRestrictions)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId, withAddress = true, approvedVisitorsOnly = false, null, null, contactsList)
    visitSchedulerMockServer.stubGetVisitHistory(visit.reference, eventList)

    // When
    val responseSpec = callGetVisitFullDetailsByReference(webTestClient, reference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val visitBookingResponse = getResult(responseSpec.expectBody())

    // as prison register search returned a 404 we expect both prison code and name to have the same value of prison code
    val expectedPrison = PrisonRegisterPrisonDto(prisonCode, prisonCode, true)
    assertVisitBookingDetails(visitBookingResponse, visit, expectedPrison, prisonerDto, listOf(alert1, alert2), offenderRestrictions, contactsList, eventList)
  }

  @Test
  fun `when prison register search returns a 500 an exception is thrown and a 500 is returned as response`() {
    // Given
    val reference = "aa-bb-cc-dd"
    val visitors = listOf(createVisitorDto(visitor1, true), createVisitorDto(visitor2, false), createVisitorDto(visitor3, true))
    val visit = createVisitDto(reference = reference, prisonCode = prisonCode, prisonerId = prisonerId, visitors = visitors)
    val contactsList = listOf(visitor1, visitor2, visitor3)
    val eventList = mutableListOf(eventAudit)

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
    val visitors = listOf(createVisitorDto(visitor1, true), createVisitorDto(visitor2, false), createVisitorDto(visitor3, true))
    val visit = createVisitDto(reference = reference, prisonCode = prisonCode, prisonerId = prisonerId, visitors = visitors)
    val contactsList = listOf(visitor1, visitor2, visitor3)
    val eventList = mutableListOf(eventAudit)

    visitSchedulerMockServer.stubGetVisit(reference, visit)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisonerDto)
    prisonRegisterMockServer.stubGetPrison(prisonCode, prison)
    // alert API returns a 500
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, null, HttpStatus.INTERNAL_SERVER_ERROR)
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, offenderRestrictions)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId, withAddress = true, approvedVisitorsOnly = false, null, null, contactsList)
    visitSchedulerMockServer.stubGetVisitHistory(visit.reference, eventList)

    // When
    val responseSpec = callGetVisitFullDetailsByReference(webTestClient, reference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().is5xxServerError
  }

  @Test
  fun `when prison API get restrictions call returns returns a 404 an exception is thrown and a 404 is returned as response`() {
    // Given
    val reference = "aa-bb-cc-dd"
    val visitors = listOf(createVisitorDto(visitor1, true), createVisitorDto(visitor2, false), createVisitorDto(visitor3, true))
    val visit = createVisitDto(reference = reference, prisonCode = prisonCode, prisonerId = prisonerId, visitors = visitors)
    val contactsList = listOf(visitor1, visitor2, visitor3)
    val eventList = mutableListOf(eventAudit)

    visitSchedulerMockServer.stubGetVisit(reference, visit)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisonerDto)
    prisonRegisterMockServer.stubGetPrison(prisonCode, prison)
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, listOf(alert1, alert2, alert3))
    // prison API returns a 404
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, null, HttpStatus.NOT_FOUND)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId, withAddress = true, approvedVisitorsOnly = false, null, null, contactsList)
    visitSchedulerMockServer.stubGetVisitHistory(visit.reference, eventList)

    // When
    val responseSpec = callGetVisitFullDetailsByReference(webTestClient, reference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
  }

  @Test
  fun `when prison API get restrictions call returns returns a 500 an exception is thrown and a 500 is returned as response`() {
    // Given
    val reference = "aa-bb-cc-dd"
    val visitors = listOf(createVisitorDto(visitor1, true), createVisitorDto(visitor2, false), createVisitorDto(visitor3, true))
    val visit = createVisitDto(reference = reference, prisonCode = prisonCode, prisonerId = prisonerId, visitors = visitors)
    val contactsList = listOf(visitor1, visitor2, visitor3)
    val eventList = mutableListOf(eventAudit)

    visitSchedulerMockServer.stubGetVisit(reference, visit)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisonerDto)
    prisonRegisterMockServer.stubGetPrison(prisonCode, prison)
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, listOf(alert1, alert2, alert3))
    // prison API returns a 404
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, null, HttpStatus.INTERNAL_SERVER_ERROR)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId, withAddress = true, approvedVisitorsOnly = false, null, null, contactsList)
    visitSchedulerMockServer.stubGetVisitHistory(visit.reference, eventList)

    // When
    val responseSpec = callGetVisitFullDetailsByReference(webTestClient, reference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().is5xxServerError
  }

  @Test
  fun `when prisoner contact registry API call returns returns a 404 an exception is thrown and a 404 is returned as response`() {
    // Given
    val reference = "aa-bb-cc-dd"
    val visitors = listOf(createVisitorDto(visitor1, true), createVisitorDto(visitor2, false), createVisitorDto(visitor3, true))
    val visit = createVisitDto(reference = reference, prisonCode = prisonCode, prisonerId = prisonerId, visitors = visitors)
    val eventList = mutableListOf(eventAudit)

    visitSchedulerMockServer.stubGetVisit(reference, visit)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisonerDto)
    prisonRegisterMockServer.stubGetPrison(prisonCode, prison)
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, listOf(alert1, alert2, alert3))
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, offenderRestrictions)
    // prisoner contact registry API returns a 404
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId, withAddress = true, approvedVisitorsOnly = false, null, null, null, HttpStatus.NOT_FOUND)
    visitSchedulerMockServer.stubGetVisitHistory(visit.reference, eventList)

    // When
    val responseSpec = callGetVisitFullDetailsByReference(webTestClient, reference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
  }

  @Test
  fun `when prisoner contact registry API call returns returns a 500 an exception is thrown and a 500 is returned as response`() {
    // Given
    val reference = "aa-bb-cc-dd"
    val visitors = listOf(createVisitorDto(visitor1, true), createVisitorDto(visitor2, false), createVisitorDto(visitor3, true))
    val visit = createVisitDto(reference = reference, prisonCode = prisonCode, prisonerId = prisonerId, visitors = visitors)
    val eventList = mutableListOf(eventAudit)

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
    val visitors = listOf(createVisitorDto(visitor1, true), createVisitorDto(visitor2, false), createVisitorDto(visitor3, true))
    val visit = createVisitDto(reference = reference, prisonCode = prisonCode, prisonerId = prisonerId, visitors = visitors)
    val contactsList = listOf(visitor1, visitor2, visitor3)
    visitSchedulerMockServer.stubGetVisit(reference, visit)

    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisonerDto)
    prisonRegisterMockServer.stubGetPrison(prisonCode, prison)
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, listOf(alert1, alert2, alert3))
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, offenderRestrictions)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId, withAddress = true, approvedVisitorsOnly = false, null, null, contactsList)

    // visits get history - returns a 404
    visitSchedulerMockServer.stubGetVisitHistory(visit.reference, emptyList(), HttpStatus.NOT_FOUND)

    // When
    val responseSpec = callGetVisitFullDetailsByReference(webTestClient, reference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
  }

  @Test
  fun `when visit history API call returns returns a 500 an exception is thrown and a 500 is returned as response`() {
    // Given
    val reference = "aa-bb-cc-dd"
    val visitors = listOf(createVisitorDto(visitor1, true), createVisitorDto(visitor2, false), createVisitorDto(visitor3, true))
    val visit = createVisitDto(reference = reference, prisonCode = prisonCode, prisonerId = prisonerId, visitors = visitors)
    val contactsList = listOf(visitor1, visitor2, visitor3)

    visitSchedulerMockServer.stubGetVisit(reference, visit)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisonerDto)
    prisonRegisterMockServer.stubGetPrison(prisonCode, prison)
    alertApiMockServer.stubGetPrisonerAlertsMono(prisonerId, listOf(alert1, alert2, alert3))
    prisonApiMockServer.stubGetPrisonerRestrictions(prisonerId, offenderRestrictions)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId, withAddress = true, approvedVisitorsOnly = false, null, null, contactsList)

    // visits get history - returns a 500
    visitSchedulerMockServer.stubGetVisitHistory(visit.reference, emptyList(), HttpStatus.INTERNAL_SERVER_ERROR)

    // When
    val responseSpec = callGetVisitFullDetailsByReference(webTestClient, reference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().is5xxServerError
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
    events: List<EventAuditDto>,
  ) {
    assertVisitDetails(visitBookingDetailsDto, visitDto)
    assertPrisonDetails(visitBookingDetailsDto, prisonDto)
    assertPrisonerDetails(visitBookingDetailsDto, visitDto, prisonerDto, relevantPrisonerAlerts, prisonerRestrictions.offenderRestrictions)
    assertVisitors(visitBookingDetailsDto, visitors)
    assertEvents(visitBookingDetailsDto, events)
  }

  private fun assertVisitDetails(
    visitBookingDetailsDto: VisitBookingDetailsDto,
    visitDto: VisitDto,
  ) {
    assertThat(visitBookingDetailsDto.reference).isEqualTo(visitDto.reference)
    assertThat(visitBookingDetailsDto.visitRoom).isEqualTo(visitDto.visitRoom)
    assertThat(visitBookingDetailsDto.visitType).isEqualTo(visitDto.visitType)
    assertThat(visitBookingDetailsDto.visitRestriction).isEqualTo(visitDto.visitRestriction)
    assertThat(visitBookingDetailsDto.visitContact).isEqualTo(visitDto.visitContact)
    assertThat(visitBookingDetailsDto.endTimestamp).isEqualTo(visitDto.endTimestamp)
    assertThat(visitBookingDetailsDto.startTimestamp).isEqualTo(visitDto.startTimestamp)
    assertThat(visitBookingDetailsDto.visitorSupport).isEqualTo(visitDto.visitorSupport)
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
  ) {
    assertThat(visitBookingDetailsDto.events).isEqualTo(events)
  }
}
