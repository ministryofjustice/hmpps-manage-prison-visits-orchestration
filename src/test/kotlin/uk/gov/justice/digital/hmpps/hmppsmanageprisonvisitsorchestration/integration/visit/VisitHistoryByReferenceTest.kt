package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.visit

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.HttpHeaders
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.HmppsAuthClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.VisitHistoryDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.EventAuditDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.ApplicationMethodType.EMAIL
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.EventAuditType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.HmppsAuthExtension.Companion.hmppsAuthApi

private const val NOT_KNOWN = "NOT_KNOWN"

@DisplayName("Get visit history by reference")
@ExtendWith(SpringExtension::class)
class VisitHistoryByReferenceTest : IntegrationTestBase() {

  @SpyBean
  private lateinit var hmppsAuthClient: HmppsAuthClient

  fun callVisitHistoryByReference(
    webTestClient: WebTestClient,
    reference: String,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): ResponseSpec {
    return webTestClient.get().uri("/visits/$reference/history")
      .headers(authHttpHeaders)
      .exchange()
  }

  @Test
  fun `when visit exists the full details search by reference returns the visit full names`() {
    // Given
    val reference = "aa-bb-cc-dd"
    val createdBy = "created-user"
    val lastUpdatedBy = "updated-user"
    val cancelledBy = "cancelled-user"

    val eventList = mutableListOf(
      EventAuditDto(type = EventAuditType.BOOKED_VISIT, actionedBy = createdBy, applicationMethodType = EMAIL),
      EventAuditDto(type = EventAuditType.UPDATED_VISIT, actionedBy = lastUpdatedBy, applicationMethodType = EMAIL),
      EventAuditDto(type = EventAuditType.CANCELLED_VISIT, actionedBy = cancelledBy, applicationMethodType = EMAIL),
    )

    visitSchedulerMockServer.stubGetVisitHistory(reference, eventList)

    hmppsAuthApi.stubGetUserDetails(createdBy, "Aled")
    hmppsAuthApi.stubGetUserDetails(lastUpdatedBy, "Ben")
    hmppsAuthApi.stubGetUserDetails(cancelledBy, "Dhiraj")

    val visitDto = createVisitDto(reference = reference)
    visitSchedulerMockServer.stubGetVisit(reference, visitDto)

    // When
    val responseSpec = callVisitHistoryByReference(webTestClient, reference, roleVisitSchedulerHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val visitHistoryDetailsDto = getVisitHistoryDetailsDto(responseSpec)
    Assertions.assertThat(visitHistoryDetailsDto.visit.reference).isEqualTo(visitDto.reference)

    Assertions.assertThat(visitHistoryDetailsDto.eventsAudit[0].actionedBy).isEqualTo("Aled")
    Assertions.assertThat(visitHistoryDetailsDto.eventsAudit[1].actionedBy).isEqualTo("Ben")
    Assertions.assertThat(visitHistoryDetailsDto.eventsAudit[2].actionedBy).isEqualTo("Dhiraj")
  }

  private fun getVisitHistoryDetailsDto(responseSpec: ResponseSpec) =
    objectMapper.readValue(responseSpec.expectBody().returnResult().responseBody, VisitHistoryDetailsDto::class.java)

  @Test
  fun `when all user details then only call auth once `() {
    // Given
    val reference = "aa-bb-cc-dd"
    val actionedBy = "created-user"

    val eventList = mutableListOf(
      EventAuditDto(type = EventAuditType.BOOKED_VISIT, actionedBy = actionedBy, applicationMethodType = EMAIL),
      EventAuditDto(type = EventAuditType.UPDATED_VISIT, actionedBy = actionedBy, applicationMethodType = EMAIL),
      EventAuditDto(type = EventAuditType.CANCELLED_VISIT, actionedBy = actionedBy, applicationMethodType = EMAIL),
    )

    visitSchedulerMockServer.stubGetVisitHistory(reference, eventList)
    hmppsAuthApi.stubGetUserDetails(actionedBy, "Aled")
    val visitDto = createVisitDto(reference = reference)
    visitSchedulerMockServer.stubGetVisit(reference, visitDto)

    // When
    val responseSpec = callVisitHistoryByReference(webTestClient, reference, roleVisitSchedulerHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk

    verify(hmppsAuthClient, times(1)).getUserDetails(any())
  }

  @Test
  fun `when visit exists but userid is NOT_KNOWN search by reference returns the visit and names as NOT_KNOWN`() {
    // Given
    val reference = "aa-bb-cc-dd"
    val actionedBy = NOT_KNOWN

    val eventList = mutableListOf(
      EventAuditDto(type = EventAuditType.BOOKED_VISIT, actionedBy = actionedBy, applicationMethodType = EMAIL),
    )

    visitSchedulerMockServer.stubGetVisitHistory(reference, eventList)

    hmppsAuthApi.stubGetUserDetails(actionedBy, "Aled")

    val visitDto = createVisitDto(reference = reference)
    visitSchedulerMockServer.stubGetVisit(reference, visitDto)

    // When
    val responseSpec = callVisitHistoryByReference(webTestClient, reference, roleVisitSchedulerHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk

    val visitHistoryDetailsDto = getVisitHistoryDetailsDto(responseSpec)
    Assertions.assertThat(visitHistoryDetailsDto.eventsAudit[0].actionedBy).isEqualTo(NOT_KNOWN)
  }

  @Test
  fun `when visit exists but userid is null search by reference returns the visit and names as null`() {
    // Given
    val reference = "aa-bb-cc-dd"
    val createdBy = "invalid-user"
    val eventList = mutableListOf(
      EventAuditDto(type = EventAuditType.BOOKED_VISIT, actionedBy = createdBy, applicationMethodType = EMAIL),
      EventAuditDto(type = EventAuditType.UPDATED_VISIT, actionedBy = null, applicationMethodType = EMAIL),
      EventAuditDto(type = EventAuditType.CANCELLED_VISIT, actionedBy = null, applicationMethodType = EMAIL),
    )

    visitSchedulerMockServer.stubGetVisitHistory(reference, eventList)

    val visitDto = createVisitDto(reference = reference)
    visitSchedulerMockServer.stubGetVisit(reference, visitDto)

    // When
    val responseSpec = callVisitHistoryByReference(webTestClient, reference, roleVisitSchedulerHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val visitHistoryDetailsDto = getVisitHistoryDetailsDto(responseSpec)
    Assertions.assertThat(visitHistoryDetailsDto.eventsAudit[0].actionedBy).isEqualTo(createdBy)
    Assertions.assertThat(visitHistoryDetailsDto.eventsAudit[1].actionedBy).isNull()
    Assertions.assertThat(visitHistoryDetailsDto.eventsAudit[2].actionedBy).isNull()
  }

  @Test
  fun `when visit does not exist search by reference returns NOT_FOUND status`() {
    // Given
    val reference = "xx-yy-cc-dd"
    visitSchedulerMockServer.stubGetVisit(reference, null)

    // When
    val responseSpec = callVisitHistoryByReference(webTestClient, reference, roleVisitSchedulerHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
  }
}
