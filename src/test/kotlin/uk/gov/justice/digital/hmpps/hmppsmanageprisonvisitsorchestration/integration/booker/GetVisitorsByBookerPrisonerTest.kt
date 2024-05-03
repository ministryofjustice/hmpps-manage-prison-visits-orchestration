package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.booker

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonVisitBookerRegistryClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonerContactRegistryClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitSchedulerClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.BookerPrisonerVisitorsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.BookerPrisonersDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.RestrictionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.VisitorInfoDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.PrisonUserClientDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import java.time.LocalDate

@DisplayName("Get visitors by booker's prisoner")
class GetVisitorsByBookerPrisonerTest : IntegrationTestBase() {
  @SpyBean
  lateinit var prisonVisitBookerRegistryClientSpy: PrisonVisitBookerRegistryClient

  @SpyBean
  lateinit var prisonerContactRegistryClientSpy: PrisonerContactRegistryClient

  @SpyBean
  lateinit var visitSchedulerClientSpy: VisitSchedulerClient

  @SpyBean
  lateinit var prisonerSearchClientSpy: PrisonerSearchClient

  companion object {
    private val INDEFINITELY_BANNED_RESTRICTION = RestrictionDto(restrictionType = "BAN", restrictionTypeDescription = "BANNED", startDate = LocalDate.now(), globalRestriction = true)
    private val EXPIRED_BANNED_RESTRICTION = RestrictionDto(restrictionType = "BAN", restrictionTypeDescription = "BANNED", startDate = LocalDate.now().minusDays(3), expiryDate = LocalDate.now().minusDays(1), globalRestriction = true)
    private val EXPIRING_IN_3_WEEKS_BANNED_RESTRICTION = RestrictionDto(restrictionType = "BAN", restrictionTypeDescription = "BANNED", startDate = LocalDate.now().minusDays(31), expiryDate = LocalDate.now().plusWeeks(3), globalRestriction = true)
    private val EXPIRING_IN_6_WEEKS_BANNED_RESTRICTION = RestrictionDto(restrictionType = "BAN", restrictionTypeDescription = "BANNED", startDate = LocalDate.now().minusDays(31), expiryDate = LocalDate.now().plusWeeks(6), globalRestriction = true)
    private val OTHER_RESTRICTION = RestrictionDto(restrictionType = "OTHER", restrictionTypeDescription = "BANNED", startDate = LocalDate.now().minusDays(31), expiryDate = null, globalRestriction = true)

    private const val PRISON_CODE = "HEI"
    private const val BOOKER_REFERENCE = "booker-1"
    private const val PRISONER_ID = "AA112233B"
  }

  val bookerRegistryPrisonerDto = BookerPrisonersDto(PRISONER_ID)

  val prisonDto = createPrison(prisonCode = PRISON_CODE)

  private final val prisonerDto = createPrisoner(
    prisonerId = PRISONER_ID,
    firstName = "FirstName",
    lastName = "LastName",
    dateOfBirth = LocalDate.of(2000, 1, 31),
    prisonId = PRISON_CODE,
  )

  private final val prisoner2Dto = createPrisoner(
    prisonerId = PRISONER_ID,
    firstName = "FirstName",
    lastName = "LastName",
    dateOfBirth = LocalDate.of(2000, 1, 31),
    prisonId = null,
  )

  private val adultVisitor = createVisitor(
    firstName = "First",
    lastName = "VisitorA",
    dateOfBirth = LocalDate.now().minusYears(19),
  )

  private val childVisitor = createVisitor(
    firstName = "Second",
    lastName = "VisitorB",
    dateOfBirth = LocalDate.now().minusYears(4),
  )

  private val noDOBVisitor = createVisitor(
    firstName = "Third",
    lastName = "VisitorC",
    dateOfBirth = null,
  )

  private val unapprovedVisitor = createVisitor(
    firstName = "Fourth",
    lastName = "VisitorD",
    dateOfBirth = LocalDate.of(1990, 4, 1),
    approved = false,
  )

  private val indefinitelyBannedVisitor = createVisitor(
    firstName = "Fifth",
    lastName = "VisitorE",
    dateOfBirth = LocalDate.of(1990, 4, 1),
    restrictions = listOf(INDEFINITELY_BANNED_RESTRICTION, OTHER_RESTRICTION),
  )

  private val bannedVisitorForNext6Weeks = createVisitor(
    firstName = "Sixth",
    lastName = "VisitorE",
    dateOfBirth = LocalDate.of(1990, 4, 1),
    restrictions = listOf(EXPIRING_IN_6_WEEKS_BANNED_RESTRICTION, OTHER_RESTRICTION),
  )

