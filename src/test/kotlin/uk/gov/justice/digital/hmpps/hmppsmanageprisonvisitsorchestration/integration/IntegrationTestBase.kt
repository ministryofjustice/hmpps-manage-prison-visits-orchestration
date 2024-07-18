package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.shaded.org.apache.commons.lang3.RandomUtils
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller.ORCHESTRATION_GET_CANCELLED_PUBLIC_VISITS_BY_BOOKER_REFERENCE
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller.ORCHESTRATION_GET_FUTURE_BOOKED_PUBLIC_VISITS_BY_BOOKER_REFERENCE
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller.ORCHESTRATION_GET_PAST_BOOKED_PUBLIC_VISITS_BY_BOOKER_REFERENCE
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.PrisonerContactDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.RestrictionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.OrchestrationVisitorDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.register.PrisonNameDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.CurrentIncentive
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.ContactDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.PrisonUserClientDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSchedulerPrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSessionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitorDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.application.ApplicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.application.ChangeApplicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.application.CreateApplicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.OutcomeStatus
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionRestriction
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType.PUBLIC
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType.STAFF
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitRestriction
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitStatus
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.whereabouts.ScheduledEventDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.helper.JwtAuthHelper
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.HmppsAuthExtension
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.ManageUsersApiMockServer
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.PrisonApiMockServer
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.PrisonOffenderSearchMockServer
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.PrisonRegisterMockServer
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.PrisonVisitBookerRegistryMockServer
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.PrisonerContactRegistryMockServer
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.VisitSchedulerMockServer
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.WhereaboutsApiMockServer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.stream.Collectors

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(HmppsAuthExtension::class)
abstract class IntegrationTestBase {
  companion object {
    val visitSchedulerMockServer = VisitSchedulerMockServer()
    val prisonApiMockServer = PrisonApiMockServer()
    val prisonOffenderSearchMockServer = PrisonOffenderSearchMockServer()
    val prisonerContactRegistryMockServer = PrisonerContactRegistryMockServer()
    val prisonRegisterMockServer = PrisonRegisterMockServer()
    val manageUsersApiMockServer = ManageUsersApiMockServer()
    val prisonVisitBookerRegistryMockServer = PrisonVisitBookerRegistryMockServer()
    val whereaboutsApiMockServer = WhereaboutsApiMockServer()

    @BeforeAll
    @JvmStatic
    fun startMocks() {
      visitSchedulerMockServer.start()
      prisonApiMockServer.start()
      prisonOffenderSearchMockServer.start()
      prisonerContactRegistryMockServer.start()
      prisonRegisterMockServer.start()
      manageUsersApiMockServer.start()
      prisonVisitBookerRegistryMockServer.start()
      prisonVisitBookerRegistryMockServer.start()
      whereaboutsApiMockServer.start()
    }

    @AfterAll
    @JvmStatic
    fun stopMocks() {
      visitSchedulerMockServer.stop()
      prisonApiMockServer.stop()
      prisonOffenderSearchMockServer.stop()
      prisonerContactRegistryMockServer.stop()
      prisonRegisterMockServer.stop()
      manageUsersApiMockServer.stop()
      prisonVisitBookerRegistryMockServer.stop()
      prisonVisitBookerRegistryMockServer.start()
      whereaboutsApiMockServer.stop()
    }

    fun getVisitsQueryParams(
      prisonCode: String?,
      prisonerId: String,
      visitStatus: List<String>,
      startDateTime: LocalDate? = null,
      endDateTime: LocalDate? = null,
      page: Int,
      size: Int,
    ): List<String> {
      val queryParams = ArrayList<String>()
      prisonCode?.let {
        queryParams.add("prisonId=$prisonCode")
      }
      queryParams.add("prisonerId=$prisonerId")
      visitStatus.forEach {
        queryParams.add("visitStatus=$it")
      }
      startDateTime?.let {
        queryParams.add("visitStartDate=$it")
      }
      endDateTime?.let {
        queryParams.add("visitEndDate=$it")
      }
      queryParams.add("page=$page")
      queryParams.add("size=$size")
      return queryParams
    }
  }

