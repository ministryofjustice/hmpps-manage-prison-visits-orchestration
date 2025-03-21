package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.prisoner.profile

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.AlertsApiClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonRegisterClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonerContactRegistryClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitSchedulerClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.PrisonerProfileDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.alerts.api.AlertDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.InmateDetailDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.VisitBalancesDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.CurrentIncentive
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.IncentiveLevel
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSummaryDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitorDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitorSummaryDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.temporal.TemporalAdjusters

@DisplayName("Get Prisoner Profile")
class GetPrisonerProfileTest(
  @Value("\${prisoner.profile.past-visits.duration-in-months: -P3M}") private val pastVisitsPeriod: Period,
  @Value("\${prisoner.profile.future-visits.duration-in-months: P2M}") private val futureVisitsPeriod: Period,
) : IntegrationTestBase() {
  companion object {
    private const val PRISONER_ID = "AA112233B"
    private const val FIRST_NAME = "FirstName"
    private const val LAST_NAME = "LastName"
    private val DATE_OF_BIRTH = LocalDate.of(2000, 1, 31)
    private const val PRISONER_CATEGORY = "Category - C"
    private const val PRISON_CODE = "MDI"
    private val alert = AlertDto(comment = "Alert code comment", alertCode = "C1", alertCodeDescription = "Alert Code Desc", alertType = "T", alertTypeDescription = "Type Description", dateCreated = LocalDate.now(), active = true)
  }

  private final val currentIncentive = createCurrentIncentive()

  private final val prisonerDto = createPrisoner(
    prisonerId = PRISONER_ID,
    firstName = FIRST_NAME,
    lastName = LAST_NAME,
    dateOfBirth = DATE_OF_BIRTH,
    currentIncentive = currentIncentive,
    convictedStatus = "Convicted",
  )

  private val visitor1 = VisitorDetails(1, "First", "VisitorA")
  private val visitor2 = VisitorDetails(2, "Second", "VisitorB")
  private val visitor3 = VisitorDetails(3, "Third", "VisitorC")

  private val prison1 = createPrisonNameDto("ABC", "ABC Prison")
  private val prison2 = createPrisonNameDto("DEF", "DEF Prison")
  private val prison3 = createPrisonNameDto("MDI", "MDI Prison")
  private val prisons = listOf(prison1, prison2, prison3)

  private final val inmateDetailDto = createInmateDetails(PRISONER_ID, PRISONER_CATEGORY)
  private final val alertResponseDto = createAlertResponseDto()
  private final val visitBalancesDto = createVisitBalancesDto()
  private val contactsDto = createContactsList(listOf(visitor1, visitor2, visitor3))
  private val visit1Visitors = listOf(
    VisitorDto(nomisPersonId = visitor1.personId, visitContact = true),
    VisitorDto(nomisPersonId = visitor2.personId, visitContact = false),
    VisitorDto(nomisPersonId = visitor3.personId, visitContact = false),
  )

  private val visit2Visitors = listOf(
    VisitorDto(nomisPersonId = visitor3.personId, visitContact = true),
  )

  // visit1 has 3 visitors in visitors list
  private final val visit1 = createVisitDto(reference = "visit-1", prisonerId = PRISONER_ID, visitors = visit1Visitors)

  // visit2 has 1 visitor in visitors list
  private final val visit2 = createVisitDto(reference = "visit-2", prisonerId = PRISONER_ID, visitors = visit2Visitors, prisonCode = "ABC")

  @MockitoSpyBean
  lateinit var visitSchedulerClientSpy: VisitSchedulerClient

  @MockitoSpyBean
  lateinit var prisonAPiClientSpy: PrisonApiClient

  @MockitoSpyBean
  lateinit var alertsApiClient: AlertsApiClient

  @MockitoSpyBean
  lateinit var prisonerSearchClientSpy: PrisonerSearchClient

  @MockitoSpyBean
  lateinit var prisonerContactRegistryClientSpy: PrisonerContactRegistryClient

  @MockitoSpyBean
  lateinit var prisonRegisterClientSpy: PrisonRegisterClient

  fun callGetPrisonerProfile(
    webTestClient: WebTestClient,
    authHttpHeaders: (HttpHeaders) -> Unit,
    prisonId: String,
    prisonerId: String,
  ): WebTestClient.ResponseSpec = webTestClient.get().uri("/prisoner/$prisonId/$prisonerId/profile")
    .headers(authHttpHeaders)
    .exchange()

  @Test
  fun `when valid prisoner prisoner profile is collated and returned`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(PRISONER_ID, inmateDetailDto)
    prisonApiMockServer.stubGetVisitBalances(PRISONER_ID, visitBalancesDto)
    alertApiMockServer.stubGetPrisonerAlertsMono(PRISONER_ID, listOf(alertResponseDto))
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = false, null, null, contactsDto)
    prisonRegisterMockServer.stubGetPrisons(prisons)
    stubGetVisits(listOf(visit1, visit2))

    // When
    val responseSpec = callGetPrisonerProfile(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, PRISON_CODE, PRISONER_ID)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerProfile = getResults(returnResult)

    assertPrisonerDtoDetails(prisonerProfile, prisonerDto)
    Assertions.assertThat(prisonerProfile.incentiveLevel).isEqualTo(prisonerDto.currentIncentive!!.level.description)
    assertInmateDetails(prisonerProfile, inmateDetailDto)
    Assertions.assertThat(prisonerProfile.visitBalances).isEqualTo(visitBalancesDto)
    Assertions.assertThat(prisonerProfile.visits.size).isEqualTo(2)
    Assertions.assertThat(prisonerProfile.alerts).isEqualTo(listOf(alert))
    assertVisits(prisonerProfile, listOf(visit1, visit2))

    verifyExternalAPIClientCalls()
  }

  @Test
  fun `when prison API get prisoner details returns NOT_FOUND prisoner profile call returns NOT_FOUND status`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, null)
    prisonApiMockServer.stubGetInmateDetails(PRISONER_ID, inmateDetailDto)
    prisonApiMockServer.stubGetVisitBalances(PRISONER_ID, visitBalancesDto)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = false, null, null, contactsDto)
    alertApiMockServer.stubGetPrisonerAlertsMono(PRISONER_ID, listOf(alertResponseDto))

    stubGetVisits(listOf(visit1, visit2))

    // When
    val responseSpec = callGetPrisonerProfile(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, PRISON_CODE, PRISONER_ID)

    // Then
    responseSpec.expectStatus().isNotFound

    verifyExternalAPIClientCalls()
  }

  @Test
  fun `when prison API get inmate details returns NOT_FOUND prisoner profile call returns NOT_FOUND status`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(PRISONER_ID, null)
    prisonApiMockServer.stubGetVisitBalances(PRISONER_ID, visitBalancesDto)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = false, null, null, contactsDto, HttpStatus.NOT_FOUND)
    alertApiMockServer.stubGetPrisonerAlertsMono(PRISONER_ID, listOf(alertResponseDto))
    prisonRegisterMockServer.stubGetPrisons(prisons)
    stubGetVisits(listOf(visit1, visit2))

    // When
    val responseSpec = callGetPrisonerProfile(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, PRISON_CODE, PRISONER_ID)

    // Then
    responseSpec.expectStatus().isNotFound

    verifyExternalAPIClientCalls()
  }

  @Test
  fun `when prison returned by prison API does not match prison id passed to endpoint prisoner profile call returns internal server error`() {
    // Given prisoner's prison ID is different to the prison ID passed in endpoint
    val prisonerDto = createPrisoner(
      prisonerId = PRISONER_ID,
      prisonId = "XYZ",
      firstName = FIRST_NAME,
      lastName = LAST_NAME,
      dateOfBirth = DATE_OF_BIRTH,
      currentIncentive = currentIncentive,
      convictedStatus = null,
    )

    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(PRISONER_ID, inmateDetailDto)
    prisonApiMockServer.stubGetVisitBalances(PRISONER_ID, visitBalancesDto)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = false, null, null, contactsDto, HttpStatus.NOT_FOUND)
    alertApiMockServer.stubGetPrisonerAlertsMono(PRISONER_ID, listOf(alertResponseDto))
    prisonRegisterMockServer.stubGetPrisons(prisons)
    stubGetVisits(listOf(visit1, visit2))

    // When
    val responseSpec = callGetPrisonerProfile(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, PRISON_CODE, PRISONER_ID)

    // Then
    responseSpec.expectStatus().is4xxClientError

    verifyExternalAPIClientCalls()
  }

  @Test
  fun `when prison API get visit balances returns NOT_FOUND prisoner profile call returns a profile profile with visitBalances as null`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(PRISONER_ID, inmateDetailDto)
    prisonApiMockServer.stubGetVisitBalances(PRISONER_ID, null)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = false, null, null, contactsDto, HttpStatus.NOT_FOUND)
    alertApiMockServer.stubGetPrisonerAlertsMono(PRISONER_ID, listOf(alertResponseDto))
    prisonRegisterMockServer.stubGetPrisons(prisons)
    stubGetVisits(listOf(visit1, visit2))

    // When
    val responseSpec = callGetPrisonerProfile(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, PRISON_CODE, PRISONER_ID)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerProfile = getResults(returnResult)

    assertPrisonerDtoDetails(prisonerProfile, prisonerDto)
    Assertions.assertThat(prisonerProfile.incentiveLevel).isEqualTo(prisonerDto.currentIncentive!!.level.description)
    assertInmateDetails(prisonerProfile, inmateDetailDto)
    Assertions.assertThat(prisonerProfile.visitBalances).isNull()
    Assertions.assertThat(prisonerProfile.visits.size).isEqualTo(2)
    Assertions.assertThat(prisonerProfile.alerts).isEqualTo(listOf(alert))
    assertVisits(prisonerProfile, listOf(visit1, visit2))

    verifyExternalAPIClientCalls()
  }

  @Test
  fun `when prison API get prisoner current incentive is null prisoner profile current incentive is null`() {
    // Given
    val prisonerDto = createPrisoner(
      prisonerId = PRISONER_ID,
      firstName = FIRST_NAME,
      lastName = LAST_NAME,
      dateOfBirth = DATE_OF_BIRTH,
      currentIncentive = null,
      convictedStatus = "Remand",
    )
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(PRISONER_ID, inmateDetailDto)
    prisonApiMockServer.stubGetVisitBalances(PRISONER_ID, visitBalancesDto)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = false, null, null, contactsDto, HttpStatus.NOT_FOUND)
    alertApiMockServer.stubGetPrisonerAlertsMono(PRISONER_ID, listOf(alertResponseDto))
    prisonRegisterMockServer.stubGetPrisons(prisons)
    stubGetVisits(listOf(visit1, visit2))

    // When
    val responseSpec = callGetPrisonerProfile(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, PRISON_CODE, PRISONER_ID)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerProfile = getResults(returnResult)

    assertPrisonerDtoDetails(prisonerProfile, prisonerDto)
    Assertions.assertThat(prisonerProfile.incentiveLevel).isNull()
    assertInmateDetails(prisonerProfile, inmateDetailDto)
    Assertions.assertThat(prisonerProfile.visitBalances).isEqualTo(visitBalancesDto)
    Assertions.assertThat(prisonerProfile.alerts).isEqualTo(listOf(alert))
    assertVisits(prisonerProfile, listOf(visit1, visit2))

    verifyExternalAPIClientCalls()
  }

  @Test
  fun `when visits do not exist for prisoner visits list is empty`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(PRISONER_ID, inmateDetailDto)
    prisonApiMockServer.stubGetVisitBalances(PRISONER_ID, visitBalancesDto)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = false, null, null, contactsDto, HttpStatus.NOT_FOUND)
    alertApiMockServer.stubGetPrisonerAlertsMono(PRISONER_ID, listOf(alertResponseDto))
    prisonRegisterMockServer.stubGetPrisons(prisons)
    stubGetVisits(mutableListOf())

    // When
    val responseSpec = callGetPrisonerProfile(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, PRISON_CODE, PRISONER_ID)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerProfile = getResults(returnResult)

    assertPrisonerDtoDetails(prisonerProfile, prisonerDto)
    Assertions.assertThat(prisonerProfile.incentiveLevel).isEqualTo(prisonerDto.currentIncentive!!.level.description)
    assertInmateDetails(prisonerProfile, inmateDetailDto)
    Assertions.assertThat(prisonerProfile.visitBalances).isEqualTo(visitBalancesDto)
    Assertions.assertThat(prisonerProfile.alerts).isEqualTo(listOf(alert))
    Assertions.assertThat(prisonerProfile.visits).isEmpty()

    verifyExternalAPIClientCalls()
  }

  @Test
  fun `when visit has valid visitors first and last name are correctly populated from prisoner contact registry`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(PRISONER_ID, inmateDetailDto)
    prisonApiMockServer.stubGetVisitBalances(PRISONER_ID, visitBalancesDto)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = false, null, null, contactsDto)
    alertApiMockServer.stubGetPrisonerAlertsMono(PRISONER_ID, listOf(alertResponseDto))
    prisonRegisterMockServer.stubGetPrisons(prisons)
    stubGetVisits(listOf(visit1, visit2))

    // When
    val responseSpec = callGetPrisonerProfile(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, PRISON_CODE, PRISONER_ID)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerProfile = getResults(returnResult)

    assertPrisonerDtoDetails(prisonerProfile, prisonerDto)
    Assertions.assertThat(prisonerProfile.visits).isNotEmpty

    val visit1Visitors = prisonerProfile.visits[0].visitors
    Assertions.assertThat(visit1Visitors?.size).isEqualTo(3)
    assertVisitorDetails(visit1Visitors?.get(0)!!, visitor1.personId, visitor1.firstName, visitor1.lastName)
    assertVisitorDetails(visit1Visitors[1], visitor2.personId, visitor2.firstName, visitor2.lastName)
    assertVisitorDetails(visit1Visitors[2], visitor3.personId, visitor3.firstName, visitor3.lastName)

    val visit2Visitors = prisonerProfile.visits[1].visitors
    Assertions.assertThat(visit2Visitors).isNotNull
    assertVisitorDetails(visit2Visitors?.get(0)!!, visitor3.personId, visitor3.firstName, visitor3.lastName)

    verifyExternalAPIClientCalls()
    // verify the call to prisoner contact registry is only done once
    verify(prisonerContactRegistryClientSpy, times(1)).getPrisonersSocialContacts(any(), eq(false), eq(false), isNull(), isNull())
  }

  @Test
  fun `when visits visitors are not found on prisoner contact registry visitors first and last name are returned as null`() {
    // Given
    val visitorNotInContactRegistry = VisitorDto(nomisPersonId = 300, visitContact = true)

    // visit does not have a visitors list
    val visit3 = createVisitDto(
      reference = "visit-3",
      prisonerId = PRISONER_ID,
      // this visitor is not on prisoner contact registry
      visitors = listOf(visitorNotInContactRegistry),
    )
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(PRISONER_ID, inmateDetailDto)
    prisonApiMockServer.stubGetVisitBalances(PRISONER_ID, visitBalancesDto)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = false, null, null, contactsDto)
    alertApiMockServer.stubGetPrisonerAlertsMono(PRISONER_ID, listOf(alertResponseDto))
    prisonRegisterMockServer.stubGetPrisons(prisons)

    stubGetVisits(listOf(visit3))

    // When
    val responseSpec = callGetPrisonerProfile(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, PRISON_CODE, PRISONER_ID)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerProfile = getResults(returnResult)

    assertPrisonerDtoDetails(prisonerProfile, prisonerDto)
    Assertions.assertThat(prisonerProfile.visits).isNotEmpty

    val visitors = prisonerProfile.visits[0].visitors
    Assertions.assertThat(prisonerProfile.visits[0].visitors).isNotNull
    Assertions.assertThat(visitors).isNotNull
    Assertions.assertThat(visitors?.size).isEqualTo(1)

    // the nomisPersonId should be set to what's on the visit's visitor list
    assertVisitorDetails(visitors?.get(0)!!, visitorNotInContactRegistry.nomisPersonId, null, null)

    verifyExternalAPIClientCalls()
    // verify the call to prisoner contact registry is made once
    verify(prisonerContactRegistryClientSpy, times(1)).getPrisonersSocialContacts(any(), eq(false), eq(false), isNull(), isNull())
  }

  @Test
  fun `when visit has visitors but call to prisoner contact registry returns NOT_FOUND visitors first and last name are not populated`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(PRISONER_ID, inmateDetailDto)
    prisonApiMockServer.stubGetVisitBalances(PRISONER_ID, visitBalancesDto)
    alertApiMockServer.stubGetPrisonerAlertsMono(PRISONER_ID, listOf(alertResponseDto))
    prisonRegisterMockServer.stubGetPrisons(prisons)

    // as we are passing null as contacts parameter a 404 will be returned
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = false, null, null, null)
    stubGetVisits(listOf(visit1, visit2))

    // When
    val responseSpec = callGetPrisonerProfile(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, PRISON_CODE, PRISONER_ID)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerProfile = getResults(returnResult)

    assertPrisonerDtoDetails(prisonerProfile, prisonerDto)
    Assertions.assertThat(prisonerProfile.visits).isNotEmpty

    val visit1Visitors = prisonerProfile.visits[0].visitors
    assertVisitorDetails(visit1Visitors?.get(0)!!, visitor1.personId, null, null)
    assertVisitorDetails(visit1Visitors[1], visitor2.personId, null, null)
    assertVisitorDetails(visit1Visitors[2], visitor3.personId, null, null)

    val visit2Visitors = prisonerProfile.visits[1].visitors
    Assertions.assertThat(visit2Visitors?.size).isEqualTo(1)
    assertVisitorDetails(visit2Visitors?.get(0)!!, visitor3.personId, null, null)

    verifyExternalAPIClientCalls()
    // verify the call to prisoner contact registry is made once
    verify(prisonerContactRegistryClientSpy, times(1)).getPrisonersSocialContacts(any(), eq(false), eq(false), isNull(), isNull())
  }

  @Test
  fun `when visit has visitors but call to prisoner contact registry returns BAD_REQUEST visitors first and last name are not populated`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(PRISONER_ID, inmateDetailDto)
    prisonApiMockServer.stubGetVisitBalances(PRISONER_ID, visitBalancesDto)
    alertApiMockServer.stubGetPrisonerAlertsMono(PRISONER_ID, listOf(alertResponseDto))
    prisonRegisterMockServer.stubGetPrisons(prisons)

    // as we are passing null as contacts parameter a 404 will be returned
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = false, null, null, null, HttpStatus.BAD_REQUEST)
    stubGetVisits(listOf(visit1, visit2))

    // When
    val responseSpec = callGetPrisonerProfile(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, PRISON_CODE, PRISONER_ID)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerProfile = getResults(returnResult)

    assertPrisonerDtoDetails(prisonerProfile, prisonerDto)
    Assertions.assertThat(prisonerProfile.visits).isNotEmpty

    val visit1Visitors = prisonerProfile.visits[0].visitors
    assertVisitorDetails(visit1Visitors?.get(0)!!, visitor1.personId, null, null)
    assertVisitorDetails(visit1Visitors[1], visitor2.personId, null, null)
    assertVisitorDetails(visit1Visitors[2], visitor3.personId, null, null)

    val visit2Visitors = prisonerProfile.visits[1].visitors
    Assertions.assertThat(visit2Visitors?.size).isEqualTo(1)
    assertVisitorDetails(visit2Visitors?.get(0)!!, visitor3.personId, null, null)

    verifyExternalAPIClientCalls()
    // verify the call to prisoner contact registry is made once
    verify(prisonerContactRegistryClientSpy, times(1)).getPrisonersSocialContacts(any(), eq(false), eq(false), isNull(), isNull())
  }

  @Test
  fun `when visits have valid prisons then prison names are correctly populated from prison register`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(PRISONER_ID, inmateDetailDto)
    prisonApiMockServer.stubGetVisitBalances(PRISONER_ID, visitBalancesDto)
    alertApiMockServer.stubGetPrisonerAlertsMono(PRISONER_ID, listOf(alertResponseDto))
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = false, null, null, contactsDto)
    prisonRegisterMockServer.stubGetPrisons(prisons)
    stubGetVisits(listOf(visit1, visit2))

    // When
    val responseSpec = callGetPrisonerProfile(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, PRISON_CODE, PRISONER_ID)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerProfile = getResults(returnResult)

    assertPrisonerDtoDetails(prisonerProfile, prisonerDto)
    Assertions.assertThat(prisonerProfile.visits).isNotEmpty

    val visits = prisonerProfile.visits
    assertPrisonDetails(visits[0], visit1.prisonCode, "MDI Prison")
    assertPrisonDetails(visits[1], visit2.prisonCode, "ABC Prison")
    verifyExternalAPIClientCalls()

    // verify the call to prison register is only done once
    verify(prisonRegisterClientSpy, times(1)).getPrisonNames()
  }

  @Test
  fun `when visits have invalid prisons then prison names are not populated from prison register`() {
    // Given
    val incorrectPrisonVisit = createVisitDto(reference = "invalid-prison", prisonerId = PRISONER_ID, visitors = visit1Visitors, prisonCode = "NONE")

    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(PRISONER_ID, inmateDetailDto)
    prisonApiMockServer.stubGetVisitBalances(PRISONER_ID, visitBalancesDto)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = false, null, null, contactsDto)
    prisonRegisterMockServer.stubGetPrisons(prisons)
    alertApiMockServer.stubGetPrisonerAlertsMono(PRISONER_ID, listOf(alertResponseDto))
    stubGetVisits(listOf(incorrectPrisonVisit))

    // When
    val responseSpec = callGetPrisonerProfile(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, PRISON_CODE, PRISONER_ID)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerProfile = getResults(returnResult)

    assertPrisonerDtoDetails(prisonerProfile, prisonerDto)
    Assertions.assertThat(prisonerProfile.visits).isNotEmpty

    val visits = prisonerProfile.visits
    assertPrisonDetails(visits[0], incorrectPrisonVisit.prisonCode, null)

    verifyExternalAPIClientCalls()
    // verify the call to prison register is only done once
    verify(prisonRegisterClientSpy, times(1)).getPrisonNames()
  }

  @Test
  fun `when call to prisoner register returns NOT_FOUND prison names are not populated`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(PRISONER_ID, inmateDetailDto)
    prisonApiMockServer.stubGetVisitBalances(PRISONER_ID, visitBalancesDto)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = false, null, null, contactsDto)
    alertApiMockServer.stubGetPrisonerAlertsMono(PRISONER_ID, listOf(alertResponseDto))

    // as we are passing null as prisons parameter a 404 will be returned
    prisonRegisterMockServer.stubGetPrisons(null)
    stubGetVisits(listOf(visit1, visit2))

    // When
    val responseSpec = callGetPrisonerProfile(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, PRISON_CODE, PRISONER_ID)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerProfile = getResults(returnResult)

    assertPrisonerDtoDetails(prisonerProfile, prisonerDto)

    val visits = prisonerProfile.visits
    assertPrisonDetails(visits[0], visit1.prisonCode, null)
    assertPrisonDetails(visits[1], visit2.prisonCode, null)

    verifyExternalAPIClientCalls()
    // verify the call to prison register is only done once
    verify(prisonRegisterClientSpy, times(1)).getPrisonNames()
  }

  @Test
  fun `when call to prisoner register returns BAD_REQUEST prison names are not populated`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(PRISONER_ID, inmateDetailDto)
    prisonApiMockServer.stubGetVisitBalances(PRISONER_ID, visitBalancesDto)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = false, null, null, contactsDto)
    alertApiMockServer.stubGetPrisonerAlertsMono(PRISONER_ID, listOf(alertResponseDto))

    // as we are passing null as prisons parameter a BAD_REQUEST will be returned
    prisonRegisterMockServer.stubGetPrisons(null)
    stubGetVisits(listOf(visit1, visit2))

    // When
    val responseSpec = callGetPrisonerProfile(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, PRISON_CODE, PRISONER_ID)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerProfile = getResults(returnResult)

    assertPrisonerDtoDetails(prisonerProfile, prisonerDto)

    val visits = prisonerProfile.visits
    Assertions.assertThat(visits[0].prisonCode).isEqualTo(visit1.prisonCode)
    Assertions.assertThat(visits[0].prisonName).isNull()
    Assertions.assertThat(visits[1].prisonCode).isEqualTo(visit2.prisonCode)
    Assertions.assertThat(visits[1].prisonName).isNull()

    verifyExternalAPIClientCalls()
    // verify the call to prison register is only done once
    verify(prisonRegisterClientSpy, times(1)).getPrisonNames()
  }

  private fun assertPrisonerDtoDetails(prisonerProfile: PrisonerProfileDto, prisonerDto: PrisonerDto) {
    Assertions.assertThat(prisonerProfile.prisonerId).isEqualTo(prisonerDto.prisonerNumber)
    Assertions.assertThat(prisonerProfile.firstName).isEqualTo(prisonerDto.firstName)
    Assertions.assertThat(prisonerProfile.lastName).isEqualTo(prisonerDto.lastName)
    Assertions.assertThat(prisonerProfile.dateOfBirth).isEqualTo(prisonerDto.dateOfBirth)
    Assertions.assertThat(prisonerProfile.prisonId).isEqualTo(prisonerDto.prisonId)
    Assertions.assertThat(prisonerProfile.prisonName).isEqualTo(prisonerDto.prisonName)
    Assertions.assertThat(prisonerProfile.cellLocation).isEqualTo(prisonerDto.cellLocation)
    Assertions.assertThat(prisonerProfile.convictedStatus).isEqualTo(prisonerDto.convictedStatus)
  }

  private fun assertInmateDetails(prisonerProfile: PrisonerProfileDto, inmateDetails: InmateDetailDto) {
    Assertions.assertThat(prisonerProfile.category).isEqualTo(inmateDetails.category)
  }

  private fun assertVisitorDetails(visitorDto: VisitorSummaryDto, personId: Long, firstName: String?, lastName: String?) {
    Assertions.assertThat(visitorDto.nomisPersonId).isEqualTo(personId)
    Assertions.assertThat(visitorDto.firstName).isEqualTo(firstName)
    Assertions.assertThat(visitorDto.lastName).isEqualTo(lastName)
  }

  private fun assertVisits(prisonerProfile: PrisonerProfileDto, visits: List<VisitDto>) {
    val visitReferences = prisonerProfile.visits.stream().map { it.reference }.toList()
    Assertions.assertThat(prisonerProfile.visits.size).isEqualTo(visits.size)
    visits.forEach {
      Assertions.assertThat(visitReferences).contains(it.reference)
    }
  }

  private fun assertPrisonDetails(visitSummary: VisitSummaryDto, prisonCode: String, prisonName: String?) {
    Assertions.assertThat(visitSummary.prisonCode).isEqualTo(prisonCode)
    Assertions.assertThat(visitSummary.prisonName).isEqualTo(prisonName)
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): PrisonerProfileDto = objectMapper.readValue(returnResult.returnResult().responseBody, PrisonerProfileDto::class.java)

  private fun createInmateDetails(
    prisonerId: String,
    category: String? = null,
  ): InmateDetailDto = InmateDetailDto(offenderNo = prisonerId, category = category)

  private fun createVisitBalancesDto(): VisitBalancesDto = VisitBalancesDto(remainingVo = 10, remainingPvo = 10)

  private fun createCurrentIncentive(): CurrentIncentive {
    val incentiveLevel = IncentiveLevel("S", "Standard")
    return CurrentIncentive(incentiveLevel, LocalDateTime.now())
  }

  private fun stubGetVisits(visits: List<VisitDto>) {
    visitSchedulerMockServer.stubGetVisits(
      prisonerId = PRISONER_ID,
      visitStatus = listOf("BOOKED", "CANCELLED"),
      startDate = LocalDate.now().minus(pastVisitsPeriod).with(TemporalAdjusters.firstDayOfMonth()),
      endDate = LocalDate.now().plus(futureVisitsPeriod).with(TemporalAdjusters.lastDayOfMonth()),
      page = 0,
      size = 1000,
      visits = visits,
    )
  }

  private fun verifyExternalAPIClientCalls() {
    verify(visitSchedulerClientSpy, times(1)).getVisitsAsMono(any())
    verify(prisonerSearchClientSpy, times(1)).getPrisonerByIdAsMono(any())
    verify(prisonAPiClientSpy, times(1)).getInmateDetailsAsMono(any())
    verify(prisonAPiClientSpy, times(1)).getVisitBalancesAsMono(any())
    verify(alertsApiClient, times(1)).getPrisonerAlertsAsMono(any())
  }
}
