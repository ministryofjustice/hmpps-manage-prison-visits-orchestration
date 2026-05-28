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
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller.VISIT_PASSES_CONTROLLER_PATH
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.AddressDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.ContactWithOptionalPrisonerRelationshipDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitorDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitStatus
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.passes.VisitPassDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.passes.VisitPassRequestDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.passes.VisitPassVisitorDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.TestObjectMapper
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.random.Random

@DisplayName("Get Visit passes for a prison and for a given date")
class GetVisitPassesTest : IntegrationTestBase() {
  val prisonCode = "HEI"

  // prisoner1 has 2 visits, rest 1 visit each
  val prisoner1 = createPrisoner("AB1234CD", "Johnny", "English")
  val prisoner2 = createPrisoner("BB1234EF", "John", "Awx")
  val prisoner3 = createPrisoner("CC1234GH", "Abc", "Gle")
  val prisoner4 = createPrisoner("DD1234IJ", "Qwe", "Tsk")
  val prisoner5 = createPrisoner("EE1234KL", "Snn", "Mds")
  val prisoner6 = createPrisoner("FF1234TT", "Der", "Tre")
  val prisoner7 = createPrisoner("GG1234SS", "Avc", "Dre")

  val contact1 = createContact("Jim", "Bim", getRandomDate(), createAddressDto(street = "ABX Lane", primary = true))
  val contact2 = createContact("Abc", "Res", null, null)
  val contact3 = createContact("Def", "Des", null, createAddressDto(street = "West Street", primary = true))
  val contact4 = createContact("Ghi", "Bes", getRandomDate(), createAddressDto(street = "Johnny's Lane", primary = true))
  val contact5 = createContact("Rrt", "Sqw", getRandomDate(), createAddressDto(street = "Johnny Bravo's Lane", primary = true))
  val contact6 = createContact("Jkl", "Kyo", null, createAddressDto(street = "Test Avenue", primary = true))
  val contact7 = createContact("Pqr", "Qua", getRandomDate(), createAddressDto(street = "Coal Lane", primary = true))
  val contact8 = createContact("Stu", "Wer", getRandomDate(), createAddressDto(street = "Swerve Road", primary = true))
  val contact9 = createContact("Vxy", "Tsr", getRandomDate(), null)
  val contact10 = createContact("Zab", "Jge", getRandomDate(), null)
  val contact11 = createContact("Cde", "Kio", getRandomDate(), null)
  val contact12 = createContact("Ggd", "Ssl", getRandomDate(), null)
  val contact13 = createContact("Ste", "Qsa", getRandomDate(), null)
  val contact14 = createContact("Rsa", "Rsx", getRandomDate(), null)
  val contact15 = createContact("Amn", "Fre", getRandomDate(), null)

  lateinit var visit1: VisitDto
  lateinit var visit2: VisitDto
  lateinit var visit3: VisitDto
  lateinit var visit4: VisitDto
  lateinit var visit5: VisitDto
  lateinit var visit6: VisitDto
  lateinit var visit7: VisitDto
  lateinit var visit8: VisitDto

  val visitDate: LocalDate = LocalDate.now()