  private val bannedVisitorForNext3Weeks = createVisitor(
    firstName = "Seventh",
    lastName = "VisitorE",
    dateOfBirth = LocalDate.of(1990, 4, 1),
    restrictions = listOf(EXPIRING_IN_3_WEEKS_BANNED_RESTRICTION, OTHER_RESTRICTION),
  )

  private val multipleBansVisitor = createVisitor(
    firstName = "Seventh",
    lastName = "VisitorE",
    dateOfBirth = LocalDate.of(1990, 4, 1),
    restrictions = listOf(EXPIRING_IN_3_WEEKS_BANNED_RESTRICTION, EXPIRING_IN_6_WEEKS_BANNED_RESTRICTION, OTHER_RESTRICTION),
  )

  private val expiredBanVisitor = createVisitor(
    firstName = "Seventh",
    lastName = "VisitorE",
    dateOfBirth = LocalDate.of(1990, 4, 1),
    restrictions = listOf(EXPIRED_BANNED_RESTRICTION, OTHER_RESTRICTION),
  )

  private val contactsList = createContactsList(
    listOf(
      adultVisitor,
      childVisitor,
      noDOBVisitor,
      unapprovedVisitor,
      indefinitelyBannedVisitor,
      bannedVisitorForNext3Weeks,
      bannedVisitorForNext6Weeks,
      multipleBansVisitor,
      expiredBanVisitor,
    ),
  )

  fun callGetVisitorsByBookersPrisoner(
    webTestClient: WebTestClient,
    authHttpHeaders: (HttpHeaders) -> Unit,
    bookerReference: String,
    prisonerNumber: String,
  ): WebTestClient.ResponseSpec {
    return webTestClient.get().uri("/public/booker/$bookerReference/prisoners/$prisonerNumber/visitors")
      .headers(authHttpHeaders)
      .exchange()
  }

