package uk.gov.justice.digital.hmpps.visits.orchestration.integration.visit

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec
import uk.gov.justice.digital.hmpps.visits.orchestration.controller.FUTURE_NOTIFICATION_VISITS
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.orchestration.OrchestrationVisitNotificationsDto
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.ActionedByDto
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.enums.UserType
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.visitnotification.NotificationEventType
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.visitnotification.NotificationEventType.PRISON_VISITS_BLOCKED_FOR_DATE
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.visitnotification.VisitNotificationEventDto
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.visitnotification.VisitNotificationsDto
import uk.gov.justice.digital.hmpps.visits.orchestration.integration.IntegrationTestBase
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("GET $FUTURE_NOTIFICATION_VISITS")
class FutureVisitsWithNotificationsTest : IntegrationTestBase() {

  @BeforeEach
  fun resetStubs() {
    visitSchedulerMockServer.resetAll()
  }

  val prisonCode = "ABC"

  @Test
  fun `when future visits with notifications are requested then appropriate results are returned`() {
    // Given
    val notifications = listOf(
      VisitNotificationEventDto(
        type = PRISON_VISITS_BLOCKED_FOR_DATE,
        notificationEventReference = "ds",
        createdDateTime = LocalDateTime.now(),
        additionalData = emptyList(),
      ),
    )

    val notification1 = VisitNotificationsDto(
      visitReference = "visit-1",
      prisonerNumber = "AB123456",
      bookedBy = ActionedByDto(bookerReference = null, userName = "test", userType = UserType.STAFF),
      visitDate = LocalDate.now().plusDays(1),
      notifications = notifications,
    )

    visitSchedulerMockServer.stubGetFutureVisitsWithNotificationsForPrison(prisonCode, notificationEventTypes = null, listOf(notification1))
    // When
    val responseSpec = callFutureNotificationVisits(webTestClient, prisonCode, null, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val visitsWithNotifications = this.getVisitsWithNotificationDtos(responseSpec)
    Assertions.assertThat(visitsWithNotifications.size).isEqualTo(1)
    Assertions.assertThat(visitsWithNotifications[0].prisonerNumber).isEqualTo("AB123456")
    Assertions.assertThat(visitsWithNotifications[0].visitReference).isEqualTo("visit-1")
    Assertions.assertThat(visitsWithNotifications[0].bookedByName).isEqualTo("NOT_KNOWN")
    Assertions.assertThat(visitsWithNotifications[0].bookedByUserName).isEqualTo("test")
    Assertions.assertThat(visitsWithNotifications[0].visitDate).isEqualTo(LocalDate.now().plusDays(1))
    Assertions.assertThat(visitsWithNotifications[0].notifications).isEqualTo(notifications)
  }

  @Test
  fun `when future visits with notifications for certain notification types are requested then appropriate results are returned`() {
    // Given
    val notifications = listOf(
      VisitNotificationEventDto(
        type = PRISON_VISITS_BLOCKED_FOR_DATE,
        notificationEventReference = "ds",
        createdDateTime = LocalDateTime.now(),
        additionalData = emptyList(),
      ),
    )

    val notification1 = VisitNotificationsDto(
      visitReference = "visit-1",
      prisonerNumber = "AB123456",
      bookedBy = ActionedByDto(bookerReference = null, userName = "test", userType = UserType.STAFF),
      visitDate = LocalDate.now().plusDays(1),
      notifications = notifications,
    )

    visitSchedulerMockServer.stubGetFutureVisitsWithNotificationsForPrison(prisonCode, notificationEventTypes = listOf(PRISON_VISITS_BLOCKED_FOR_DATE), listOf(notification1))
    // When
    val responseSpec = callFutureNotificationVisits(webTestClient, prisonCode, notificationEventTypes = listOf(PRISON_VISITS_BLOCKED_FOR_DATE), roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val visitsWithNotifications = this.getVisitsWithNotificationDtos(responseSpec)
    Assertions.assertThat(visitsWithNotifications.size).isEqualTo(1)
    Assertions.assertThat(visitsWithNotifications[0].prisonerNumber).isEqualTo("AB123456")
    Assertions.assertThat(visitsWithNotifications[0].visitReference).isEqualTo("visit-1")
    Assertions.assertThat(visitsWithNotifications[0].bookedByName).isEqualTo("NOT_KNOWN")
    Assertions.assertThat(visitsWithNotifications[0].bookedByUserName).isEqualTo("test")
    Assertions.assertThat(visitsWithNotifications[0].visitDate).isEqualTo(LocalDate.now().plusDays(1))
    Assertions.assertThat(visitsWithNotifications[0].notifications).isEqualTo(notifications)
  }

  @Test
  fun `when future visits with notifications are requested but none exist then empty results are returned`() {
    // Given
    visitSchedulerMockServer.stubGetFutureVisitsWithNotificationsForPrison(prisonCode, notificationEventTypes = null, emptyList())
    // When
    val responseSpec = callFutureNotificationVisits(webTestClient, prisonCode, null, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val visitsWithNotifications = this.getVisitsWithNotificationDtos(responseSpec)
    Assertions.assertThat(visitsWithNotifications.size).isEqualTo(0)
  }

  @Test
  fun `when no role specified then access forbidden status is returned`() {
    // Given
    val authHttpHeaders = setAuthorisation(roles = listOf())

    // When
    val responseSpec = callFutureNotificationVisits(webTestClient, prisonCode, null, authHttpHeaders)

    // Then
    responseSpec.expectStatus().isForbidden
  }

  @Test
  fun `when no token passed then unauthorized status is returned`() {
    // Given

    // When
    val responseSpec = webTestClient.get().uri(FUTURE_NOTIFICATION_VISITS.replace("{prisonCode}", prisonCode)).exchange()

    // Then
    responseSpec.expectStatus().isUnauthorized
  }

  fun getVisitsWithNotificationDtos(responseSpec: ResponseSpec): Array<OrchestrationVisitNotificationsDto> = objectMapper.readValue(responseSpec.expectBody().returnResult().responseBody, Array<OrchestrationVisitNotificationsDto>::class.java)

  fun callFutureNotificationVisits(
    webTestClient: WebTestClient,
    prisonCode: String,
    notificationEventTypes: List<NotificationEventType>?,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): ResponseSpec {
    var url = FUTURE_NOTIFICATION_VISITS.replace("{prisonCode}", prisonCode)
    if (!notificationEventTypes.isNullOrEmpty()) {
      url += "?types=${notificationEventTypes.joinToString(",") { it.name }}"
    }

    return webTestClient.get().uri(url)
      .headers(authHttpHeaders)
      .exchange()
  }
}
