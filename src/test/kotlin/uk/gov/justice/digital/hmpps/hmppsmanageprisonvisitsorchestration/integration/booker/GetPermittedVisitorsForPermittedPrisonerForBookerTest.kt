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
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller.PUBLIC_BOOKER_GET_VISITORS_CONTROLLER_PATH
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.PermittedPrisonerForBookerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.PermittedVisitorsForPermittedPrisonerBookerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.RestrictionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.VisitorInfoDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.VisitorRestrictionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.VisitorRestrictionType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.PrisonUserClientDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import java.time.LocalDate

@DisplayName("Get permitted visitors for permitted prisoner for booker")
class GetPermittedVisitorsForPermittedPrisonerForBookerTest : IntegrationTestBase() {
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
    private val CLOSED_RESTRICTION = RestrictionDto(restrictionType = "CLOSED", restrictionTypeDescription = "CLOSED", startDate = LocalDate.now().minusDays(31), expiryDate = null, globalRestriction = true)

    private const val PRISON_CODE = "HEI"
    private const val BOOKER_REFERENCE = "booker-1"
    private const val PRISONER_ID = "AA112233B"
  }

  val bookerRegistryPrisonerDto = PermittedPrisonerForBookerDto(PRISONER_ID, true, listOf())

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

  @Test
  fun `when booker's prisoners has valid visitors then all allowed visitors are returned`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = true, null, true, contactsList)
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(BOOKER_REFERENCE, listOf(bookerRegistryPrisonerDto))
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisonerVisitors(
      BOOKER_REFERENCE,
      PRISONER_ID,
      listOf(
        PermittedVisitorsForPermittedPrisonerBookerDto(adultVisitor.personId, true),
        PermittedVisitorsForPermittedPrisonerBookerDto(childVisitor.personId, true),
      ),
    )

    // When
    val responseSpec = callGetVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE, PRISONER_ID)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(2)
    assertVisitorContactBasicDetails(prisonerDetailsList[0], adultVisitor)
    Assertions.assertThat(prisonerDetailsList[0].visitorRestrictions).isEmpty()
    assertVisitorContactBasicDetails(prisonerDetailsList[1], childVisitor)
    Assertions.assertThat(prisonerDetailsList[0].visitorRestrictions).isEmpty()

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForPermittedPrisonerAndBooker(BOOKER_REFERENCE)
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForBookersAssociatedPrisoner(BOOKER_REFERENCE, PRISONER_ID)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerById(PRISONER_ID)
    verify(visitSchedulerClientSpy, times(1)).getPrison(PRISON_CODE)
    verify(prisonerContactRegistryClientSpy, times(1)).getPrisonersSocialContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = true, null, true)
  }

  @Test
  fun `when booker's prisoners has banned and unbanned visitors then visitors are returned with restriction list populated`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = true, null, true, contactsList)
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(BOOKER_REFERENCE, listOf(bookerRegistryPrisonerDto))
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisonerVisitors(
      BOOKER_REFERENCE,
      PRISONER_ID,
      listOf(
        PermittedVisitorsForPermittedPrisonerBookerDto(adultVisitor.personId, true),
        PermittedVisitorsForPermittedPrisonerBookerDto(childVisitor.personId, true),
        PermittedVisitorsForPermittedPrisonerBookerDto(indefinitelyBannedVisitor.personId, true),
        PermittedVisitorsForPermittedPrisonerBookerDto(bannedVisitorForNext3Weeks.personId, true),
        PermittedVisitorsForPermittedPrisonerBookerDto(bannedVisitorForNext6Weeks.personId, true),
        PermittedVisitorsForPermittedPrisonerBookerDto(multipleBansVisitor.personId, true),
        PermittedVisitorsForPermittedPrisonerBookerDto(expiredBanVisitor.personId, true),
      ),
    )

    // When
    val responseSpec = callGetVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE, PRISONER_ID)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(7)
    assertVisitorContactBasicDetails(prisonerDetailsList[0], adultVisitor)
    Assertions.assertThat(prisonerDetailsList[0].visitorRestrictions).isEmpty()
    assertVisitorContactBasicDetails(prisonerDetailsList[1], childVisitor)
    Assertions.assertThat(prisonerDetailsList[1].visitorRestrictions).isEmpty()
    assertVisitorContactBasicDetails(prisonerDetailsList[2], indefinitelyBannedVisitor)
    Assertions.assertThat(prisonerDetailsList[2].visitorRestrictions.size).isEqualTo(1)
    assertVisitorRestriction(prisonerDetailsList[2].visitorRestrictions.toList()[0], VisitorRestrictionType.BAN, null)
    assertVisitorContactBasicDetails(prisonerDetailsList[3], bannedVisitorForNext3Weeks)
    Assertions.assertThat(prisonerDetailsList[3].visitorRestrictions.size).isEqualTo(1)
    assertVisitorRestriction(prisonerDetailsList[3].visitorRestrictions.toList()[0], VisitorRestrictionType.BAN, EXPIRING_IN_3_WEEKS_BANNED_RESTRICTION.expiryDate)
    assertVisitorContactBasicDetails(prisonerDetailsList[4], bannedVisitorForNext6Weeks)
    Assertions.assertThat(prisonerDetailsList[4].visitorRestrictions.size).isEqualTo(1)
    assertVisitorRestriction(prisonerDetailsList[4].visitorRestrictions.toList()[0], VisitorRestrictionType.BAN, EXPIRING_IN_6_WEEKS_BANNED_RESTRICTION.expiryDate)
    assertVisitorContactBasicDetails(prisonerDetailsList[5], multipleBansVisitor)
    Assertions.assertThat(prisonerDetailsList[5].visitorRestrictions.size).isEqualTo(1)
    assertVisitorRestriction(prisonerDetailsList[5].visitorRestrictions.toList()[0], VisitorRestrictionType.BAN, EXPIRING_IN_6_WEEKS_BANNED_RESTRICTION.expiryDate)
    assertVisitorContactBasicDetails(prisonerDetailsList[6], expiredBanVisitor)
    Assertions.assertThat(prisonerDetailsList[6].visitorRestrictions).isEmpty()

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForPermittedPrisonerAndBooker(BOOKER_REFERENCE)
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForBookersAssociatedPrisoner(BOOKER_REFERENCE, PRISONER_ID)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerById(PRISONER_ID)
    verify(visitSchedulerClientSpy, times(1)).getPrison(PRISON_CODE)
    verify(prisonerContactRegistryClientSpy, times(1)).getPrisonersSocialContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = true, null, true)
  }

  @Test
  fun `when booker's prisoners has visitor with multiple bans with 1 indefinite ban then restriction expiry date is null`() {
    // Given
    val visitorWithIndefiniteBan = createVisitor(
      firstName = "Fifth",
      lastName = "VisitorE",
      dateOfBirth = LocalDate.of(1990, 4, 1),
      restrictions = listOf(INDEFINITELY_BANNED_RESTRICTION, EXPIRING_IN_3_WEEKS_BANNED_RESTRICTION),
    )

    val contacts = createContactsList(listOf(visitorWithIndefiniteBan))

    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = true, null, true, contacts)
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(BOOKER_REFERENCE, listOf(bookerRegistryPrisonerDto))
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisonerVisitors(
      BOOKER_REFERENCE,
      PRISONER_ID,
      listOf(
        PermittedVisitorsForPermittedPrisonerBookerDto(visitorWithIndefiniteBan.personId, true),
      ),
    )

    // When
    val responseSpec = callGetVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE, PRISONER_ID)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(1)
    assertVisitorContactBasicDetails(prisonerDetailsList[0], visitorWithIndefiniteBan)
    assertVisitorRestriction(prisonerDetailsList[0].visitorRestrictions.toList()[0], VisitorRestrictionType.BAN, null)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForPermittedPrisonerAndBooker(BOOKER_REFERENCE)
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForBookersAssociatedPrisoner(BOOKER_REFERENCE, PRISONER_ID)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerById(PRISONER_ID)
    verify(visitSchedulerClientSpy, times(1)).getPrison(PRISON_CODE)
    verify(prisonerContactRegistryClientSpy, times(1)).getPrisonersSocialContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = true, null, true)
  }

  @Test
  fun `when booker's prisoners has visitor with multiple bans with different end date then restriction expiry date is the farthest date`() {
    // Given
    val visitorWithBan = createVisitor(
      firstName = "Fifth",
      lastName = "VisitorE",
      dateOfBirth = LocalDate.of(1990, 4, 1),
      restrictions = listOf(EXPIRING_IN_6_WEEKS_BANNED_RESTRICTION, EXPIRING_IN_3_WEEKS_BANNED_RESTRICTION),
    )

    val contacts = createContactsList(listOf(visitorWithBan))

    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = true, null, true, contacts)
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(BOOKER_REFERENCE, listOf(bookerRegistryPrisonerDto))
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisonerVisitors(
      BOOKER_REFERENCE,
      PRISONER_ID,
      listOf(
        PermittedVisitorsForPermittedPrisonerBookerDto(visitorWithBan.personId, true),
      ),
    )

    // When
    val responseSpec = callGetVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE, PRISONER_ID)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(1)
    assertVisitorContactBasicDetails(prisonerDetailsList[0], visitorWithBan)
    assertVisitorRestriction(prisonerDetailsList[0].visitorRestrictions.toList()[0], VisitorRestrictionType.BAN, EXPIRING_IN_6_WEEKS_BANNED_RESTRICTION.expiryDate)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForPermittedPrisonerAndBooker(BOOKER_REFERENCE)
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForBookersAssociatedPrisoner(BOOKER_REFERENCE, PRISONER_ID)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerById(PRISONER_ID)
    verify(visitSchedulerClientSpy, times(1)).getPrison(PRISON_CODE)
    verify(prisonerContactRegistryClientSpy, times(1)).getPrisonersSocialContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = true, null, true)
  }

  @Test
  fun `when booker's prisoners has visitor with multiple restrictions only bans are returned`() {
    // Given
    val visitorWithBan = createVisitor(
      firstName = "Fifth",
      lastName = "VisitorE",
      dateOfBirth = LocalDate.of(1990, 4, 1),
      restrictions = listOf(EXPIRING_IN_3_WEEKS_BANNED_RESTRICTION, CLOSED_RESTRICTION),
    )

    val contacts = createContactsList(listOf(visitorWithBan))

    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = true, null, true, contacts)
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(BOOKER_REFERENCE, listOf(bookerRegistryPrisonerDto))
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisonerVisitors(
      BOOKER_REFERENCE,
      PRISONER_ID,
      listOf(
        PermittedVisitorsForPermittedPrisonerBookerDto(visitorWithBan.personId, true),
      ),
    )

    // When
    val responseSpec = callGetVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE, PRISONER_ID)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(1)
    assertVisitorContactBasicDetails(prisonerDetailsList[0], visitorWithBan)
    assertVisitorRestriction(prisonerDetailsList[0].visitorRestrictions.toList()[0], VisitorRestrictionType.BAN, EXPIRING_IN_3_WEEKS_BANNED_RESTRICTION.expiryDate)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForPermittedPrisonerAndBooker(BOOKER_REFERENCE)
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForBookersAssociatedPrisoner(BOOKER_REFERENCE, PRISONER_ID)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerById(PRISONER_ID)
    verify(visitSchedulerClientSpy, times(1)).getPrison(PRISON_CODE)
    verify(prisonerContactRegistryClientSpy, times(1)).getPrisonersSocialContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = true, null, true)
  }

  @Test
  fun `when booker's prisoners has no valid visitors then no visitors are returned`() {
    // Given
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = true, null, true, contactsList)
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

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForPermittedPrisonerAndBooker(BOOKER_REFERENCE)
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForBookersAssociatedPrisoner(BOOKER_REFERENCE, PRISONER_ID)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerById(PRISONER_ID)
    verify(visitSchedulerClientSpy, times(1)).getPrison(PRISON_CODE)
    verify(prisonerContactRegistryClientSpy, times(0)).getPrisonersSocialContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = true)
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
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = true, null, true, contactsList)

    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)

    // When
    val responseSpec = callGetVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE, PRISONER_ID)

    // Then
    responseSpec.expectStatus().isNotFound
    assertErrorResult(responseSpec, HttpStatus.NOT_FOUND, "Prisoners for booker reference - $BOOKER_REFERENCE not found on public-visits-booker-registry")

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForPermittedPrisonerAndBooker(BOOKER_REFERENCE)
    verify(prisonVisitBookerRegistryClientSpy, times(0)).getPermittedVisitorsForBookersAssociatedPrisoner(BOOKER_REFERENCE, PRISONER_ID)
    verify(prisonerSearchClientSpy, times(0)).getPrisonerById(PRISONER_ID)
    verify(visitSchedulerClientSpy, times(0)).getPrison(PRISON_CODE)
    verify(prisonerContactRegistryClientSpy, times(0)).getPrisonersSocialContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = true)
  }

  @Test
  fun `when prisoner is in a prison not on VSIP then NOT_FOUND status is sent back`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(BOOKER_REFERENCE, listOf(bookerRegistryPrisonerDto))
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisonerVisitors(
      BOOKER_REFERENCE,
      PRISONER_ID,
      listOf(
        PermittedVisitorsForPermittedPrisonerBookerDto(adultVisitor.personId, true),
        PermittedVisitorsForPermittedPrisonerBookerDto(expiredBanVisitor.personId, true),
      ),
    )
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = true, null, true, contactsList)
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, null)

    // When
    val responseSpec = callGetVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE, PRISONER_ID)

    // Then
    responseSpec.expectStatus().isNotFound
    assertErrorResult(responseSpec, HttpStatus.NOT_FOUND, "Prison with prison code - $PRISON_CODE not found on visit-scheduler")
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForPermittedPrisonerAndBooker(BOOKER_REFERENCE)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerById(PRISONER_ID)
    verify(prisonVisitBookerRegistryClientSpy, times(0)).getPermittedVisitorsForBookersAssociatedPrisoner(BOOKER_REFERENCE, PRISONER_ID)
    verify(visitSchedulerClientSpy, times(1)).getPrison(PRISON_CODE)
    verify(prisonerContactRegistryClientSpy, times(0)).getPrisonersSocialContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = true)
  }

  @Test
  fun `when prisoner is in a prison not active on VSIP then BAD_REQUEST status is sent back`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(BOOKER_REFERENCE, listOf(bookerRegistryPrisonerDto))
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisonerVisitors(
      BOOKER_REFERENCE,
      PRISONER_ID,
      listOf(
        PermittedVisitorsForPermittedPrisonerBookerDto(adultVisitor.personId, true),
        PermittedVisitorsForPermittedPrisonerBookerDto(expiredBanVisitor.personId, true),
      ),
    )
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = true, null, true, contactsList)
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, createPrison(PRISON_CODE, active = false))

    // When
    val responseSpec = callGetVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE, PRISONER_ID)

    // Then
    responseSpec.expectStatus().isBadRequest
    assertErrorResult(responseSpec, HttpStatus.BAD_REQUEST, "prison validation for prison code - $PRISON_CODE for prisoner number - $PRISONER_ID failed with error - Prison with code - $PRISON_CODE, is not active on visit-scheduler")
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForPermittedPrisonerAndBooker(BOOKER_REFERENCE)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerById(PRISONER_ID)
    verify(prisonVisitBookerRegistryClientSpy, times(0)).getPermittedVisitorsForBookersAssociatedPrisoner(BOOKER_REFERENCE, PRISONER_ID)
    verify(visitSchedulerClientSpy, times(1)).getPrison(PRISON_CODE)
    verify(prisonerContactRegistryClientSpy, times(0)).getPrisonersSocialContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = true)
  }

  @Test
  fun `when prisoner is in a prison only active for staff UI on VSIP then BAD_REQUEST status is sent back`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(BOOKER_REFERENCE, listOf(bookerRegistryPrisonerDto))
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisonerVisitors(
      BOOKER_REFERENCE,
      PRISONER_ID,
      listOf(
        PermittedVisitorsForPermittedPrisonerBookerDto(adultVisitor.personId, true),
        PermittedVisitorsForPermittedPrisonerBookerDto(expiredBanVisitor.personId, true),
      ),
    )
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = true, null, true, contactsList)
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
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForPermittedPrisonerAndBooker(BOOKER_REFERENCE)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerById(PRISONER_ID)
    verify(prisonVisitBookerRegistryClientSpy, times(0)).getPermittedVisitorsForBookersAssociatedPrisoner(BOOKER_REFERENCE, PRISONER_ID)
    verify(visitSchedulerClientSpy, times(1)).getPrison(PRISON_CODE)
    verify(prisonerContactRegistryClientSpy, times(0)).getPrisonersSocialContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = true)
  }

  @Test
  fun `when prisoner is in a prison not active for staff UI on VSIP then BAD_REQUEST status is sent back`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisoners(BOOKER_REFERENCE, listOf(bookerRegistryPrisonerDto))
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisonerVisitors(
      BOOKER_REFERENCE,
      PRISONER_ID,
      listOf(
        PermittedVisitorsForPermittedPrisonerBookerDto(adultVisitor.personId, true),
        PermittedVisitorsForPermittedPrisonerBookerDto(expiredBanVisitor.personId, true),
      ),
    )
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = true, null, true, contactsList)
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
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForPermittedPrisonerAndBooker(BOOKER_REFERENCE)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerById(PRISONER_ID)
    verify(prisonVisitBookerRegistryClientSpy, times(0)).getPermittedVisitorsForBookersAssociatedPrisoner(BOOKER_REFERENCE, PRISONER_ID)
    verify(visitSchedulerClientSpy, times(1)).getPrison(PRISON_CODE)
    verify(prisonerContactRegistryClientSpy, times(0)).getPrisonersSocialContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = true)
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

    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = true, null, true, contactsList)
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)

    // When
    val responseSpec = callGetVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE, PRISONER_ID)

    // Then
    responseSpec.expectStatus().isNotFound
    assertErrorResult(responseSpec, HttpStatus.NOT_FOUND, "Visitors for booker reference - booker-1 and prisoner id - AA112233B not found on public-visits-booker-registry")

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForPermittedPrisonerAndBooker(BOOKER_REFERENCE)
    verify(visitSchedulerClientSpy, times(1)).getPrison(PRISON_CODE)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerById(PRISONER_ID)
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForBookersAssociatedPrisoner(BOOKER_REFERENCE, PRISONER_ID)
    verify(prisonerContactRegistryClientSpy, times(0)).getPrisonersSocialContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = true)
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

    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = true, null, true, contactsList)
    prisonOffenderSearchMockServer.stubGetPrisonerById(PRISONER_ID, prisonerDto)
    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)

    // When
    val responseSpec = callGetVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE, PRISONER_ID)

    // Then
    responseSpec.expectStatus().is5xxServerError

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForPermittedPrisonerAndBooker(BOOKER_REFERENCE)
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForBookersAssociatedPrisoner(BOOKER_REFERENCE, PRISONER_ID)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerById(PRISONER_ID)
    verify(visitSchedulerClientSpy, times(1)).getPrison(PRISON_CODE)
    verify(prisonerContactRegistryClientSpy, times(0)).getPrisonersSocialContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = true)
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
        PermittedVisitorsForPermittedPrisonerBookerDto(adultVisitor.personId, true),
        PermittedVisitorsForPermittedPrisonerBookerDto(childVisitor.personId, true),
      ),
    )

    // prisoner contact registry returns 404
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = true, null, true, null, HttpStatus.NOT_FOUND)

    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)

    // When
    val responseSpec = callGetVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE, PRISONER_ID)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(0)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForPermittedPrisonerAndBooker(BOOKER_REFERENCE)
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForBookersAssociatedPrisoner(BOOKER_REFERENCE, PRISONER_ID)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerById(PRISONER_ID)
    verify(visitSchedulerClientSpy, times(1)).getPrison(PRISON_CODE)
    verify(prisonerContactRegistryClientSpy, times(1)).getPrisonersSocialContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = true, null, true)
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
        PermittedVisitorsForPermittedPrisonerBookerDto(adultVisitor.personId, true),
        PermittedVisitorsForPermittedPrisonerBookerDto(childVisitor.personId, true),
      ),
    )

    // prisoner contact registry returns INTERNAL_SERVER_ERROR
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = true, null, true, null, HttpStatus.INTERNAL_SERVER_ERROR)

    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)

    // When
    val responseSpec = callGetVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE, PRISONER_ID)

    // Then
    responseSpec.expectStatus().is5xxServerError

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForPermittedPrisonerAndBooker(BOOKER_REFERENCE)
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForBookersAssociatedPrisoner(BOOKER_REFERENCE, PRISONER_ID)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerById(PRISONER_ID)
    verify(prisonerContactRegistryClientSpy, times(1)).getPrisonersSocialContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = true, null, true)
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
        PermittedVisitorsForPermittedPrisonerBookerDto(adultVisitor.personId, true),
        PermittedVisitorsForPermittedPrisonerBookerDto(childVisitor.personId, true),
      ),
    )

    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = true, null, true, contactsList)

    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)

    // When
    val responseSpec = callGetVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE, PRISONER_ID)

    // Then
    responseSpec.expectStatus().isNotFound
    assertErrorResult(responseSpec, HttpStatus.NOT_FOUND, "Prisoner with id - AA112233B not found on prisoner search")

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForPermittedPrisonerAndBooker(BOOKER_REFERENCE)
    verify(prisonVisitBookerRegistryClientSpy, times(0)).getPermittedVisitorsForBookersAssociatedPrisoner(BOOKER_REFERENCE, PRISONER_ID)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerById(PRISONER_ID)
    verify(prisonerContactRegistryClientSpy, times(0)).getPrisonersSocialContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = true)
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
        PermittedVisitorsForPermittedPrisonerBookerDto(adultVisitor.personId, true),
        PermittedVisitorsForPermittedPrisonerBookerDto(childVisitor.personId, true),
      ),
    )

    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = true, null, true, contactsList)

    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)

    // When
    val responseSpec = callGetVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE, PRISONER_ID)

    // Then
    responseSpec.expectStatus().isBadRequest
    assertErrorResult(responseSpec, HttpStatus.BAD_REQUEST, "prisoner validation for prisoner number - $PRISONER_ID failed with error - Prisoner - $PRISONER_ID on prisoner search does not have a valid prison")

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForPermittedPrisonerAndBooker(BOOKER_REFERENCE)
    verify(prisonVisitBookerRegistryClientSpy, times(0)).getPermittedVisitorsForBookersAssociatedPrisoner(BOOKER_REFERENCE, PRISONER_ID)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerById(PRISONER_ID)
    verify(prisonerContactRegistryClientSpy, times(0)).getPrisonersSocialContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = true)
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
        PermittedVisitorsForPermittedPrisonerBookerDto(adultVisitor.personId, true),
        PermittedVisitorsForPermittedPrisonerBookerDto(childVisitor.personId, true),
      ),
    )

    prisonerContactRegistryMockServer.stubGetPrisonerContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = true, null, true, contactsList)

    visitSchedulerMockServer.stubGetPrison(PRISON_CODE, prisonDto)

    // When
    val responseSpec = callGetVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, BOOKER_REFERENCE, PRISONER_ID)

    // Then
    responseSpec.expectStatus().is5xxServerError

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getPermittedVisitorsForPermittedPrisonerAndBooker(BOOKER_REFERENCE)
    verify(prisonVisitBookerRegistryClientSpy, times(0)).getPermittedVisitorsForBookersAssociatedPrisoner(BOOKER_REFERENCE, PRISONER_ID)
    verify(prisonerSearchClientSpy, times(1)).getPrisonerById(PRISONER_ID)
    verify(prisonerContactRegistryClientSpy, times(0)).getPrisonersSocialContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = true)
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

    verify(prisonVisitBookerRegistryClientSpy, times(0)).getPermittedVisitorsForPermittedPrisonerAndBooker(BOOKER_REFERENCE)
    verify(prisonVisitBookerRegistryClientSpy, times(0)).getPermittedVisitorsForBookersAssociatedPrisoner(BOOKER_REFERENCE, PRISONER_ID)
    verify(prisonerSearchClientSpy, times(0)).getPrisonerById(PRISONER_ID)
    verify(prisonerContactRegistryClientSpy, times(0)).getPrisonersSocialContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = true)
    verify(visitSchedulerClientSpy, times(0)).getPrison(PRISON_CODE)
  }

  @Test
  fun `when get visitors by prisoner called without token then unauthorised status is returned`() {
    // When
    val responseSpec = webTestClient.get().uri("/public/booker/$BOOKER_REFERENCE/prisoners/$PRISONER_ID/visitors").exchange()

    // Then
    responseSpec.expectStatus().isUnauthorized

    // And
    verify(prisonVisitBookerRegistryClientSpy, times(0)).getPermittedVisitorsForPermittedPrisonerAndBooker(BOOKER_REFERENCE)
    verify(prisonVisitBookerRegistryClientSpy, times(0)).getPermittedVisitorsForBookersAssociatedPrisoner(BOOKER_REFERENCE, PRISONER_ID)
    verify(prisonerSearchClientSpy, times(0)).getPrisonerById(PRISONER_ID)
    verify(prisonerContactRegistryClientSpy, times(0)).getPrisonersSocialContacts(PRISONER_ID, withAddress = false, approvedVisitorsOnly = true, null, true)
    verify(visitSchedulerClientSpy, times(0)).getPrison(PRISON_CODE)
  }

  private fun assertVisitorContactBasicDetails(visitorBasicInfo: VisitorInfoDto, visitorDetails: VisitorDetails) {
    Assertions.assertThat(visitorBasicInfo.visitorId).isEqualTo(visitorDetails.personId)
    Assertions.assertThat(visitorBasicInfo.firstName).isEqualTo(visitorDetails.firstName)
    Assertions.assertThat(visitorBasicInfo.lastName).isEqualTo(visitorDetails.lastName)
    Assertions.assertThat(visitorBasicInfo.dateOfBirth).isEqualTo(visitorDetails.dateOfBirth)
  }

  private fun assertVisitorRestriction(visitorRestriction: VisitorRestrictionDto, restrictionType: VisitorRestrictionType, restrictionExpiryDate: LocalDate?) {
    Assertions.assertThat(visitorRestriction.restrictionType).isEqualTo(restrictionType)
    Assertions.assertThat(visitorRestriction.expiryDate).isEqualTo(restrictionExpiryDate)
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

  fun callGetVisitorsByBookersPrisoner(
    webTestClient: WebTestClient,
    authHttpHeaders: (HttpHeaders) -> Unit,
    bookerReference: String,
    prisonerId: String,
  ): WebTestClient.ResponseSpec {
    return webTestClient.get().uri(PUBLIC_BOOKER_GET_VISITORS_CONTROLLER_PATH.replace("{bookerReference}", bookerReference).replace("{prisonerId}", prisonerId))
      .headers(authHttpHeaders)
      .exchange()
  }
}