  @Test
  fun `when booker's prisoners has valid visitors then all allowed visitors are returned`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, contactsList)
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(BOOKER_REFERENCE, listOf(bookerRegistryPrisonerDto))
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisonerVisitors(
      BOOKER_REFERENCE,
      PRISONER_ID,
      listOf(
        BookerPrisonerVisitorsDto(PRISONER_ID, adultVisitor.personId),
        BookerPrisonerVisitorsDto(PRISONER_ID, childVisitor.personId),
      ),
    )

    // When
    val responseSpec = callGetVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE, PRISONER_ID)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(2)
    assertVisitorContactBasicDetails(prisonerDetailsList[0], adultVisitor)
    assertVisitorContactBasicDetails(prisonerDetailsList[1], childVisitor)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPrisonersForBooker(BOOKER_REFERENCE)
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getVisitorsForBookersAssociatedPrisoner(BOOKER_REFERENCE, PRISONER_ID)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerById(PRISONER_ID)
    verify(visitSchedulerClientSpy, times(1)).getPrison(PRISON_CODE)
    verify(prisonerContactRegistryClientSpy, times(1)).getPrisonersSocialContacts(PRISONER_ID, false)
  }

  @Test
  fun `when booker's prisoners has no valid visitors then no visitors are returned`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, contactsList)
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(BOOKER_REFERENCE, listOf(bookerRegistryPrisonerDto))
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisonerVisitors(
      BOOKER_REFERENCE,
      PRISONER_ID,
      emptyList(),
    )

    // When
    val responseSpec = callGetVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE, PRISONER_ID)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(0)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPrisonersForBooker(BOOKER_REFERENCE)
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getVisitorsForBookersAssociatedPrisoner(BOOKER_REFERENCE, PRISONER_ID)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerById(PRISONER_ID)
    verify(visitSchedulerClientSpy, times(1)).getPrison(PRISON_CODE)
    verify(prisonerContactRegistryClientSpy, times(0)).getPrisonersSocialContacts(PRISONER_ID, false)
  }

  @Test
  fun `when booker's prisoners has valid visitors but one of them has no date of birth then that visitor is not returned`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, contactsList)
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(BOOKER_REFERENCE, listOf(bookerRegistryPrisonerDto))
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisonerVisitors(
      BOOKER_REFERENCE,
      PRISONER_ID,
      listOf(
        BookerPrisonerVisitorsDto(PRISONER_ID, adultVisitor.personId),
        BookerPrisonerVisitorsDto(PRISONER_ID, noDOBVisitor.personId),
      ),
    )

    // When
    val responseSpec = callGetVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE, PRISONER_ID)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(1)
    assertVisitorContactBasicDetails(prisonerDetailsList[0], adultVisitor)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPrisonersForBooker(BOOKER_REFERENCE)
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getVisitorsForBookersAssociatedPrisoner(BOOKER_REFERENCE, PRISONER_ID)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerById(PRISONER_ID)
    verify(visitSchedulerClientSpy, times(1)).getPrison(PRISON_CODE)
    verify(prisonerContactRegistryClientSpy, times(1)).getPrisonersSocialContacts(PRISONER_ID, false)
  }

  @Test
  fun `when booker's prisoners has valid visitors but one of them is not approved then that visitor is not returned`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, contactsList)
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(BOOKER_REFERENCE, listOf(bookerRegistryPrisonerDto))
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisonerVisitors(
      BOOKER_REFERENCE,
      PRISONER_ID,
      listOf(
        BookerPrisonerVisitorsDto(PRISONER_ID, adultVisitor.personId),
        BookerPrisonerVisitorsDto(PRISONER_ID, unapprovedVisitor.personId),
      ),
    )

    // When
    val responseSpec = callGetVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE, PRISONER_ID)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(1)
    assertVisitorContactBasicDetails(prisonerDetailsList[0], adultVisitor)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPrisonersForBooker(BOOKER_REFERENCE)
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getVisitorsForBookersAssociatedPrisoner(BOOKER_REFERENCE, PRISONER_ID)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerById(PRISONER_ID)
    verify(visitSchedulerClientSpy, times(1)).getPrison(PRISON_CODE)
    verify(prisonerContactRegistryClientSpy, times(1)).getPrisonersSocialContacts(PRISONER_ID, false)
  }

  @Test
  fun `when booker's prisoners has valid visitors but one of them is BANNED with no end date then that visitor is not returned`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, contactsList)
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(BOOKER_REFERENCE, listOf(bookerRegistryPrisonerDto))
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisonerVisitors(
      BOOKER_REFERENCE,
      PRISONER_ID,
      listOf(
        BookerPrisonerVisitorsDto(PRISONER_ID, adultVisitor.personId),
        BookerPrisonerVisitorsDto(PRISONER_ID, indefinitelyBannedVisitor.personId),
      ),
    )

    // When
    val responseSpec = callGetVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE, PRISONER_ID)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(1)
    assertVisitorContactBasicDetails(prisonerDetailsList[0], adultVisitor)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPrisonersForBooker(BOOKER_REFERENCE)
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getVisitorsForBookersAssociatedPrisoner(BOOKER_REFERENCE, PRISONER_ID)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerById(PRISONER_ID)
    verify(visitSchedulerClientSpy, times(1)).getPrison(PRISON_CODE)
    verify(prisonerContactRegistryClientSpy, times(1)).getPrisonersSocialContacts(PRISONER_ID, false)
  }

  @Test
  fun `when booker's prisoners has valid visitors but one of them is BANNED with end date after prisons max session days then that visitor is not returned`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, contactsList)
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(BOOKER_REFERENCE, listOf(bookerRegistryPrisonerDto))
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisonerVisitors(
      BOOKER_REFERENCE,
      PRISONER_ID,
      listOf(
        BookerPrisonerVisitorsDto(PRISONER_ID, adultVisitor.personId),
        BookerPrisonerVisitorsDto(PRISONER_ID, bannedVisitorForNext6Weeks.personId),
      ),
    )

    // When
    val responseSpec = callGetVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE, PRISONER_ID)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(1)
    assertVisitorContactBasicDetails(prisonerDetailsList[0], adultVisitor)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPrisonersForBooker(BOOKER_REFERENCE)
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getVisitorsForBookersAssociatedPrisoner(BOOKER_REFERENCE, PRISONER_ID)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerById(PRISONER_ID)
    verify(visitSchedulerClientSpy, times(1)).getPrison(PRISON_CODE)
    verify(prisonerContactRegistryClientSpy, times(1)).getPrisonersSocialContacts(PRISONER_ID, false)
  }

  @Test
  fun `when booker's prisoners has valid visitors but one of them is BANNED with end date before prisons max session days then that visitor is returned`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, contactsList)
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(BOOKER_REFERENCE, listOf(bookerRegistryPrisonerDto))

    prisonVisitBookerRegistryMockServer.stubGetBookersPrisonerVisitors(
      BOOKER_REFERENCE,
      PRISONER_ID,
      listOf(
        BookerPrisonerVisitorsDto(PRISONER_ID, adultVisitor.personId),
        BookerPrisonerVisitorsDto(PRISONER_ID, bannedVisitorForNext3Weeks.personId),
      ),
    )

    // When
    val responseSpec = callGetVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE, PRISONER_ID)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(2)
    assertVisitorContactBasicDetails(prisonerDetailsList[0], adultVisitor)
    assertVisitorContactBasicDetails(prisonerDetailsList[1], bannedVisitorForNext3Weeks)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPrisonersForBooker(BOOKER_REFERENCE)
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getVisitorsForBookersAssociatedPrisoner(BOOKER_REFERENCE, PRISONER_ID)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerById(PRISONER_ID)
    verify(visitSchedulerClientSpy, times(1)).getPrison(PRISON_CODE)
    verify(prisonerContactRegistryClientSpy, times(1)).getPrisonersSocialContacts(PRISONER_ID, false)
  }

  @Test
  fun `when booker's prisoners has valid visitors but one of them has multiple bans with end date but one after prisons max session days then that visitor is not returned`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, contactsList)
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(BOOKER_REFERENCE, listOf(bookerRegistryPrisonerDto))
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisonerVisitors(
      BOOKER_REFERENCE,
      PRISONER_ID,
      listOf(
        BookerPrisonerVisitorsDto(PRISONER_ID, adultVisitor.personId),
        BookerPrisonerVisitorsDto(PRISONER_ID, multipleBansVisitor.personId),
      ),
    )

    // When
    val responseSpec = callGetVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE, PRISONER_ID)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(1)
    assertVisitorContactBasicDetails(prisonerDetailsList[0], adultVisitor)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPrisonersForBooker(BOOKER_REFERENCE)
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getVisitorsForBookersAssociatedPrisoner(BOOKER_REFERENCE, PRISONER_ID)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerById(PRISONER_ID)
    verify(visitSchedulerClientSpy, times(1)).getPrison(PRISON_CODE)
    verify(prisonerContactRegistryClientSpy, times(1)).getPrisonersSocialContacts(PRISONER_ID, false)
  }

  @Test
  fun `when booker's prisoners has valid visitors but one of them has expired ban then that visitor is returned`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, contactsList)
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(BOOKER_REFERENCE, listOf(bookerRegistryPrisonerDto))
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisonerVisitors(
      BOOKER_REFERENCE,
      PRISONER_ID,
      listOf(
        BookerPrisonerVisitorsDto(PRISONER_ID, adultVisitor.personId),
        BookerPrisonerVisitorsDto(PRISONER_ID, expiredBanVisitor.personId),
      ),
    )

    // When
    val responseSpec = callGetVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE, PRISONER_ID)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(2)
    assertVisitorContactBasicDetails(prisonerDetailsList[0], adultVisitor)
    assertVisitorContactBasicDetails(prisonerDetailsList[1], expiredBanVisitor)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPrisonersForBooker(BOOKER_REFERENCE)
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getVisitorsForBookersAssociatedPrisoner(BOOKER_REFERENCE, PRISONER_ID)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerById(PRISONER_ID)
    verify(visitSchedulerClientSpy, times(1)).getPrison(PRISON_CODE)
    verify(prisonerContactRegistryClientSpy, times(1)).getPrisonersSocialContacts(PRISONER_ID, false)
  }

  @Test
  fun `when prisoner not returned for booker from booker registry then BAD_REQUEST status is sent back`() {
    // Given
    // prison visit booker registry returns 404
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(
      BOOKER_REFERENCE,
      null,
    )
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, contactsList)

    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)

    // When
    val responseSpec = callGetVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE, PRISONER_ID)

    // Then
    responseSpec.expectStatus().isBadRequest
    assertErrorResult(responseSpec, HttpStatus.BAD_REQUEST, "Prisoner with number - $PRISONER_ID not found for booker reference - $BOOKER_REFERENCE")

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPrisonersForBooker(BOOKER_REFERENCE)
    verify(prisonVisitBookerRegistryClientSpy, times(0)).getVisitorsForBookersAssociatedPrisoner(BOOKER_REFERENCE, PRISONER_ID)
    verify(prisonerSearchClientSpy, times(0)).getPrisonerById(PRISONER_ID)
    verify(visitSchedulerClientSpy, times(0)).getPrison(PRISON_CODE)
    verify(prisonerContactRegistryClientSpy, times(0)).getPrisonersSocialContacts(PRISONER_ID, false)
  }

  @Test
  fun `when prisoner is in a prison not on VSIP then BAD_REQUEST status is sent back`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(BOOKER_REFERENCE, listOf(bookerRegistryPrisonerDto))
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisonerVisitors(
      BOOKER_REFERENCE,
      PRISONER_ID,
      listOf(
        BookerPrisonerVisitorsDto(PRISONER_ID, adultVisitor.personId),
        BookerPrisonerVisitorsDto(PRISONER_ID, expiredBanVisitor.personId),
      ),
    )
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, contactsList)
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, null)

    // When
    val responseSpec = callGetVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE, PRISONER_ID)

    // Then
    responseSpec.expectStatus().isBadRequest
    assertErrorResult(responseSpec, HttpStatus.BAD_REQUEST, "prison validation for prison code - $PRISON_CODE for prisoner number - $PRISONER_ID failed with error - Prison with code - $PRISON_CODE, not found on visit-scheduler")
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPrisonersForBooker(BOOKER_REFERENCE)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerById(PRISONER_ID)
    verify(prisonVisitBookerRegistryClientSpy, times(0)).getVisitorsForBookersAssociatedPrisoner(BOOKER_REFERENCE, PRISONER_ID)
    verify(visitSchedulerClientSpy, times(1)).getPrison(PRISON_CODE)
    verify(prisonerContactRegistryClientSpy, times(0)).getPrisonersSocialContacts(PRISONER_ID, false)
  }

  @Test
  fun `when prisoner is in a prison not active on VSIP then BAD_REQUEST status is sent back`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(BOOKER_REFERENCE, listOf(bookerRegistryPrisonerDto))
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisonerVisitors(
      BOOKER_REFERENCE,
      PRISONER_ID,
      listOf(
        BookerPrisonerVisitorsDto(PRISONER_ID, adultVisitor.personId),
        BookerPrisonerVisitorsDto(PRISONER_ID, expiredBanVisitor.personId),
      ),
    )
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, contactsList)
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, createPrison(PRISON_CODE, active = false))

    // When
    val responseSpec = callGetVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE, PRISONER_ID)

    // Then
    responseSpec.expectStatus().isBadRequest
    assertErrorResult(responseSpec, HttpStatus.BAD_REQUEST, "prison validation for prison code - $PRISON_CODE for prisoner number - $PRISONER_ID failed with error - Prison with code - $PRISON_CODE, is not active on visit-scheduler")
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPrisonersForBooker(BOOKER_REFERENCE)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerById(PRISONER_ID)
    verify(prisonVisitBookerRegistryClientSpy, times(0)).getVisitorsForBookersAssociatedPrisoner(BOOKER_REFERENCE, PRISONER_ID)
    verify(visitSchedulerClientSpy, times(1)).getPrison(PRISON_CODE)
    verify(prisonerContactRegistryClientSpy, times(0)).getPrisonersSocialContacts(PRISONER_ID, false)
  }

  @Test
  fun `when prisoner is in a prison only active for staff UI on VSIP then BAD_REQUEST status is sent back`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(BOOKER_REFERENCE, listOf(bookerRegistryPrisonerDto))
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisonerVisitors(
      BOOKER_REFERENCE,
      PRISONER_ID,
      listOf(
        BookerPrisonerVisitorsDto(PRISONER_ID, adultVisitor.personId),
        BookerPrisonerVisitorsDto(PRISONER_ID, expiredBanVisitor.personId),
      ),
    )
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, contactsList)
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)

    // prison only active for STAFF
    visitSchedulerMockServer.stubGetPrison(
      PRISON_CODE,
      createPrison(
        PRISON_CODE,
        active = true,
        clients = listOf(PrisonUserClientDto(UserType.STAFF, true)),
      ),
    )

    // When
    val responseSpec = callGetVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE, PRISONER_ID)

    // Then
    responseSpec.expectStatus().isBadRequest
    assertErrorResult(responseSpec, HttpStatus.BAD_REQUEST, "prison validation for prison code - $PRISON_CODE for prisoner number - $PRISONER_ID failed with error - Prison with code - $PRISON_CODE, is not active for public users on visit-scheduler")
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPrisonersForBooker(BOOKER_REFERENCE)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerById(PRISONER_ID)
    verify(prisonVisitBookerRegistryClientSpy, times(0)).getVisitorsForBookersAssociatedPrisoner(BOOKER_REFERENCE, PRISONER_ID)
    verify(visitSchedulerClientSpy, times(1)).getPrison(PRISON_CODE)
    verify(prisonerContactRegistryClientSpy, times(0)).getPrisonersSocialContacts(PRISONER_ID, false)
  }

  @Test
  fun `when prisoner is in a prison not active for staff UI on VSIP then BAD_REQUEST status is sent back`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(BOOKER_REFERENCE, listOf(bookerRegistryPrisonerDto))
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisonerVisitors(
      BOOKER_REFERENCE,
      PRISONER_ID,
      listOf(
        BookerPrisonerVisitorsDto(PRISONER_ID, adultVisitor.personId),
        BookerPrisonerVisitorsDto(PRISONER_ID, expiredBanVisitor.personId),
      ),
    )
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, contactsList)
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)

    // prison only active for STAFF
    visitSchedulerMockServer.stubGetPrison(
      PRISON_CODE,
      createPrison(
        PRISON_CODE,
        active = true,
        clients = listOf(PrisonUserClientDto(UserType.PUBLIC, false)),
      ),
    )

    // When
    val responseSpec = callGetVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE, PRISONER_ID)

    // Then
    responseSpec.expectStatus().isBadRequest
    assertErrorResult(responseSpec, HttpStatus.BAD_REQUEST, "prison validation for prison code - $PRISON_CODE for prisoner number - $PRISONER_ID failed with error - Prison with code - $PRISON_CODE, is not active for public users on visit-scheduler")
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPrisonersForBooker(BOOKER_REFERENCE)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerById(PRISONER_ID)
    verify(prisonVisitBookerRegistryClientSpy, times(0)).getVisitorsForBookersAssociatedPrisoner(BOOKER_REFERENCE, PRISONER_ID)
    verify(visitSchedulerClientSpy, times(1)).getPrison(PRISON_CODE)
    verify(prisonerContactRegistryClientSpy, times(0)).getPrisonersSocialContacts(PRISONER_ID, false)
  }

  @Test
  fun `when NOT_FOUND is returned from booker registry then NOT_FOUND status is sent back`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(BOOKER_REFERENCE, listOf(bookerRegistryPrisonerDto))
    // prison visit booker registry returns 404 on get visitors call
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisonerVisitors(
      BOOKER_REFERENCE,
      PRISONER_ID,
      null,
      HttpStatus.NOT_FOUND,
    )

    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, contactsList)
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)

    // When
    val responseSpec = callGetVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE, PRISONER_ID)

    // Then
    responseSpec.expectStatus().isNotFound

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPrisonersForBooker(BOOKER_REFERENCE)
    verify(visitSchedulerClientSpy, times(1)).getPrison(PRISON_CODE)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerById(PRISONER_ID)
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getVisitorsForBookersAssociatedPrisoner(BOOKER_REFERENCE, PRISONER_ID)
    verify(prisonerContactRegistryClientSpy, times(0)).getPrisonersSocialContacts(PRISONER_ID, false)
  }

  @Test
  fun `when INTERNAL_SERVER_ERROR is returned from booker registry then INTERNAL_SERVER_ERROR status is sent back`() {
    // Given
    // prison visit booker registry returns 404
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisonerVisitors(
      BOOKER_REFERENCE,
      PRISONER_ID,
      null,
      HttpStatus.INTERNAL_SERVER_ERROR,
    )

    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, contactsList)
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)

    // When
    val responseSpec = callGetVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE, PRISONER_ID)

    // Then
    responseSpec.expectStatus().is5xxServerError

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPrisonersForBooker(BOOKER_REFERENCE)
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getVisitorsForBookersAssociatedPrisoner(BOOKER_REFERENCE, PRISONER_ID)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerById(PRISONER_ID)
    verify(visitSchedulerClientSpy, times(1)).getPrison(PRISON_CODE)
    verify(prisonerContactRegistryClientSpy, times(0)).getPrisonersSocialContacts(PRISONER_ID, false)
  }

  @Test
  fun `when NOT_FOUND is returned from prisoner contact registry then empty visitor list is returned`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(BOOKER_REFERENCE, listOf(bookerRegistryPrisonerDto))
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)

    prisonVisitBookerRegistryMockServer.stubGetBookersPrisonerVisitors(
      BOOKER_REFERENCE,
      PRISONER_ID,
      listOf(
        BookerPrisonerVisitorsDto(PRISONER_ID, adultVisitor.personId),
        BookerPrisonerVisitorsDto(PRISONER_ID, childVisitor.personId),
      ),
    )

    // prisoner contact registry returns 404
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, null, HttpStatus.NOT_FOUND)

    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)

    // When
    val responseSpec = callGetVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE, PRISONER_ID)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(0)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPrisonersForBooker(BOOKER_REFERENCE)
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getVisitorsForBookersAssociatedPrisoner(BOOKER_REFERENCE, PRISONER_ID)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerById(PRISONER_ID)
    verify(visitSchedulerClientSpy, times(1)).getPrison(PRISON_CODE)
    verify(prisonerContactRegistryClientSpy, times(1)).getPrisonersSocialContacts(PRISONER_ID, false)
  }

  @Test
  fun `when INTERNAL_SERVER_ERROR is returned from prisoner contact registry then INTERNAL_SERVER_ERROR status code is sent back`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(BOOKER_REFERENCE, listOf(bookerRegistryPrisonerDto))
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)

    prisonVisitBookerRegistryMockServer.stubGetBookersPrisonerVisitors(
      BOOKER_REFERENCE,
      PRISONER_ID,
      listOf(
        BookerPrisonerVisitorsDto(PRISONER_ID, adultVisitor.personId),
        BookerPrisonerVisitorsDto(PRISONER_ID, childVisitor.personId),
      ),
    )

    // prisoner contact registry returns INTERNAL_SERVER_ERROR
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, null, HttpStatus.INTERNAL_SERVER_ERROR)

    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)

    // When
    val responseSpec = callGetVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE, PRISONER_ID)

    // Then
    responseSpec.expectStatus().is5xxServerError

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPrisonersForBooker(BOOKER_REFERENCE)
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getVisitorsForBookersAssociatedPrisoner(BOOKER_REFERENCE, PRISONER_ID)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerById(PRISONER_ID)
    verify(prisonerContactRegistryClientSpy, times(1)).getPrisonersSocialContacts(PRISONER_ID, false)
    verify(visitSchedulerClientSpy, times(1)).getPrison(PRISON_CODE)
  }

  @Test
  fun `when 404 is returned from prisoner offender search for prisoner then BAD_REQUEST status code is sent back`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(BOOKER_REFERENCE, listOf(bookerRegistryPrisonerDto))

    // 404 is returned from prisoner search
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, null)

    prisonVisitBookerRegistryMockServer.stubGetBookersPrisonerVisitors(
      BOOKER_REFERENCE,
      PRISONER_ID,
      listOf(
        BookerPrisonerVisitorsDto(PRISONER_ID, adultVisitor.personId),
        BookerPrisonerVisitorsDto(PRISONER_ID, childVisitor.personId),
      ),
    )

    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, contactsList)

    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)

    // When
    val responseSpec = callGetVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE, PRISONER_ID)

    // Then
    responseSpec.expectStatus().isBadRequest
    assertErrorResult(responseSpec, HttpStatus.BAD_REQUEST, "prisoner validation for prisoner number - $PRISONER_ID failed with error - Prisoner with id - $PRISONER_ID not found on offender search")

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPrisonersForBooker(BOOKER_REFERENCE)
    verify(prisonVisitBookerRegistryClientSpy, times(0)).getVisitorsForBookersAssociatedPrisoner(BOOKER_REFERENCE, PRISONER_ID)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerById(PRISONER_ID)
    verify(prisonerContactRegistryClientSpy, times(0)).getPrisonersSocialContacts(PRISONER_ID, false)
    verify(visitSchedulerClientSpy, times(0)).getPrison(PRISON_CODE)
  }

  @Test
  fun `when prisoner is returned from prisoner offender search with null prisonId for prisoner then BAD_REQUEST status code is sent back`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(BOOKER_REFERENCE, listOf(bookerRegistryPrisonerDto))

    // prisoner has prison ID as null
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisoner2Dto)

    prisonVisitBookerRegistryMockServer.stubGetBookersPrisonerVisitors(
      BOOKER_REFERENCE,
      PRISONER_ID,
      listOf(
        BookerPrisonerVisitorsDto(PRISONER_ID, adultVisitor.personId),
        BookerPrisonerVisitorsDto(PRISONER_ID, childVisitor.personId),
      ),
    )

    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, contactsList)

    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)

    // When
    val responseSpec = callGetVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE, PRISONER_ID)

    // Then
    responseSpec.expectStatus().isBadRequest
    assertErrorResult(responseSpec, HttpStatus.BAD_REQUEST, "prisoner validation for prisoner number - $PRISONER_ID failed with error - Prisoner - $PRISONER_ID on prisoner search does not have a valid prison")

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPrisonersForBooker(BOOKER_REFERENCE)
    verify(prisonVisitBookerRegistryClientSpy, times(0)).getVisitorsForBookersAssociatedPrisoner(BOOKER_REFERENCE, PRISONER_ID)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerById(PRISONER_ID)
    verify(prisonerContactRegistryClientSpy, times(0)).getPrisonersSocialContacts(PRISONER_ID, false)
    verify(visitSchedulerClientSpy, times(0)).getPrison(PRISON_CODE)
  }

  @Test
  fun `when INTERNAL_SERVER_ERROR is returned from prisoner offender search for prisoner then BAD_REQUEST status code is sent back`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(BOOKER_REFERENCE, listOf(bookerRegistryPrisonerDto))

    // 404 is returned from prisoner search
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, null, HttpStatus.INTERNAL_SERVER_ERROR)

    prisonVisitBookerRegistryMockServer.stubGetBookersPrisonerVisitors(
      BOOKER_REFERENCE,
      PRISONER_ID,
      listOf(
        BookerPrisonerVisitorsDto(PRISONER_ID, adultVisitor.personId),
        BookerPrisonerVisitorsDto(PRISONER_ID, childVisitor.personId),
      ),
    )

    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, contactsList)

    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)

    // When
    val responseSpec = callGetVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE, PRISONER_ID)

    // Then
    responseSpec.expectStatus().is5xxServerError

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPrisonersForBooker(BOOKER_REFERENCE)
    verify(prisonVisitBookerRegistryClientSpy, times(0)).getVisitorsForBookersAssociatedPrisoner(BOOKER_REFERENCE, PRISONER_ID)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerById(PRISONER_ID)
    verify(prisonerContactRegistryClientSpy, times(0)).getPrisonersSocialContacts(PRISONER_ID, false)
    verify(visitSchedulerClientSpy, times(0)).getPrison(PRISON_CODE)
  }

  @Test
  fun `when get visitors by prisoner called without correct role then access forbidden is returned`() {
    // When
    val invalidRoleHttpHeaders = setAuthorisation(roles = listOf("ROLE_INVALID"))
    val responseSpec = callGetVisitorsByBookersPrisoner(webTestClient, invalidRoleHttpHeaders, BOOKER_REFERENCE, PRISONER_ID)

    // Then
    responseSpec.expectStatus().isForbidden

    // And

    verify(prisonVisitBookerRegistryClientSpy, times(0)).getPrisonersForBooker(BOOKER_REFERENCE)
    verify(prisonVisitBookerRegistryClientSpy, times(0)).getVisitorsForBookersAssociatedPrisoner(BOOKER_REFERENCE, PRISONER_ID)
    verify(prisonerSearchClientSpy, times(0)).getPrisonerById(PRISONER_ID)
    verify(prisonerContactRegistryClientSpy, times(0)).getPrisonersSocialContacts(PRISONER_ID, false)
    verify(visitSchedulerClientSpy, times(0)).getPrison(PRISON_CODE)
  }

  @Test
  fun `when get visitors by prisoner called without token then unauthorised status is returned`() {
    // When
    val responseSpec = webTestClient.get().uri("/public/booker/$BOOKER_REFERENCE/prisoners/$PRISONER_ID/visitors").exchange()

    // Then
    responseSpec.expectStatus().isUnauthorized

    // And
    verify(prisonVisitBookerRegistryClientSpy, times(0)).getPrisonersForBooker(BOOKER_REFERENCE)
    verify(prisonVisitBookerRegistryClientSpy, times(0)).getVisitorsForBookersAssociatedPrisoner(BOOKER_REFERENCE, PRISONER_ID)
    verify(prisonerSearchClientSpy, times(0)).getPrisonerById(PRISONER_ID)
    verify(prisonerContactRegistryClientSpy, times(0)).getPrisonersSocialContacts(PRISONER_ID, false)
    verify(visitSchedulerClientSpy, times(0)).getPrison(PRISON_CODE)
  }

  private fun assertVisitorContactBasicDetails(visitorBasicInfo: VisitorInfoDto, visitorDetails: VisitorDetails) {
    Assertions.assertThat(visitorBasicInfo.personId).isEqualTo(visitorDetails.personId)
    Assertions.assertThat(visitorBasicInfo.firstName).isEqualTo(visitorDetails.firstName)
    Assertions.assertThat(visitorBasicInfo.lastName).isEqualTo(visitorDetails.lastName)
    Assertions.assertThat(visitorBasicInfo.dateOfBirth).isEqualTo(visitorDetails.dateOfBirth)
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

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): List<VisitorInfoDto> {
    return objectMapper.readValue(returnResult.returnResult().responseBody, Array<VisitorInfoDto>::class.java).toList()
  }
}