  @BeforeEach
  fun setupData() {
    // visit1 with 2 contacts (ids 1 and 2)
    val visit1visitors = createVisitors(listOf(contact1.contactId, contact2.contactId))
    visit1 = createVisitDto(reference = "visit-1", prisonerId = prisoner1.prisonerNumber, visitors = visit1visitors, prisonCode = prisonCode, startTimestamp = visitDate.atTime(10, 0), endTimestamp = visitDate.atTime(11, 0))

    // visit2 with 2 contacts (ids 3 and 4)
    val visit2visitors = createVisitors(listOf(contact3.contactId, contact4.contactId))
    visit2 = createVisitDto(reference = "visit-2", prisonerId = prisoner2.prisonerNumber, visitors = visit2visitors, prisonCode = prisonCode, startTimestamp = visitDate.atTime(10, 0), endTimestamp = visitDate.atTime(11, 0))

    // visit3 with 3 contacts (ids 5, 6 and 7)
    val visit3visitors = createVisitors(listOf(contact5.contactId, contact6.contactId, contact7.contactId))
    visit3 = createVisitDto(reference = "visit-3", prisonerId = prisoner3.prisonerNumber, visitors = visit3visitors, prisonCode = prisonCode, startTimestamp = visitDate.atTime(10, 0), endTimestamp = visitDate.atTime(11, 0))

    // visit4 with 2 contacts (ids 8 and 9)
    val visit4visitors = createVisitors(listOf(contact8.contactId, contact9.contactId))
    visit4 = createVisitDto(reference = "visit-4", prisonerId = prisoner4.prisonerNumber, visitors = visit4visitors, prisonCode = prisonCode, startTimestamp = visitDate.atTime(10, 0), endTimestamp = visitDate.atTime(11, 0))

    // visit5 with 2 contacts (ids 10 and 11)
    val visit5visitors = createVisitors(listOf(contact10.contactId, contact11.contactId))
    visit5 = createVisitDto(reference = "visit-5", prisonerId = prisoner5.prisonerNumber, visitors = visit5visitors, prisonCode = prisonCode, startTimestamp = visitDate.atTime(10, 0), endTimestamp = visitDate.atTime(11, 0))

    // visit6 with 2 contacts (ids 12 and 13)
    val visit6visitors = createVisitors(listOf(contact12.contactId, contact13.contactId))
    visit6 = createVisitDto(reference = "visit-6", prisonerId = prisoner6.prisonerNumber, visitors = visit6visitors, prisonCode = prisonCode, startTimestamp = visitDate.atTime(10, 0), endTimestamp = visitDate.atTime(11, 0))

    // visit7 with 2 contacts (ids 1 and 14, both are on 2 separate visits)
    val visit7visitors = createVisitors(listOf(contact1.contactId, contact14.contactId))
    visit7 = createVisitDto(reference = "visit-7", prisonerId = prisoner7.prisonerNumber, visitors = visit7visitors, prisonCode = prisonCode, startTimestamp = visitDate.atTime(11, 0), endTimestamp = visitDate.atTime(12, 0))

    // visit8 with 2 contacts (ids 14 and 15, 14 is on a separate visit)
    val visit8visitors = createVisitors(listOf(contact14.contactId, contact15.contactId))
    visit8 = createVisitDto(reference = "visit-8", prisonerId = prisoner1.prisonerNumber, visitors = visit8visitors, prisonCode = prisonCode, startTimestamp = visitDate.atTime(11, 0), endTimestamp = visitDate.atTime(12, 0))
  }

