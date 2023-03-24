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
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.ChangeVisitSlotRequestDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.ReserveVisitSlotDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSchedulerReserveVisitSlotDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSessionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitorDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.helper.JwtAuthHelper
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.HmppsAuthExtension
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.PrisonApiMockServer
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.PrisonOffenderSearchMockServer
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.VisitSchedulerMockServer
import java.time.LocalDateTime
import java.util.ArrayList

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(HmppsAuthExtension::class)
abstract class IntegrationTestBase {
  companion object {
    val visitSchedulerMockServer = VisitSchedulerMockServer(ObjectMapper().registerModule(JavaTimeModule()))
    val prisonApiMockServer = PrisonApiMockServer(ObjectMapper().registerModule(JavaTimeModule()))
    val prisonOffenderSearchMockServer = PrisonOffenderSearchMockServer(ObjectMapper().registerModule(JavaTimeModule()))

    @BeforeAll
    @JvmStatic
    fun startMocks() {
      visitSchedulerMockServer.start()
      prisonApiMockServer.start()
      prisonOffenderSearchMockServer.start()
    }

    @AfterAll
    @JvmStatic
    fun stopMocks() {
      visitSchedulerMockServer.stop()
      prisonApiMockServer.stop()
      prisonOffenderSearchMockServer.stop()
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
    visitType: String = "SOCIAL",
    visitStatus: String = "BOOKED",
    visitRestriction: String = "OPEN",
    startTimestamp: LocalDateTime = LocalDateTime.now(),
    endTimestamp: LocalDateTime = startTimestamp.plusHours(1),
    outcomeStatus: String? = null,
    createdBy: String = "created-by",
    updatedBy: String? = null,
    cancelledBy: String? = null,
    createdTimestamp: LocalDateTime = LocalDateTime.now(),
    modifiedTimestamp: LocalDateTime = LocalDateTime.now(),
  ): VisitDto {
    return VisitDto(
      applicationReference = applicationReference,
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
      createdBy = createdBy,
      updatedBy = updatedBy,
      cancelledBy = cancelledBy,
      createdTimestamp = createdTimestamp,
      modifiedTimestamp = modifiedTimestamp,
    )
  }

  fun createReserveVisitSlotDto(prisonerId: String): VisitSchedulerReserveVisitSlotDto {
    val visitor = VisitorDto(1, false)
    return VisitSchedulerReserveVisitSlotDto(
      ReserveVisitSlotDto(
        prisonerId = prisonerId,
        prisonCode = "MDI",
        visitRoom = "A1 L3",
        visitType = "SOCIAL",
        visitRestriction = "OPEN",
        startTimestamp = LocalDateTime.now(),
        endTimestamp = LocalDateTime.now().plusHours(1),
        visitContact = null,
        visitors = setOf(visitor),
      ),
      actionedBy = "user -1",
    )
  }

  fun createChangeVisitSlotRequestDto(): ChangeVisitSlotRequestDto {
    val visitor = VisitorDto(1, false)
    return ChangeVisitSlotRequestDto(
      visitRestriction = "OPEN",
      startTimestamp = LocalDateTime.now(),
      endTimestamp = LocalDateTime.now().plusHours(1),
      visitContact = null,
      visitors = setOf(visitor),
    )
  }

  fun createVisitSessionDto(prisonCode: String, sessionTemplateId: Long): VisitSessionDto {
    return VisitSessionDto(
      sessionTemplateId = sessionTemplateId,
      prisonCode = prisonCode,
      visitRoomName = "Visit Room",
      visitType = "Social",
      closedVisitCapacity = 5,
      openVisitCapacity = 30,
      startTimestamp = LocalDateTime.now(),
      endTimestamp = LocalDateTime.now().plusHours(1),
    )
  }
}
