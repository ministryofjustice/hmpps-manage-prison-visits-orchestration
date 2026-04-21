package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.prisoner.profile

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.PrisonerProfileDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.alerts.api.AlertDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.VisitBalancesDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.InmateDetailDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.OffenderRestrictionsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation.PrisonerVOBalanceDetailedDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSummaryDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitorDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitorSummaryDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.TestObjectMapper
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.temporal.TemporalAdjusters

@DisplayName("Get Prisoner Profile")
class GetPrisonerProfileTest(
  @param:Value("\${prisoner.profile.past-visits.duration-in-months: -P3M}") private val pastVisitsPeriod: Period,
  @param:Value("\${prisoner.profile.future-visits.duration-in-months: P2M}") private val futureVisitsPeriod: Period,
) : IntegrationTestBase() {
  companion object {
    private const val PRISONER_ID = "AA112233B"
    private const val FIRST_NAME = "FirstName"
    private const val LAST_NAME = "LastName"
    private val DATE_OF_BIRTH = LocalDate.of(2000, 1, 31)
    private const val PRISONER_CATEGORY = "Category - C"
    private const val PRISON_CODE = "MDI"
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
  private val alert = AlertDto(alertResponseDto)
  private final val prisonerRestrictionDto = createOffenderRestrictionDto()
  private final val visitBalancesDto = createPrisonerVoBalanceDto()
  private final val expectedVisitBalances = VisitBalancesDto(remainingVo = 10, remainingPvo = 10, lastVoAllocationDate = LocalDate.now(), nextVoAllocationDate = LocalDate.now().plusDays(14), lastPvoAllocationDate = LocalDate.now(), nextPvoAllocationDate = LocalDate.now().plusDays(28))

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
    visitAllocationApiMockServer.stubGetPrisonerVOBalanceDetailed(PRISONER_ID, visitBalancesDto)
    alertApiMockServer.stubGetPrisonerAlertsMono(PRISONER_ID, listOf(alertResponseDto))
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, contactsList = contactsDto)
    prisonRegisterMockServer.stubGetPrisonNames(prisons)
    prisonApiMockServer.stubGetPrisonerRestrictions(PRISONER_ID, OffenderRestrictionsDto(bookingId = 1, listOf(prisonerRestrictionDto)))
    stubGetVisits(listOf(visit1, visit2))

    // When
    val responseSpec = callGetPrisonerProfile(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, PRISON_CODE, PRISONER_ID)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerProfile = getResults(returnResult)

    assertPrisonerDtoDetails(prisonerProfile, prisonerDto)
    Assertions.assertThat(prisonerProfile.incentiveLevel).isEqualTo(prisonerDto.currentIncentive!!.level.description)
    assertInmateDetails(prisonerProfile, inmateDetailDto)
    Assertions.assertThat(prisonerProfile.visitBalances).isEqualTo(expectedVisitBalances)
    Assertions.assertThat(prisonerProfile.visits.size).isEqualTo(2)
    Assertions.assertThat(prisonerProfile.alerts).isEqualTo(listOf(alert))
    Assertions.assertThat(prisonerProfile.prisonerRestrictions).isEqualTo(listOf(prisonerRestrictionDto))
    assertVisits(prisonerProfile, listOf(visit1, visit2))

    verifyExternalAPIClientCalls()
  }

  @Test
  fun `when prison API get prisoner details returns NOT_FOUND prisoner profile call returns NOT_FOUND status`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, null)
    prisonApiMockServer.stubGetInmateDetails(PRISONER_ID, inmateDetailDto)
    visitAllocationApiMockServer.stubGetPrisonerVOBalanceDetailed(PRISONER_ID, visitBalancesDto)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, contactsList = contactsDto)
    prisonApiMockServer.stubGetPrisonerRestrictions(PRISONER_ID, OffenderRestrictionsDto(bookingId = 1, listOf(prisonerRestrictionDto)))
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
    visitAllocationApiMockServer.stubGetPrisonerVOBalanceDetailed(PRISONER_ID, visitBalancesDto)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, contactsList = contactsDto, httpStatus = HttpStatus.NOT_FOUND)
    alertApiMockServer.stubGetPrisonerAlertsMono(PRISONER_ID, listOf(alertResponseDto))
    prisonApiMockServer.stubGetPrisonerRestrictions(PRISONER_ID, OffenderRestrictionsDto(bookingId = 1, listOf(prisonerRestrictionDto)))
    prisonRegisterMockServer.stubGetPrisonNames(prisons)
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
    visitAllocationApiMockServer.stubGetPrisonerVOBalanceDetailed(PRISONER_ID, visitBalancesDto)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, contactsList = contactsDto, httpStatus = HttpStatus.NOT_FOUND)
    alertApiMockServer.stubGetPrisonerAlertsMono(PRISONER_ID, listOf(alertResponseDto))
    prisonApiMockServer.stubGetPrisonerRestrictions(PRISONER_ID, OffenderRestrictionsDto(bookingId = 1, listOf(prisonerRestrictionDto)))
    prisonRegisterMockServer.stubGetPrisonNames(prisons)
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
    visitAllocationApiMockServer.stubGetPrisonerVOBalanceDetailed(PRISONER_ID, null)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, contactsList = contactsDto, httpStatus = HttpStatus.NOT_FOUND)
    alertApiMockServer.stubGetPrisonerAlertsMono(PRISONER_ID, listOf(alertResponseDto))
    prisonApiMockServer.stubGetPrisonerRestrictions(PRISONER_ID, OffenderRestrictionsDto(bookingId = 1, listOf(prisonerRestrictionDto)))
    prisonRegisterMockServer.stubGetPrisonNames(prisons)
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
    visitAllocationApiMockServer.stubGetPrisonerVOBalanceDetailed(PRISONER_ID, visitBalancesDto)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, contactsList = contactsDto, httpStatus = HttpStatus.NOT_FOUND)
    alertApiMockServer.stubGetPrisonerAlertsMono(PRISONER_ID, listOf(alertResponseDto))
    prisonApiMockServer.stubGetPrisonerRestrictions(PRISONER_ID, OffenderRestrictionsDto(bookingId = 1, listOf(prisonerRestrictionDto)))
    prisonRegisterMockServer.stubGetPrisonNames(prisons)
    stubGetVisits(listOf(visit1, visit2))

    // When
    val responseSpec = callGetPrisonerProfile(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, PRISON_CODE, PRISONER_ID)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerProfile = getResults(returnResult)

    assertPrisonerDtoDetails(prisonerProfile, prisonerDto)
    Assertions.assertThat(prisonerProfile.incentiveLevel).isNull()
    assertInmateDetails(prisonerProfile, inmateDetailDto)
    Assertions.assertThat(prisonerProfile.visitBalances).isEqualTo(expectedVisitBalances)
    Assertions.assertThat(prisonerProfile.alerts).isEqualTo(listOf(alert))
    assertVisits(prisonerProfile, listOf(visit1, visit2))

    verifyExternalAPIClientCalls()
  }

  @Test
  fun `when visits do not exist for prisoner visits list is empty`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(PRISONER_ID, inmateDetailDto)
    visitAllocationApiMockServer.stubGetPrisonerVOBalanceDetailed(PRISONER_ID, visitBalancesDto)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, contactsList = contactsDto, httpStatus = HttpStatus.NOT_FOUND)
    alertApiMockServer.stubGetPrisonerAlertsMono(PRISONER_ID, listOf(alertResponseDto))
    prisonApiMockServer.stubGetPrisonerRestrictions(PRISONER_ID, OffenderRestrictionsDto(bookingId = 1, listOf(prisonerRestrictionDto)))
    prisonRegisterMockServer.stubGetPrisonNames(prisons)
    stubGetVisits(mutableListOf())

    // When
    val responseSpec = callGetPrisonerProfile(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, PRISON_CODE, PRISONER_ID)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerProfile = getResults(returnResult)

    assertPrisonerDtoDetails(prisonerProfile, prisonerDto)
    Assertions.assertThat(prisonerProfile.incentiveLevel).isEqualTo(prisonerDto.currentIncentive!!.level.description)
    assertInmateDetails(prisonerProfile, inmateDetailDto)
    Assertions.assertThat(prisonerProfile.visitBalances).isEqualTo(expectedVisitBalances)
    Assertions.assertThat(prisonerProfile.alerts).isEqualTo(listOf(alert))
    Assertions.assertThat(prisonerProfile.visits).isEmpty()

    verifyExternalAPIClientCalls()
  }

  @Test
  fun `when visit has valid visitors first and last name are correctly populated from prisoner contact registry`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(PRISONER_ID, inmateDetailDto)
    visitAllocationApiMockServer.stubGetPrisonerVOBalanceDetailed(PRISONER_ID, visitBalancesDto)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, contactsList = contactsDto)
    alertApiMockServer.stubGetPrisonerAlertsMono(PRISONER_ID, listOf(alertResponseDto))
    prisonApiMockServer.stubGetPrisonerRestrictions(PRISONER_ID, OffenderRestrictionsDto(bookingId = 1, listOf(prisonerRestrictionDto)))
    prisonRegisterMockServer.stubGetPrisonNames(prisons)
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
    verify(prisonerContactRegistryClientSpy, times(1)).getPrisonersSocialContacts(any(), eq(null))
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
    visitAllocationApiMockServer.stubGetPrisonerVOBalanceDetailed(PRISONER_ID, visitBalancesDto)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, contactsList = contactsDto)
    alertApiMockServer.stubGetPrisonerAlertsMono(PRISONER_ID, listOf(alertResponseDto))
    prisonApiMockServer.stubGetPrisonerRestrictions(PRISONER_ID, OffenderRestrictionsDto(bookingId = 1, listOf(prisonerRestrictionDto)))
    prisonRegisterMockServer.stubGetPrisonNames(prisons)

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
    verify(prisonerContactRegistryClientSpy, times(1)).getPrisonersSocialContacts(any(), eq(null))
  }

  @Test
  fun `when visit has visitors but call to prisoner contact registry returns NOT_FOUND visitors first and last name are not populated`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(PRISONER_ID, inmateDetailDto)
    visitAllocationApiMockServer.stubGetPrisonerVOBalanceDetailed(PRISONER_ID, visitBalancesDto)
    alertApiMockServer.stubGetPrisonerAlertsMono(PRISONER_ID, listOf(alertResponseDto))
    prisonApiMockServer.stubGetPrisonerRestrictions(PRISONER_ID, OffenderRestrictionsDto(bookingId = 1, listOf(prisonerRestrictionDto)))
    prisonRegisterMockServer.stubGetPrisonNames(prisons)

    // as we are passing null as contacts parameter a 404 will be returned
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, contactsList = null)
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
    verify(prisonerContactRegistryClientSpy, times(1)).getPrisonersSocialContacts(any(), eq(null))
  }

  @Test
  fun `when visit has visitors but call to prisoner contact registry returns BAD_REQUEST visitors first and last name are not populated`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(PRISONER_ID, inmateDetailDto)
    visitAllocationApiMockServer.stubGetPrisonerVOBalanceDetailed(PRISONER_ID, visitBalancesDto)
    alertApiMockServer.stubGetPrisonerAlertsMono(PRISONER_ID, listOf(alertResponseDto))
    prisonApiMockServer.stubGetPrisonerRestrictions(PRISONER_ID, OffenderRestrictionsDto(bookingId = 1, listOf(prisonerRestrictionDto)))
    prisonRegisterMockServer.stubGetPrisonNames(prisons)

    // as we are passing null as contacts parameter a 404 will be returned
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, contactsList = null, httpStatus = HttpStatus.BAD_REQUEST)
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
    verify(prisonerContactRegistryClientSpy, times(1)).getPrisonersSocialContacts(any(), eq(null))
  }

  @Test
  fun `when visits have valid prisons then prison names are correctly populated from prison register`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(PRISONER_ID, inmateDetailDto)
    visitAllocationApiMockServer.stubGetPrisonerVOBalanceDetailed(PRISONER_ID, visitBalancesDto)
    alertApiMockServer.stubGetPrisonerAlertsMono(PRISONER_ID, listOf(alertResponseDto))
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, contactsList = contactsDto)
    prisonApiMockServer.stubGetPrisonerRestrictions(PRISONER_ID, OffenderRestrictionsDto(bookingId = 1, listOf(prisonerRestrictionDto)))
    prisonRegisterMockServer.stubGetPrisonNames(prisons)
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
    visitAllocationApiMockServer.stubGetPrisonerVOBalanceDetailed(PRISONER_ID, visitBalancesDto)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, contactsList = contactsDto)
    prisonRegisterMockServer.stubGetPrisonNames(prisons)
    alertApiMockServer.stubGetPrisonerAlertsMono(PRISONER_ID, listOf(alertResponseDto))
    prisonApiMockServer.stubGetPrisonerRestrictions(PRISONER_ID, OffenderRestrictionsDto(bookingId = 1, listOf(prisonerRestrictionDto)))
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
    visitAllocationApiMockServer.stubGetPrisonerVOBalanceDetailed(PRISONER_ID, visitBalancesDto)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, contactsList = contactsDto)
    alertApiMockServer.stubGetPrisonerAlertsMono(PRISONER_ID, listOf(alertResponseDto))
    prisonApiMockServer.stubGetPrisonerRestrictions(PRISONER_ID, OffenderRestrictionsDto(bookingId = 1, listOf(prisonerRestrictionDto)))

    // as we are passing null as prisons parameter a 404 will be returned
    prisonRegisterMockServer.stubGetPrisonNames(null)
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
    visitAllocationApiMockServer.stubGetPrisonerVOBalanceDetailed(PRISONER_ID, visitBalancesDto)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, contactsList = contactsDto)
    alertApiMockServer.stubGetPrisonerAlertsMono(PRISONER_ID, listOf(alertResponseDto))
    prisonApiMockServer.stubGetPrisonerRestrictions(PRISONER_ID, OffenderRestrictionsDto(bookingId = 1, listOf(prisonerRestrictionDto)))

    // as we are passing null as prisons parameter a BAD_REQUEST will be returned
    prisonRegisterMockServer.stubGetPrisonNames(null)
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

  @Test
  fun `when alerts API get alerts call returns returns a NOT_FOUND an exception is thrown and a NOT_FOUND is returned as response`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(PRISONER_ID, inmateDetailDto)
    visitAllocationApiMockServer.stubGetPrisonerVOBalanceDetailed(PRISONER_ID, visitBalancesDto)
    alertApiMockServer.stubGetPrisonerAlertsMono(PRISONER_ID, null, HttpStatus.NOT_FOUND)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, contactsList = contactsDto)
    prisonRegisterMockServer.stubGetPrisonNames(prisons)
    prisonApiMockServer.stubGetPrisonerRestrictions(PRISONER_ID, OffenderRestrictionsDto(bookingId = 1, listOf(prisonerRestrictionDto)))
    stubGetVisits(listOf(visit1, visit2))

    // When
    val responseSpec = callGetPrisonerProfile(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, PRISON_CODE, PRISONER_ID)

    // Then
    responseSpec.expectStatus().isNotFound

    verifyExternalAPIClientCalls()
  }

  @Test
  fun `when alerts API get alerts call returns returns a INTERNAL_SERVER_ERROR an exception is thrown and a INTERNAL_SERVER_ERROR is returned as response`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(PRISONER_ID, inmateDetailDto)
    visitAllocationApiMockServer.stubGetPrisonerVOBalanceDetailed(PRISONER_ID, visitBalancesDto)
    alertApiMockServer.stubGetPrisonerAlertsMono(PRISONER_ID, null, HttpStatus.INTERNAL_SERVER_ERROR)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, contactsList = contactsDto)
    prisonRegisterMockServer.stubGetPrisonNames(prisons)
    prisonApiMockServer.stubGetPrisonerRestrictions(PRISONER_ID, OffenderRestrictionsDto(bookingId = 1, listOf(prisonerRestrictionDto)))
    stubGetVisits(listOf(visit1, visit2))

    // When
    val responseSpec = callGetPrisonerProfile(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, PRISON_CODE, PRISONER_ID)

    // Then
    responseSpec.expectStatus().is5xxServerError

    verifyExternalAPIClientCalls()
  }

  @Test
  fun `when prison API get restrictions call returns returns a NOT_FOUND an exception is thrown and a NOT_FOUND is returned as response`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(PRISONER_ID, inmateDetailDto)
    visitAllocationApiMockServer.stubGetPrisonerVOBalanceDetailed(PRISONER_ID, visitBalancesDto)
    alertApiMockServer.stubGetPrisonerAlertsMono(PRISONER_ID, listOf(alertResponseDto))
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, contactsList = contactsDto)
    prisonRegisterMockServer.stubGetPrisonNames(prisons)
    prisonApiMockServer.stubGetPrisonerRestrictions(PRISONER_ID, null, HttpStatus.NOT_FOUND)
    stubGetVisits(listOf(visit1, visit2))

    // When
    val responseSpec = callGetPrisonerProfile(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, PRISON_CODE, PRISONER_ID)

    // Then
    responseSpec.expectStatus().isNotFound

    verifyExternalAPIClientCalls()
  }

  @Test
  fun `when prison API get restrictions call returns returns a INTERNAL_SERVER_ERROR an exception is thrown and a INTERNAL_SERVER_ERROR is returned as response`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(PRISONER_ID, inmateDetailDto)
    visitAllocationApiMockServer.stubGetPrisonerVOBalanceDetailed(PRISONER_ID, visitBalancesDto)
    alertApiMockServer.stubGetPrisonerAlertsMono(PRISONER_ID, listOf(alertResponseDto))
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, contactsList = contactsDto)
    prisonRegisterMockServer.stubGetPrisonNames(prisons)
    prisonApiMockServer.stubGetPrisonerRestrictions(PRISONER_ID, null, HttpStatus.INTERNAL_SERVER_ERROR)
    stubGetVisits(listOf(visit1, visit2))

    // When
    val responseSpec = callGetPrisonerProfile(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, PRISON_CODE, PRISONER_ID)

    // Then
    responseSpec.expectStatus().is5xxServerError

    verifyExternalAPIClientCalls()
  }

  @Test
  fun `when prisoner has alerts these alerts are sorted by updatedDate and then by createdDate`() {
    // Given

    // alert 1 - created 11 years back, updated 21 days back
    val alert1 = createAlertResponseDto(alertTypeCode = "A", lastModifiedAt = LocalDateTime.now().minusDays(21), createdAt = LocalDateTime.now().minusYears(10))
    // alert 2 - created 1 year back, updated 1 day back
    val alert2 = createAlertResponseDto(alertTypeCode = "B", lastModifiedAt = LocalDateTime.now().minusDays(1), createdAt = LocalDateTime.now().minusYears(1))
    // alert 3 - created 1 year back, not updated
    val alert3 = createAlertResponseDto(alertTypeCode = "C", lastModifiedAt = null, createdAt = LocalDateTime.now().minusYears(1))
    // alert 4 - created 2 year back, not updated
    val alert4 = createAlertResponseDto(alertTypeCode = "D", lastModifiedAt = null, createdAt = LocalDateTime.now().minusYears(2))
    // alert 5 - created 1 day back
    val alert5 = createAlertResponseDto(alertTypeCode = "E", lastModifiedAt = null, createdAt = LocalDateTime.now().minusDays(1).minusMinutes(1))
    // alert 6 - created 1 year back
    val alert6 = createAlertResponseDto(alertTypeCode = "F", lastModifiedAt = null, createdAt = LocalDateTime.now().minusYears(1).minusMinutes(1))
    // alert 7 - updated today, created last month
    val alert7 = createAlertResponseDto(alertTypeCode = "G", lastModifiedAt = LocalDateTime.now(), createdAt = LocalDateTime.now().minusMonths(1))
    // alert 7 - updated today, created today
    val alert8 = createAlertResponseDto(alertTypeCode = "H", lastModifiedAt = LocalDateTime.now().minusMinutes(1), createdAt = LocalDateTime.now().minusMinutes(1))
    // alert 9 - created 3 years back, not updated, active to is 2 months from today
    val alert9 = createAlertResponseDto(alertTypeCode = "I", lastModifiedAt = null, createdAt = LocalDateTime.now().minusYears(3), activeFrom = LocalDate.now().minusYears(3), activeTo = LocalDate.now().plusMonths(2))
    // alert 10 - created 3 years back, not updated, active to is 3 months from today
    val alert10 = createAlertResponseDto(alertTypeCode = "J", lastModifiedAt = null, createdAt = LocalDateTime.now().minusYears(3), activeFrom = LocalDate.now().minusYears(3), activeTo = LocalDate.now().plusMonths(3))
    // alert 11 - created 3 years back, not updated, active from is 2 years back
    val alert11 = createAlertResponseDto(alertTypeCode = "K", lastModifiedAt = null, createdAt = LocalDateTime.now().minusYears(3), activeFrom = LocalDate.now().minusYears(2), activeTo = null)

    // expected sort order is alert7, alert8, alert2, alert5,  alert1, alert3, alert6, alert4, alert11, alert10, alert9 - G,H,B,E,A,C,F,D,K,J,I
    val expectedAlerts = listOf(AlertDto(alert7), AlertDto(alert8), AlertDto(alert2), AlertDto(alert5), AlertDto(alert1), AlertDto(alert3), AlertDto(alert6), AlertDto(alert4), AlertDto(alert11), AlertDto(alert10), AlertDto(alert9))

    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(PRISONER_ID, inmateDetailDto)
    visitAllocationApiMockServer.stubGetPrisonerVOBalanceDetailed(PRISONER_ID, visitBalancesDto)
    alertApiMockServer.stubGetPrisonerAlertsMono(PRISONER_ID, listOf(alert1, alert2, alert3, alert4, alert5, alert6, alert7, alert8, alert9, alert10, alert11))
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, contactsList = contactsDto)
    prisonRegisterMockServer.stubGetPrisonNames(prisons)
    prisonApiMockServer.stubGetPrisonerRestrictions(PRISONER_ID, OffenderRestrictionsDto(bookingId = 1, listOf(prisonerRestrictionDto)))
    stubGetVisits(listOf(visit1, visit2))

    // When
    val responseSpec = callGetPrisonerProfile(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, PRISON_CODE, PRISONER_ID)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerProfile = getResults(returnResult)

    assertPrisonerDtoDetails(prisonerProfile, prisonerDto)
    Assertions.assertThat(prisonerProfile.incentiveLevel).isEqualTo(prisonerDto.currentIncentive!!.level.description)
    assertInmateDetails(prisonerProfile, inmateDetailDto)
    Assertions.assertThat(prisonerProfile.visitBalances).isEqualTo(expectedVisitBalances)
    Assertions.assertThat(prisonerProfile.visits.size).isEqualTo(2)
    Assertions.assertThat(prisonerProfile.alerts).isEqualTo(expectedAlerts)
    Assertions.assertThat(prisonerProfile.prisonerRestrictions).isEqualTo(listOf(prisonerRestrictionDto))
    assertVisits(prisonerProfile, listOf(visit1, visit2))

    verifyExternalAPIClientCalls()
  }

  @Test
  fun `when prisoner has restrictions these restrictions are sorted by startDate in descending order`() {
    // Given
    // restriction 1 - start date today
    val restriction1 = createOffenderRestrictionDto(restrictionId = 1, startDate = LocalDate.now(), expiryDate = null)
    // restriction 2 - start date tomorrow
    val restriction2 = createOffenderRestrictionDto(restrictionId = 2, startDate = LocalDate.now().plusDays(1), expiryDate = null)
    // restriction 3 - start date 1 month back, expiry date tomorrow
    val restriction3 = createOffenderRestrictionDto(restrictionId = 3, startDate = LocalDate.now().minusMonths(1), expiryDate = LocalDate.now().plusDays(1))
    // restriction 4 - start date 5 days back, expiry date 3 months ahead
    val restriction4 = createOffenderRestrictionDto(restrictionId = 4, startDate = LocalDate.now().minusDays(5), expiryDate = LocalDate.now().plusMonths(3))
    // restriction 5 - start date 5 days back, expiry date 2 days ahead
    val restriction5 = createOffenderRestrictionDto(restrictionId = 5, startDate = LocalDate.now().minusDays(5), expiryDate = LocalDate.now().plusDays(2))
    // restriction 6 - start date 5 days back, expiry date 3 days ahead
    val restriction6 = createOffenderRestrictionDto(restrictionId = 6, startDate = LocalDate.now().minusDays(5), expiryDate = LocalDate.now().plusDays(3))

    // expected sort order is restriction2, restriction1, restriction4, restriction6, restriction5, restriction3
    val expectedRestrictions = listOf(restriction2, restriction1, restriction4, restriction6, restriction5, restriction3)

    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)
    prisonApiMockServer.stubGetInmateDetails(PRISONER_ID, inmateDetailDto)
    visitAllocationApiMockServer.stubGetPrisonerVOBalanceDetailed(PRISONER_ID, visitBalancesDto)
    alertApiMockServer.stubGetPrisonerAlertsMono(PRISONER_ID, listOf(alertResponseDto))
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, contactsList = contactsDto)
    prisonRegisterMockServer.stubGetPrisonNames(prisons)
    prisonApiMockServer.stubGetPrisonerRestrictions(PRISONER_ID, OffenderRestrictionsDto(bookingId = 1, listOf(restriction1, restriction2, restriction3, restriction4, restriction5, restriction6)))
    stubGetVisits(listOf(visit1, visit2))

    // When
    val responseSpec = callGetPrisonerProfile(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, PRISON_CODE, PRISONER_ID)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerProfile = getResults(returnResult)

    assertPrisonerDtoDetails(prisonerProfile, prisonerDto)
    Assertions.assertThat(prisonerProfile.incentiveLevel).isEqualTo(prisonerDto.currentIncentive!!.level.description)
    assertInmateDetails(prisonerProfile, inmateDetailDto)
    Assertions.assertThat(prisonerProfile.visitBalances).isEqualTo(expectedVisitBalances)
    Assertions.assertThat(prisonerProfile.visits.size).isEqualTo(2)
    Assertions.assertThat(prisonerProfile.alerts).isEqualTo(listOf(alert))
    Assertions.assertThat(prisonerProfile.prisonerRestrictions).isEqualTo(expectedRestrictions)
    assertVisits(prisonerProfile, listOf(visit1, visit2))

    verifyExternalAPIClientCalls()
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

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): PrisonerProfileDto = TestObjectMapper.mapper.readValue(returnResult.returnResult().responseBody, PrisonerProfileDto::class.java)

  private fun createPrisonerVoBalanceDto(): PrisonerVOBalanceDetailedDto = PrisonerVOBalanceDetailedDto(
    prisonerId = "prisonerId",
    availableVos = 8,
    accumulatedVos = 4,
    negativeVos = 2,
    voBalance = 10,
    availablePvos = 10,
    negativePvos = 0,
    pvoBalance = 10,
    lastVoAllocatedDate = LocalDate.now(),
    nextVoAllocationDate = LocalDate.now().plusDays(14),
    lastPvoAllocatedDate = LocalDate.now(),
    nextPvoAllocationDate = LocalDate.now().plusDays(28),
  )

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
    verify(prisonApiClientSpy, times(1)).getInmateDetailsAsMono(any())
    verify(visitAllocationApiClientSpy, times(1)).getPrisonerVOBalanceDetailedAsMono(any())
    verify(alertsApiClientSpy, times(1)).getPrisonerAlertsAsMono(any())
    verify(prisonApiClientSpy, times(1)).getPrisonerRestrictionsAsMono(any())
  }
}
