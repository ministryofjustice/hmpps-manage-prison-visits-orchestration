package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.*
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.helper.JwtAuthHelper
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.VisitSchedulerMockServer
import java.time.LocalDateTime

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
abstract class IntegrationTestBase {
  companion object {
    val visitSchedulerMockServer = VisitSchedulerMockServer(ObjectMapper().registerModule(JavaTimeModule()))

    @BeforeAll
    @JvmStatic
    fun startMocks() {
      visitSchedulerMockServer.start()
    }

    @AfterAll
    @JvmStatic
    fun stopMocks() {
      visitSchedulerMockServer.stop()
    }
  }

  @Autowired
  lateinit var webTestClient: WebTestClient

  lateinit var roleVisitSchedulerHttpHeaders: (HttpHeaders) -> Unit

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthHelper

  @BeforeEach
  internal fun setUp() {
    roleVisitSchedulerHttpHeaders = setAuthorisation(roles = listOf("ROLE_VISIT_SCHEDULER"))
  }

  internal fun setAuthorisation(
    user: String = "AUTH_ADM",
    roles: List<String> = listOf(),
    scopes: List<String> = listOf()
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisation(user, roles, scopes)

  fun createVisitDto(
    reference: String = "aa-bb-cc-dd",
    applicationReference: String = "aaa-bbb-ccc-ddd"): VisitDto {
    return VisitDto(
      applicationReference = applicationReference,
      reference = reference,
      prisonerId = "AB12345DS",
      prisonCode = "MDI",
      visitRoom = "A1 L3",
      visitType = "SOCIAL",
      visitStatus = "BOOKED",
      visitRestriction = "OPEN",
      startTimestamp = LocalDateTime.now(),
      endTimestamp = LocalDateTime.now().plusHours(1),
      outcomeStatus = null,
      createdTimestamp = LocalDateTime.now(),
      modifiedTimestamp = LocalDateTime.now()
    )
  }

  fun createReserveVisitSlotDto(prisonerId: String): ReserveVisitSlotDto {
    val visitor = VisitorDto(1, false)
    return ReserveVisitSlotDto(
      prisonerId = prisonerId,
      prisonCode = "MDI",
      visitRoom = "A1 L3",
      visitType = "SOCIAL",
      visitRestriction = "OPEN",
      startTimestamp = LocalDateTime.now(),
      endTimestamp = LocalDateTime.now().plusHours(1),
      visitContact = null,
      visitors = setOf(visitor),
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
      endTimestamp = LocalDateTime.now().plusHours(1)
    )
  }
}