  @Test
  fun `when a prison has multiple booked visits then get visit passes returns visit passes with details populated`() {
    // Given
    val allVisits = listOf(visit1, visit2, visit3, visit4, visit5, visit6, visit7, visit8)
    val allContacts = listOf(contact1, contact2, contact3, contact4, contact5, contact6, contact7, contact8, contact9, contact10, contact11, contact12, contact13, contact14, contact15)
    val allPrisoners = listOf(prisoner1, prisoner2, prisoner3, prisoner4, prisoner5, prisoner6, prisoner7)

    visitSchedulerMockServer.stubGetVisits(prisonCode = prisonCode, startDate = visitDate, endDate = visitDate, page = 0, size = 250, visitStatus = listOf(VisitStatus.BOOKED.name), visits = allVisits)
    prisonerContactRegistryMockServer.stubSearchContacts(contactIds = allContacts.map { it.contactId }.distinct(), contactsList = allContacts)
    prisonOffenderSearchMockServer.stubGetPrisonersByPrisonerIds(prisonerIds = allPrisoners.map { it.prisonerNumber }.distinct(), prisoners = allPrisoners)

    // When
    val responseSpec = callGetVisitPasses(webTestClient, prisonCode, visitDate, "STAFF_USER", roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val visitPasses = getResults(responseSpec.expectBody())
    assertThat(visitPasses.size).isEqualTo(8)
    assertThat(visitPasses).anySatisfy { assertVisitPassDetails(visit1, prisoner1, listOf(contact1, contact2), it) }
    assertThat(visitPasses).anySatisfy { assertVisitPassDetails(visit2, prisoner2, listOf(contact3, contact4), it) }
    assertThat(visitPasses).anySatisfy { assertVisitPassDetails(visit3, prisoner3, listOf(contact5, contact6, contact7), it) }
    assertThat(visitPasses).anySatisfy { assertVisitPassDetails(visit4, prisoner4, listOf(contact8, contact9), it) }
    assertThat(visitPasses).anySatisfy { assertVisitPassDetails(visit5, prisoner5, listOf(contact10, contact11), it) }
    assertThat(visitPasses).anySatisfy { assertVisitPassDetails(visit6, prisoner6, listOf(contact12, contact13), it) }
    assertThat(visitPasses).anySatisfy { assertVisitPassDetails(visit7, prisoner7, listOf(contact1, contact14), it) }
    assertThat(visitPasses).anySatisfy { assertVisitPassDetails(visit8, prisoner1, listOf(contact14, contact15), it) }
    verify(visitSchedulerClientSpy, times(1)).getVisits(any())
    verify(prisonerSearchClientSpy, times(1)).getPrisonersByPrisonerIdsAttributeSearchAsMono(any())
    verify(prisonerContactRegistryClientSpy, times(1)).searchContactsAsMono(any(), anyOrNull(), any())

    verify(telemetryClientSpy).trackEvent(
      eq("print-visit-passes"),
      check {
        assertThat(it["prisonCode"]).isEqualTo(prisonCode)
        assertThat(it["visitDate"]).isEqualTo(visitDate.format(DateTimeFormatter.ISO_DATE))
        assertThat(it["actionedBy"]).isEqualTo("STAFF_USER")
        assertThat(it["totalVisits"]).isEqualTo("8")
      },
      isNull(),
    )
  }

  @Test
  fun `when a prison has no booked visits then no visit passes are returned`() {
    // Given
    val visits = emptyList<VisitDto>()
    visitSchedulerMockServer.stubGetVisits(prisonCode = prisonCode, startDate = visitDate, endDate = visitDate, page = 0, size = 250, visitStatus = listOf(VisitStatus.BOOKED.name), visits = visits)

    // When
    val responseSpec = callGetVisitPasses(webTestClient, prisonCode, visitDate, "STAFF_USER1", roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val visitPasses = getResults(responseSpec.expectBody())
    assertThat(visitPasses.size).isEqualTo(0)

    verify(visitSchedulerClientSpy, times(1)).getVisits(any())
    verify(prisonerSearchClientSpy, times(0)).getPrisonersByPrisonerIdsAttributeSearchAsMono(any())
    verify(prisonerContactRegistryClientSpy, times(0)).searchContactsAsMono(any(), anyOrNull(), any())
    verify(telemetryClientSpy).trackEvent(
      eq("print-visit-passes"),
      check {
        assertThat(it["prisonCode"]).isEqualTo(prisonCode)
        assertThat(it["visitDate"]).isEqualTo(visitDate.format(DateTimeFormatter.ISO_DATE))
        assertThat(it["actionedBy"]).isEqualTo("STAFF_USER1")
        assertThat(it["totalVisits"]).isEqualTo("0")
      },
      isNull(),
    )
  }

  @Test
  fun `when a prison has multiple booked visits but not all prisoner details are returned then an INTERNAL_SERVER_ERROR is returned`() {
    // Given
    val allVisits = listOf(visit1, visit2)
    val allContacts = listOf(contact1, contact2, contact3, contact4)
    val allPrisoners = listOf(prisoner1, prisoner2)

    visitSchedulerMockServer.stubGetVisits(prisonCode = prisonCode, startDate = visitDate, endDate = visitDate, page = 0, size = 250, visitStatus = listOf(VisitStatus.BOOKED.name), visits = allVisits)
    prisonerContactRegistryMockServer.stubSearchContacts(contactIds = allContacts.map { it.contactId }.distinct(), contactsList = allContacts)

    // only 1 prisoner details returned
    prisonOffenderSearchMockServer.stubGetPrisonersByPrisonerIds(prisonerIds = allPrisoners.map { it.prisonerNumber }.distinct(), prisoners = listOf(prisoner1))

    // When
    val responseSpec = callGetVisitPasses(webTestClient, prisonCode, visitDate, "STAFF_USER2", roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().is5xxServerError
    verify(visitSchedulerClientSpy, times(1)).getVisits(any())
    verify(prisonerSearchClientSpy, times(1)).getPrisonersByPrisonerIdsAttributeSearchAsMono(any())
    verify(prisonerContactRegistryClientSpy, times(1)).searchContactsAsMono(any(), anyOrNull(), any())
    verify(telemetryClientSpy, times(0)).trackEvent(any(), anyOrNull(), anyOrNull())
  }

  @Test
  fun `when a prison has multiple booked visits but not all contact details are returned then an INTERNAL_SERVER_ERROR is returned`() {
    // Given
    val allVisits = listOf(visit1, visit2)
    val allContacts = listOf(contact1, contact2, contact3, contact4)
    val allPrisoners = listOf(prisoner1, prisoner2)

    visitSchedulerMockServer.stubGetVisits(prisonCode = prisonCode, startDate = visitDate, endDate = visitDate, page = 0, size = 250, visitStatus = listOf(VisitStatus.BOOKED.name), visits = allVisits)

    // only 3 contact details returned
    prisonerContactRegistryMockServer.stubSearchContacts(contactIds = allContacts.map { it.contactId }.distinct(), contactsList = listOf(contact1, contact2, contact3))
    prisonOffenderSearchMockServer.stubGetPrisonersByPrisonerIds(prisonerIds = allPrisoners.map { it.prisonerNumber }.distinct(), prisoners = listOf(prisoner1, prisoner2))

    // When
    val responseSpec = callGetVisitPasses(webTestClient, prisonCode, visitDate, "STAFF_USER", roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().is5xxServerError
    verify(visitSchedulerClientSpy, times(1)).getVisits(any())
    verify(prisonerSearchClientSpy, times(1)).getPrisonersByPrisonerIdsAttributeSearchAsMono(any())
    verify(prisonerContactRegistryClientSpy, times(1)).searchContactsAsMono(any(), anyOrNull(), any())
    verify(telemetryClientSpy, times(0)).trackEvent(any(), anyOrNull(), anyOrNull())
  }

  @Test
  fun `when a prison has multiple booked visits but prisoner contact registry returns a NOT_FOUND then a NOT_FOUND error is returned`() {
    // Given
    val allVisits = listOf(visit1, visit2)
    val allContacts = listOf(contact1, contact2, contact3, contact4)
    val allPrisoners = listOf(prisoner1, prisoner2)

    visitSchedulerMockServer.stubGetVisits(prisonCode = prisonCode, startDate = visitDate, endDate = visitDate, page = 0, size = 250, visitStatus = listOf(VisitStatus.BOOKED.name), visits = allVisits)
    // contact registry returns a 404
    prisonerContactRegistryMockServer.stubSearchContacts(contactIds = allContacts.map { it.contactId }.distinct(), contactsList = null, httpStatus = HttpStatus.NOT_FOUND)
    prisonOffenderSearchMockServer.stubGetPrisonersByPrisonerIds(prisonerIds = allPrisoners.map { it.prisonerNumber }.distinct(), prisoners = listOf(prisoner1, prisoner2))

    // When
    val responseSpec = callGetVisitPasses(webTestClient, prisonCode, visitDate, "STAFF_USER", roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
    verify(visitSchedulerClientSpy, times(1)).getVisits(any())
    verify(prisonerSearchClientSpy, times(1)).getPrisonersByPrisonerIdsAttributeSearchAsMono(any())
    verify(prisonerContactRegistryClientSpy, times(1)).searchContactsAsMono(any(), anyOrNull(), any())
    verify(telemetryClientSpy, times(0)).trackEvent(any(), anyOrNull(), anyOrNull())
  }

  @Test
  fun `when a prison has multiple booked visits but prisoner search returns a NOT_FOUND then a NOT_FOUND error is returned`() {
    // Given
    val allVisits = listOf(visit1, visit2)
    val allContacts = listOf(contact1, contact2, contact3, contact4)
    val allPrisoners = listOf(prisoner1, prisoner2)

    visitSchedulerMockServer.stubGetVisits(prisonCode = prisonCode, startDate = visitDate, endDate = visitDate, page = 0, size = 250, visitStatus = listOf(VisitStatus.BOOKED.name), visits = allVisits)
    prisonerContactRegistryMockServer.stubSearchContacts(contactIds = allContacts.map { it.contactId }.distinct(), contactsList = allContacts)
    // prisoner search returns a 404
    prisonOffenderSearchMockServer.stubGetPrisonersByPrisonerIds(prisonerIds = allPrisoners.map { it.prisonerNumber }.distinct(), prisoners = null, HttpStatus.NOT_FOUND)

    // When
    val responseSpec = callGetVisitPasses(webTestClient, prisonCode, visitDate, "STAFF_USER", roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
    verify(visitSchedulerClientSpy, times(1)).getVisits(any())
    verify(prisonerSearchClientSpy, times(1)).getPrisonersByPrisonerIdsAttributeSearchAsMono(any())
    verify(prisonerContactRegistryClientSpy, times(1)).searchContactsAsMono(any(), anyOrNull(), any())
    verify(telemetryClientSpy, times(0)).trackEvent(any(), anyOrNull(), anyOrNull())
  }

  @Test
  fun `when a prison has multiple booked visits but prisoner contact registry returns an INTERNAL_SERVER_ERROR then a INTERNAL_SERVER_ERROR error is returned`() {
    // Given
    val allVisits = listOf(visit1, visit2)
    val allContacts = listOf(contact1, contact2, contact3, contact4)
    val allPrisoners = listOf(prisoner1, prisoner2)

    visitSchedulerMockServer.stubGetVisits(prisonCode = prisonCode, startDate = visitDate, endDate = visitDate, page = 0, size = 250, visitStatus = listOf(VisitStatus.BOOKED.name), visits = allVisits)
    // contact registry returns a INTERNAL_SERVER_ERROR
    prisonerContactRegistryMockServer.stubSearchContacts(contactIds = allContacts.map { it.contactId }.distinct(), contactsList = null, httpStatus = HttpStatus.INTERNAL_SERVER_ERROR)
    prisonOffenderSearchMockServer.stubGetPrisonersByPrisonerIds(prisonerIds = allPrisoners.map { it.prisonerNumber }.distinct(), prisoners = listOf(prisoner1, prisoner2))

    // When
    val responseSpec = callGetVisitPasses(webTestClient, prisonCode, visitDate, "STAFF_USER", roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().is5xxServerError
    verify(visitSchedulerClientSpy, times(1)).getVisits(any())
    verify(prisonerSearchClientSpy, times(1)).getPrisonersByPrisonerIdsAttributeSearchAsMono(any())
    verify(prisonerContactRegistryClientSpy, times(1)).searchContactsAsMono(any(), anyOrNull(), any())
    verify(telemetryClientSpy, times(0)).trackEvent(any(), anyOrNull(), anyOrNull())
  }

  @Test
  fun `when a prison has multiple booked visits but prisoner search returns an INTERNAL_SERVER_ERROR then a INTERNAL_SERVER_ERROR error is returned`() {
    // Given
    val allVisits = listOf(visit1, visit2)
    val allContacts = listOf(contact1, contact2, contact3, contact4)
    val allPrisoners = listOf(prisoner1, prisoner2)

    visitSchedulerMockServer.stubGetVisits(prisonCode = prisonCode, startDate = visitDate, endDate = visitDate, page = 0, size = 250, visitStatus = listOf(VisitStatus.BOOKED.name), visits = allVisits)
    prisonerContactRegistryMockServer.stubSearchContacts(contactIds = allContacts.map { it.contactId }.distinct(), contactsList = allContacts)
    // prisoner search returns an INTERNAL_SERVER_ERROR
    prisonOffenderSearchMockServer.stubGetPrisonersByPrisonerIds(prisonerIds = allPrisoners.map { it.prisonerNumber }.distinct(), prisoners = null, HttpStatus.INTERNAL_SERVER_ERROR)

    // When
    val responseSpec = callGetVisitPasses(webTestClient, prisonCode, visitDate, "STAFF_USER", roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().is5xxServerError
    verify(visitSchedulerClientSpy, times(1)).getVisits(any())
    verify(prisonerSearchClientSpy, times(1)).getPrisonersByPrisonerIdsAttributeSearchAsMono(any())
    verify(prisonerContactRegistryClientSpy, times(1)).searchContactsAsMono(any(), anyOrNull(), any())
    verify(telemetryClientSpy, times(0)).trackEvent(any(), anyOrNull(), anyOrNull())
  }

  @Test
  fun `when a prison has multiple booked visits but visit scheduler returns a NOT_FOUND then a NOT_FOUND error is returned`() {
    // Given
    val allContacts = listOf(contact1, contact2, contact3, contact4)
    val allPrisoners = listOf(prisoner1, prisoner2)

    // visit scheduler returns a 404
    visitSchedulerMockServer.stubGetVisits(prisonCode = prisonCode, startDate = visitDate, endDate = visitDate, page = 0, size = 250, visitStatus = listOf(VisitStatus.BOOKED.name), visits = null, httpStatus = HttpStatus.NOT_FOUND)
    prisonerContactRegistryMockServer.stubSearchContacts(contactIds = allContacts.map { it.contactId }.distinct(), contactsList = allContacts)
    prisonOffenderSearchMockServer.stubGetPrisonersByPrisonerIds(prisonerIds = allPrisoners.map { it.prisonerNumber }.distinct(), prisoners = allPrisoners)

    // When
    val responseSpec = callGetVisitPasses(webTestClient, prisonCode, visitDate, "STAFF_USER", roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
    verify(visitSchedulerClientSpy, times(1)).getVisits(any())
    verify(prisonerSearchClientSpy, times(0)).getPrisonersByPrisonerIdsAttributeSearchAsMono(any())
    verify(prisonerContactRegistryClientSpy, times(0)).searchContactsAsMono(any(), anyOrNull(), any())
    verify(telemetryClientSpy, times(0)).trackEvent(any(), anyOrNull(), anyOrNull())
  }

  @Test
  fun `when a prison has multiple booked visits but visit scheduler returns an INTERNAL_SERVER_ERROR then a INTERNAL_SERVER_ERROR error is returned`() {
    // Given
    val allContacts = listOf(contact1, contact2, contact3, contact4)
    val allPrisoners = listOf(prisoner1, prisoner2)

    // visit scheduler returns an INTERNAL_SERVER_ERROR
    visitSchedulerMockServer.stubGetVisits(prisonCode = prisonCode, startDate = visitDate, endDate = visitDate, page = 0, size = 250, visitStatus = listOf(VisitStatus.BOOKED.name), visits = null, httpStatus = HttpStatus.INTERNAL_SERVER_ERROR)
    prisonerContactRegistryMockServer.stubSearchContacts(contactIds = allContacts.map { it.contactId }.distinct(), contactsList = allContacts)
    prisonOffenderSearchMockServer.stubGetPrisonersByPrisonerIds(prisonerIds = allPrisoners.map { it.prisonerNumber }.distinct(), prisoners = allPrisoners)

    // When
    val responseSpec = callGetVisitPasses(webTestClient, prisonCode, visitDate, "STAFF_USER", roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().is5xxServerError
    verify(visitSchedulerClientSpy, times(1)).getVisits(any())
    verify(prisonerSearchClientSpy, times(0)).getPrisonersByPrisonerIdsAttributeSearchAsMono(any())
    verify(prisonerContactRegistryClientSpy, times(0)).searchContactsAsMono(any(), anyOrNull(), any())
    verify(telemetryClientSpy, times(0)).trackEvent(any(), anyOrNull(), anyOrNull())
  }

  private fun callGetVisitPasses(
    webTestClient: WebTestClient,
    prisonCode: String,
    visitDate: LocalDate,
    actionedBy: String,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    val visitPassRequest = VisitPassRequestDto(date = visitDate, actionedBy = actionedBy)
    return webTestClient.post()
      .uri(VISIT_PASSES_CONTROLLER_PATH.replace("{prisonId}", prisonCode))
      .body(BodyInserters.fromValue(visitPassRequest))
      .headers(authHttpHeaders)
      .exchange()
  }

  private fun createPrisoner(
    prisonerId: String,
    firstName: String,
    lastName: String,
    dob: LocalDate? = null,
    prisonCode: String = "HEI",
    convictedStatus: String = "CONVICTED",
  ): PrisonerDto = createPrisoner(
    prisonerId = prisonerId,
    firstName = firstName,
    lastName = lastName,
    dateOfBirth = dob ?: getRandomDate(),
    prisonId = prisonCode,
    convictedStatus = convictedStatus,
  )

  private fun createContact(
    firstName: String,
    lastName: String,
    dob: LocalDate? = null,
    addressDto: AddressDto?,
  ): ContactWithOptionalPrisonerRelationshipDto = createContactWithOptionalPrisonerRelationshipDto(
    firstName = firstName,
    lastName = lastName,
    dateOfBirth = dob,
    address = addressDto,
  )

  private fun createVisitors(visitorId: List<Long>): List<VisitorDto> = visitorId.map { createVisitor(it) }

  private fun createVisitor(
    visitorId: Long,
  ): VisitorDto = VisitorDto(visitorId, Random.nextBoolean())

  private fun getRandomDate(): LocalDate {
    val date = (1..28).random()
    val month = (1..12).random()
    val year = (1950..2005).random()
    return LocalDate.of(year, month, date)
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): List<VisitPassDto> = TestObjectMapper.mapper.readValue(returnResult.returnResult().responseBody, Array<VisitPassDto>::class.java).toList()

  private fun assertVisitPassDetails(
    visitDto: VisitDto,
    prisonerDto: PrisonerDto,
    contactDtoList: List<ContactWithOptionalPrisonerRelationshipDto>,
    visitPassDto: VisitPassDto,
  ) {
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
