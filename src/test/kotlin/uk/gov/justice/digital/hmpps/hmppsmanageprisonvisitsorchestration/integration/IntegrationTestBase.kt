package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
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
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.register.PrisonNameDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.CurrentIncentive
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.ContactDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSessionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitorDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.application.ApplicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.application.ChangeApplicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.application.CreateApplicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.CreateApplicationRestriction
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.OutcomeStatus
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitRestriction
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitStatus
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.helper.JwtAuthHelper
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.HmppsAuthExtension
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.ManageUsersApiMockServer
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.PrisonApiMockServer
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.PrisonOffenderSearchMockServer
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.PrisonRegisterMockServer
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.PrisonerContactRegistryMockServer
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.VisitSchedulerMockServer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.ArrayList

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(HmppsAuthExtension::class)
abstract class IntegrationTestBase {
  companion object {
    val objectMapper: ObjectMapper = ObjectMapper().registerModule(JavaTimeModule())
    val visitSchedulerMockServer = VisitSchedulerMockServer(objectMapper)
    val prisonApiMockServer = PrisonApiMockServer(objectMapper)
    val prisonOffenderSearchMockServer = PrisonOffenderSearchMockServer(objectMapper)
    val prisonerContactRegistryMockServer = PrisonerContactRegistryMockServer(objectMapper)
    val prisonRegisterMockServer = PrisonRegisterMockServer(objectMapper)
    val manageUsersApiMockServer = ManageUsersApiMockServer(objectMapper)

    @BeforeAll
    @JvmStatic
    fun startMocks() {
      visitSchedulerMockServer.start()
      prisonApiMockServer.start()
      prisonOffenderSearchMockServer.start()
      prisonerContactRegistryMockServer.start()
      prisonRegisterMockServer.start()
      manageUsersApiMockServer.start()
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

  lateinit var roleVisitSchedulerHttpHeaders: (HttpHeaders) -> Unit

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthHelper

  @Autowired
  protected lateinit var objectMapper: ObjectMapper

  @BeforeEach
  internal fun setUp() {
    roleVisitSchedulerHttpHeaders = setAuthorisation(roles = listOf("ROLE_VISIT_SCHEDULER"))
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
    sessionTemplateReference: String = "ref.ref.ref",
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
      applicationRestriction = CreateApplicationRestriction.OPEN,
      sessionDate = sessionDate!!,
      visitContact = null,
      visitors = setOf(visitor),
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
    )
  }

  fun createChangeApplicationDto(): ChangeApplicationDto {
    val visitor = VisitorDto(1, false)
    return ChangeApplicationDto(
      applicationRestriction = CreateApplicationRestriction.OPEN,
      sessionDate = LocalDate.now(),
      visitContact = null,
      visitors = setOf(visitor),
      sessionTemplateReference = "aa-bb-cc-dd",
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

  fun getOrchestrationVisitsBySessionTemplateQueryParams(
    sessionDate: LocalDate,
    visitStatus: List<String>,
    visitRestrictions: List<VisitRestriction>?,
    page: Int,
    size: Int,
  ): List<String> {
    val queryParams = ArrayList<String>()
    queryParams.add("sessionDate=$sessionDate")
    visitStatus.forEach {
      queryParams.add("visitStatus=$it")
    }
    visitRestrictions?.let {
      visitRestrictions.forEach {
        queryParams.add("visitRestrictions=$it")
      }
    }
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
    page: Int,
    size: Int,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    return webTestClient.get()
      .uri("/visits/session-template/$sessionTemplateReference?${getOrchestrationVisitsBySessionTemplateQueryParams(sessionDate, visitStatus, visitRestriction, page, size).joinToString("&")}")
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

  final fun createPrisoner(
    prisonerId: String,
    firstName: String,
    lastName: String,
    dateOfBirth: LocalDate,
    prisonId: String = "MDI",
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
}