  @Autowired
  lateinit var webTestClient: WebTestClient

  lateinit var roleVSIPOrchestrationServiceHttpHeaders: (HttpHeaders) -> Unit

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthHelper

  @Autowired
  protected lateinit var objectMapper: ObjectMapper

  @BeforeEach
  internal fun setUp() {
    roleVSIPOrchestrationServiceHttpHeaders = setAuthorisation(roles = listOf("ROLE_VSIP_ORCHESTRATION_SERVICE"))
  }

  internal fun setAuthorisation(
    user: String = "AUTH_ADM",
    roles: List<String> = listOf(),
    scopes: List<String> = listOf(),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisation(user, roles, scopes)

  fun callGetVisits(
    webTestClient: WebTestClient,
    prisonerId: String,
    visitStatus: List<String>,
    startDateTime: LocalDateTime? = null,
    endDateTime: LocalDateTime? = null,
    page: Int,
    size: Int,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    return webTestClient.get()
      .uri("/visits/search?${getVisitsQueryParams(prisonerId, visitStatus, startDateTime, endDateTime, page, size).joinToString("&")}")
      .headers(authHttpHeaders)
      .exchange()
  }

  private fun getVisitsQueryParams(
    prisonerId: String,
    visitStatus: List<String>,
    startDateTime: LocalDateTime? = null,
    endDateTime: LocalDateTime? = null,
    page: Int,
    size: Int,
  ): List<String> {
    val queryParams = ArrayList<String>()
    queryParams.add("prisonerId=$prisonerId")
    visitStatus.forEach {
      queryParams.add("visitStatus=$it")
    }
    startDateTime?.let {
      queryParams.add("startDateTime=$it")
    }
    endDateTime?.let {
      queryParams.add("endDateTime=$it")
    }
    queryParams.add("page=$page")
    queryParams.add("size=$size")
    return queryParams
  }

  final fun createVisitDto(
    reference: String = "aa-bb-cc-dd",
    applicationReference: String = "aaa-bbb-ccc-ddd",
    prisonerId: String = "AB12345DS",
    prisonCode: String = "MDI",
    visitRoom: String = "A1 L3",
    visitType: VisitType = VisitType.SOCIAL,
    visitStatus: VisitStatus = VisitStatus.BOOKED,
    visitRestriction: VisitRestriction = VisitRestriction.OPEN,
    startTimestamp: LocalDateTime = LocalDateTime.now(),
    endTimestamp: LocalDateTime = startTimestamp.plusHours(1),
    outcomeStatus: OutcomeStatus? = null,
    createdTimestamp: LocalDateTime = LocalDateTime.now(),
    modifiedTimestamp: LocalDateTime = LocalDateTime.now(),
    sessionTemplateReference: String? = "ref.ref.ref",
    visitors: List<VisitorDto>? = null,
    contact: ContactDto = ContactDto("Jane Doe", "01234567890"),
  ): VisitDto {
    return VisitDto(
      applicationReference = applicationReference,
      sessionTemplateReference = sessionTemplateReference,
      reference = reference,
      prisonerId = prisonerId,
      prisonCode = prisonCode,
      visitRoom = visitRoom,
      visitType = visitType,
      visitStatus = visitStatus,
      visitRestriction = visitRestriction,
      startTimestamp = startTimestamp,
      endTimestamp = endTimestamp,
      outcomeStatus = outcomeStatus,
      createdTimestamp = createdTimestamp,
      modifiedTimestamp = modifiedTimestamp,
      visitors = visitors,
      visitContact = contact,
    )
  }

  fun createCreateApplicationDto(prisonerId: String, sessionTemplateReference: String = "ref.ref.ref", sessionDate: LocalDate? = LocalDate.now()): CreateApplicationDto {
    val visitor = VisitorDto(1, false)
    return CreateApplicationDto(
      prisonerId = prisonerId,
      sessionTemplateReference = sessionTemplateReference,
      applicationRestriction = SessionRestriction.OPEN,
      sessionDate = sessionDate!!,
      visitContact = null,
      visitors = setOf(visitor),
      userType = STAFF,
      actionedBy = "Aled",
      allowOverBooking = true,
    )
  }

  final fun createApplicationDto(
    reference: String = "aa-bb-cc-dd",
    prisonerId: String = "AB12345DS",
    prisonCode: String = "MDI",
    visitType: VisitType = VisitType.SOCIAL,
    visitRestriction: VisitRestriction = VisitRestriction.OPEN,
    startTimestamp: LocalDateTime = LocalDateTime.now(),
    endTimestamp: LocalDateTime = startTimestamp.plusHours(1),
    createdTimestamp: LocalDateTime = LocalDateTime.now(),
    modifiedTimestamp: LocalDateTime = LocalDateTime.now(),
    sessionTemplateReference: String = "ref.ref.ref",
    visitors: List<VisitorDto>? = listOf(),
  ): ApplicationDto {
    return ApplicationDto(
      sessionTemplateReference = sessionTemplateReference,
      reference = reference,
      prisonerId = prisonerId,
      prisonCode = prisonCode,
      visitType = visitType,
      visitRestriction = visitRestriction,
      startTimestamp = startTimestamp,
      endTimestamp = endTimestamp,
      createdTimestamp = createdTimestamp,
      modifiedTimestamp = modifiedTimestamp,
      visitors = visitors!!,
      completed = false,
      reserved = false,
      userType = PUBLIC,
    )
  }

  fun createChangeApplicationDto(): ChangeApplicationDto {
    val visitor = VisitorDto(1, false)
    return ChangeApplicationDto(
      applicationRestriction = SessionRestriction.OPEN,
      sessionDate = LocalDate.now(),
      visitContact = null,
      visitors = setOf(visitor),
      sessionTemplateReference = "aa-bb-cc-dd",
      allowOverBooking = false,
    )
  }

  fun createVisitSessionDto(prisonCode: String, sessionTemplateReference: String): VisitSessionDto {
    return VisitSessionDto(
      sessionTemplateReference = sessionTemplateReference,
      prisonCode = prisonCode,
      visitRoom = "Visit Main Hall",
      visitType = VisitType.SOCIAL,
      closedVisitCapacity = 5,
      openVisitCapacity = 30,
      startTimestamp = LocalDateTime.now(),
      endTimestamp = LocalDateTime.now().plusHours(1),
    )
  }

  final fun createPrisonNameDto(prisonCode: String, name: String): PrisonNameDto {
    return PrisonNameDto(
      prisonId = prisonCode,
      prisonName = name,
    )
  }

  final fun createScheduledEvent(
    bookingId: Long,
    eventDate: LocalDate,
    eventType: String = "APP",
    eventTypeDesc: String = "Appointment",
    eventSubType: String,
    eventSubTypeDesc: String,
    eventStartTime: LocalDateTime? = null,
    eventEndTime: LocalDateTime? = null,
  ): ScheduledEventDto {
    return ScheduledEventDto(
      bookingId = bookingId,
      eventDate = eventDate,
      eventType = eventType,
      eventTypeDesc = eventTypeDesc,
      eventSubType = eventSubType,
      eventSubTypeDesc = eventSubTypeDesc,
      startTime = eventStartTime,
      endTime = eventEndTime,
    )
  }

  fun getOrchestrationVisitsBySessionTemplateQueryParams(
    sessionTemplateReference: String?,
    sessionDate: LocalDate,
    visitStatus: List<String>,
    visitRestrictions: List<VisitRestriction>?,
    prisonCode: String,
    page: Int,
    size: Int,
  ): List<String> {
    val queryParams = ArrayList<String>()
    sessionTemplateReference?.let {
      queryParams.add("sessionTemplateReference=$sessionTemplateReference")
    }
    queryParams.add("sessionDate=$sessionDate")
    visitStatus.forEach {
      queryParams.add("visitStatus=$it")
    }
    visitRestrictions?.let {
      visitRestrictions.forEach {
        queryParams.add("visitRestrictions=$it")
      }
    }
    queryParams.add("prisonCode=$prisonCode")
    queryParams.add("page=$page")
    queryParams.add("size=$size")
    return queryParams
  }

  fun callGetVisitsBySessionTemplate(
    webTestClient: WebTestClient,
    sessionTemplateReference: String,
    sessionDate: LocalDate,
    visitStatus: List<String>,
    visitRestriction: List<VisitRestriction>?,
    prisonCode: String,
    page: Int,
    size: Int,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    return webTestClient.get()
      .uri("/visits/session-template?${getOrchestrationVisitsBySessionTemplateQueryParams(sessionTemplateReference, sessionDate, visitStatus, visitRestriction, prisonCode, page, size).joinToString("&")}")
      .headers(authHttpHeaders)
      .exchange()
  }

  fun callFutureVisits(
    webTestClient: WebTestClient,
    prisonerId: String,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    return webTestClient.get()
      .uri("/visits/search/future/$prisonerId")
      .headers(authHttpHeaders)
      .exchange()
  }

  fun callPublicFutureVisits(
    webTestClient: WebTestClient,
    bookerReference: String,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    return webTestClient.get()
      .uri(ORCHESTRATION_GET_FUTURE_BOOKED_PUBLIC_VISITS_BY_BOOKER_REFERENCE.replace("{bookerReference}", bookerReference))
      .headers(authHttpHeaders)
      .exchange()
  }

  fun callPublicPastVisits(
    webTestClient: WebTestClient,
    bookerReference: String,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    return webTestClient.get()
      .uri(ORCHESTRATION_GET_PAST_BOOKED_PUBLIC_VISITS_BY_BOOKER_REFERENCE.replace("{bookerReference}", bookerReference))
      .headers(authHttpHeaders)
      .exchange()
  }

  fun callPublicCancelledVisits(
    webTestClient: WebTestClient,
    bookerReference: String,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    return webTestClient.get()
      .uri(ORCHESTRATION_GET_CANCELLED_PUBLIC_VISITS_BY_BOOKER_REFERENCE.replace("{bookerReference}", bookerReference))
      .headers(authHttpHeaders)
      .exchange()
  }

  fun callGetAvailableVisitSessions(
    webTestClient: WebTestClient,
    prisonCode: String,
    prisonerId: String,
    sessionRestriction: SessionRestriction,
    visitorIds: List<Long>? = null,
    withAppointmentsCheck: Boolean,
    authHttpHeaders: (HttpHeaders) -> Unit,
    excludedApplicationReference: String? = null,
    advanceFromDateByDays: Int? = null,
    currentUser: String? = null,
  ): WebTestClient.ResponseSpec {
    val uri = "/visit-sessions/available"

    val uriParams =
      getAvailableVisitSessionQueryParams(
        prisonCode = prisonCode,
        prisonerId = prisonerId,
        sessionRestriction = sessionRestriction,
        visitorIds = visitorIds,
        withAppointmentsCheck = withAppointmentsCheck,
        excludedApplicationReference = excludedApplicationReference,
        advanceFromDateByDays = advanceFromDateByDays,
      ).joinToString("&")

    return webTestClient.get().uri("$uri?$uriParams")
      .headers(authHttpHeaders)
      .exchange()
  }

  final fun createPrisoner(
    prisonerId: String,
    firstName: String,
    lastName: String,
    dateOfBirth: LocalDate,
    prisonId: String? = "MDI",
    prisonName: String = "HMP Leeds",
    cellLocation: String? = null,
    currentIncentive: CurrentIncentive? = null,
  ): PrisonerDto {
    return PrisonerDto(
      prisonerNumber = prisonerId,
      firstName = firstName,
      lastName = lastName,
      dateOfBirth = dateOfBirth,
      prisonId = prisonId,
      prisonName = prisonName,
      cellLocation = cellLocation,
      currentIncentive = currentIncentive,
    )
  }

  fun createContactDto(
    personId: Long = RandomUtils.nextLong(),
    firstName: String,
    lastName: String,
    dateOfBirth: LocalDate? = null,
    approvedVisitor: Boolean = true,
    restrictions: List<RestrictionDto> = emptyList(),
  ): PrisonerContactDto {
    return PrisonerContactDto(
      personId = personId,
      firstName = firstName,
      lastName = lastName,
      approvedVisitor = approvedVisitor,
      dateOfBirth = dateOfBirth,
      relationshipCode = "OTH",
      contactType = "S",
      emergencyContact = true,
      nextOfKin = true,
      restrictions = restrictions,
    )
  }

  final fun createContactsList(visitorDetails: List<VisitorDetails>): List<PrisonerContactDto> {
    return visitorDetails.stream().map {
      createContactDto(it.personId, it.firstName, it.lastName, it.dateOfBirth, it.approved, it.restrictions)
    }.collect(Collectors.toList())
  }

  final fun createVisitor(
    visitorId: Int = RandomUtils.nextInt(),
    firstName: String,
    lastName: String,
    dateOfBirth: LocalDate?,
    approved: Boolean = true,
    restrictions: List<RestrictionDto> = emptyList(),
  ): VisitorDetails {
    return VisitorDetails(visitorId.toLong(), firstName, lastName, dateOfBirth, approved, restrictions = restrictions)
  }

  final fun createVisitorDto(
    contact: PrisonerContactDto,
    visitContact: Boolean = false,
  ): VisitorDto {
    return VisitorDto(
      nomisPersonId = contact.personId!!,
      visitContact = visitContact,
    )
  }

  final fun createPrison(
    prisonCode: String = "HEI",
    active: Boolean = true,
    policyNoticeDaysMin: Int = 2,
    policyNoticeDaysMax: Int = 28,
    maxTotalVisitors: Int = 6,
    maxAdultVisitors: Int = 3,
    maxChildVisitors: Int = 3,
    adultAgeYears: Int = 18,
    clients: List<PrisonUserClientDto> = listOf(
      PrisonUserClientDto(STAFF, true),
      PrisonUserClientDto(PUBLIC, true),
    ),
  ): VisitSchedulerPrisonDto {
    return VisitSchedulerPrisonDto(
      prisonCode,
      active,
      policyNoticeDaysMin,
      policyNoticeDaysMax,
      maxTotalVisitors,
      maxAdultVisitors,
      maxChildVisitors,
      adultAgeYears,
      clients = clients,
    )
  }

  private fun getAvailableVisitSessionQueryParams(
    prisonCode: String,
    prisonerId: String,
    sessionRestriction: SessionRestriction,
    visitorIds: List<Long>? = null,
    withAppointmentsCheck: Boolean,
    excludedApplicationReference: String?,
    advanceFromDateByDays: Int?,
    currentUser: String? = null,
  ): List<String> {
    val queryParams = java.util.ArrayList<String>()
    queryParams.add("prisonId=$prisonCode")
    queryParams.add("prisonerId=$prisonerId")
    queryParams.add("sessionRestriction=${sessionRestriction.name}")
    visitorIds?.let {
      queryParams.add("visitors=${it.joinToString(",")}")
    }
    queryParams.add("withAppointmentsCheck=$withAppointmentsCheck")

    excludedApplicationReference?.let {
      queryParams.add("excludedApplicationReference=$excludedApplicationReference")
    }
    advanceFromDateByDays?.let {
      queryParams.add("advanceFromDateByDays=$advanceFromDateByDays")
    }
    currentUser?.let {
      queryParams.add("currentUser=$currentUser")
    }

    return queryParams
  }

  class VisitorDetails(
    val personId: Long,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate? = null,
    val approved: Boolean = true,
    val restrictions: List<RestrictionDto> = emptyList(),
  )

  protected fun assertVisitorDetails(visitors: List<OrchestrationVisitorDto>, contacts: List<PrisonerContactDto>) {
    for (visitor in visitors) {
      val contact = contacts.first { it.personId == visitor.nomisPersonId }
      Assertions.assertThat(visitor.nomisPersonId).isEqualTo(contact.personId)
      Assertions.assertThat(visitor.firstName).isNotNull()
      Assertions.assertThat(visitor.firstName).isEqualTo(contact.firstName)
      Assertions.assertThat(visitor.lastName).isNotNull()
      Assertions.assertThat(visitor.lastName).isEqualTo(contact.lastName)
    }
  }
}
