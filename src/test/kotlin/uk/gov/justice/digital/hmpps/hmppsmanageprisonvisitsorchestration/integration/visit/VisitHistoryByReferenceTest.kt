package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.visit

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpHeaders
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.ManageUsersApiClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.VisitHistoryDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.ActionedByDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.EventAuditDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.ApplicationMethodType.EMAIL
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.ApplicationMethodType.NOT_APPLICABLE
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.EventAuditType.BOOKED_VISIT
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.EventAuditType.CANCELLED_VISIT
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.EventAuditType.IGNORE_VISIT_NOTIFICATIONS_EVENT
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.EventAuditType.NON_ASSOCIATION_EVENT
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.EventAuditType.UPDATED_VISIT
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType.PUBLIC
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType.STAFF
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType.SYSTEM
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase

private const val NOT_KNOWN = "NOT_KNOWN"

@DisplayName("Get visit history by reference")
@ExtendWith(SpringExtension::class)
class VisitHistoryByReferenceTest : IntegrationTestBase() {

  @MockitoSpyBean
  private lateinit var manageUsersApiClient: ManageUsersApiClient

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
    val createdByUserName = "created-user"
    val lastUpdatedByUserName = "updated-user"
    val cancelledByUserName = "cancelled-user"

    val createdBy = ActionedByDto(bookerReference = null, userName = createdByUserName, userType = STAFF)
    val lastUpdatedBy = ActionedByDto(bookerReference = null, userName = lastUpdatedByUserName, userType = STAFF)
    val cancelledBy = ActionedByDto(bookerReference = null, userName = cancelledByUserName, userType = STAFF)

    val eventList = mutableListOf(
      EventAuditDto(type = BOOKED_VISIT, actionedBy = createdBy, applicationMethodType = EMAIL),
      EventAuditDto(type = UPDATED_VISIT, actionedBy = lastUpdatedBy, applicationMethodType = EMAIL),
      EventAuditDto(type = CANCELLED_VISIT, actionedBy = cancelledBy, applicationMethodType = EMAIL),
      EventAuditDto(type = IGNORE_VISIT_NOTIFICATIONS_EVENT, actionedBy = createdBy, applicationMethodType = NOT_APPLICABLE, text = "ignore for now"),
      EventAuditDto(type = IGNORE_VISIT_NOTIFICATIONS_EVENT, actionedBy = cancelledBy, applicationMethodType = NOT_APPLICABLE, text = "ignore again"),
    )

    visitSchedulerMockServer.stubGetVisitHistory(reference, eventList)

    manageUsersApiMockServer.stubGetUserDetails(createdByUserName, "Aled")
    manageUsersApiMockServer.stubGetUserDetails(lastUpdatedByUserName, "Ben")
    manageUsersApiMockServer.stubGetUserDetails(cancelledByUserName, "Dhiraj")

    val visitDto = createVisitDto(reference = reference)
    visitSchedulerMockServer.stubGetVisit(reference, visitDto)

    // When
    val responseSpec = callVisitHistoryByReference(webTestClient, reference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val visitHistoryDetailsDto = getVisitHistoryDetailsDto(responseSpec)
    assertThat(visitHistoryDetailsDto.visit.reference).isEqualTo(visitDto.reference)

    assertThat(visitHistoryDetailsDto.eventsAudit[0].actionedByFullName).isEqualTo("Aled")
    assertThat(visitHistoryDetailsDto.eventsAudit[0].text).isNull()
    assertThat(visitHistoryDetailsDto.eventsAudit[1].actionedByFullName).isEqualTo("Ben")
    assertThat(visitHistoryDetailsDto.eventsAudit[1].text).isNull()
    assertThat(visitHistoryDetailsDto.eventsAudit[2].actionedByFullName).isEqualTo("Dhiraj")
    assertThat(visitHistoryDetailsDto.eventsAudit[2].text).isNull()
    assertThat(visitHistoryDetailsDto.eventsAudit[3].actionedByFullName).isEqualTo("Aled")
    assertThat(visitHistoryDetailsDto.eventsAudit[3].text).isEqualTo("ignore for now")
    assertThat(visitHistoryDetailsDto.eventsAudit[4].actionedByFullName).isEqualTo("Dhiraj")
    assertThat(visitHistoryDetailsDto.eventsAudit[4].text).isEqualTo("ignore again")
  }

  @Test
  fun `when visit history is requested only user full names are given for visits that are created by Staff`() {
    // Given
    val reference = "aa-bb-cc-dd"
    val lastUpdatedByUserName = "updated-user"
    val cancelledByUserName = "cancelled-user"

    val createdBy = ActionedByDto(bookerReference = "asd-asd-asd", userName = null, userType = PUBLIC)
    val lastUpdatedBy = ActionedByDto(bookerReference = null, userName = lastUpdatedByUserName, userType = STAFF)
    val cancelledBy = ActionedByDto(bookerReference = null, userName = cancelledByUserName, userType = STAFF)
    val notification = ActionedByDto(bookerReference = null, userName = null, userType = SYSTEM)

    val eventList = mutableListOf(
      EventAuditDto(type = BOOKED_VISIT, actionedBy = createdBy, applicationMethodType = EMAIL),
      EventAuditDto(type = UPDATED_VISIT, actionedBy = lastUpdatedBy, applicationMethodType = EMAIL),
      EventAuditDto(type = CANCELLED_VISIT, actionedBy = cancelledBy, applicationMethodType = EMAIL),
      EventAuditDto(type = NON_ASSOCIATION_EVENT, actionedBy = notification, applicationMethodType = EMAIL),
    )

    visitSchedulerMockServer.stubGetVisitHistory(reference, eventList)

    manageUsersApiMockServer.stubGetUserDetails(lastUpdatedByUserName, "Ben")
    manageUsersApiMockServer.stubGetUserDetails(cancelledByUserName, "Dhiraj")

    val visitDto = createVisitDto(reference = reference)
    visitSchedulerMockServer.stubGetVisit(reference, visitDto)

    // When
    val responseSpec = callVisitHistoryByReference(webTestClient, reference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val visitHistoryDetailsDto = getVisitHistoryDetailsDto(responseSpec)
    assertThat(visitHistoryDetailsDto.visit.reference).isEqualTo(visitDto.reference)

    assertThat(visitHistoryDetailsDto.eventsAudit[0].actionedByFullName).isNull()
    assertThat(visitHistoryDetailsDto.eventsAudit[0].userType).isEqualTo(PUBLIC)
    assertThat(visitHistoryDetailsDto.eventsAudit[1].actionedByFullName).isEqualTo("Ben")
    assertThat(visitHistoryDetailsDto.eventsAudit[1].userType).isEqualTo(STAFF)
    assertThat(visitHistoryDetailsDto.eventsAudit[2].actionedByFullName).isEqualTo("Dhiraj")
    assertThat(visitHistoryDetailsDto.eventsAudit[2].userType).isEqualTo(STAFF)
    assertThat(visitHistoryDetailsDto.eventsAudit[3].actionedByFullName).isNull()
    assertThat(visitHistoryDetailsDto.eventsAudit[3].userType).isEqualTo(SYSTEM)
  }

  private fun getVisitHistoryDetailsDto(responseSpec: ResponseSpec) =
    objectMapper.readValue(responseSpec.expectBody().returnResult().responseBody, VisitHistoryDetailsDto::class.java)

  @Test
  fun `when all user details then only call auth once `() {
    // Given
    val reference = "aa-bb-cc-dd"
    val actionedByUserName = "created-user"

    val actionedBy = ActionedByDto(bookerReference = null, userName = actionedByUserName, userType = STAFF)

    val eventList = mutableListOf(
      EventAuditDto(type = BOOKED_VISIT, actionedBy = actionedBy, applicationMethodType = EMAIL),
      EventAuditDto(type = UPDATED_VISIT, actionedBy = actionedBy, applicationMethodType = EMAIL),
      EventAuditDto(type = CANCELLED_VISIT, actionedBy = actionedBy, applicationMethodType = EMAIL),
    )

    visitSchedulerMockServer.stubGetVisitHistory(reference, eventList)
    manageUsersApiMockServer.stubGetUserDetails(actionedByUserName, "Aled")
    val visitDto = createVisitDto(reference = reference)
    visitSchedulerMockServer.stubGetVisit(reference, visitDto)

    // When
    val responseSpec = callVisitHistoryByReference(webTestClient, reference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk

    verify(manageUsersApiClient, times(1)).getUserDetails(any())
  }

  @Test
  fun `when visit exists but userid is NOT_KNOWN search by reference returns the visit and names as NOT_KNOWN`() {
    // Given
    val reference = "aa-bb-cc-dd"
    val actionedByUserName = NOT_KNOWN

    val actionedBy = ActionedByDto(bookerReference = null, userName = actionedByUserName, userType = STAFF)

    val eventList = mutableListOf(
      EventAuditDto(type = BOOKED_VISIT, actionedBy = actionedBy, applicationMethodType = EMAIL),
    )

    visitSchedulerMockServer.stubGetVisitHistory(reference, eventList)

    manageUsersApiMockServer.stubGetUserDetails(actionedByUserName, "Aled")

    val visitDto = createVisitDto(reference = reference)
    visitSchedulerMockServer.stubGetVisit(reference, visitDto)

    // When
    val responseSpec = callVisitHistoryByReference(webTestClient, reference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk

    val visitHistoryDetailsDto = getVisitHistoryDetailsDto(responseSpec)
    assertThat(visitHistoryDetailsDto.eventsAudit[0].actionedByFullName).isEqualTo(NOT_KNOWN)
  }

  @Test
  fun `when visit exists but userid is null search by reference returns the visit and names as null`() {
    // Given
    val reference = "aa-bb-cc-dd"
    val createdByName = "invalid-user"

    val createdBy = ActionedByDto(bookerReference = null, userName = createdByName, userType = STAFF)
    val updatedBy = ActionedByDto(bookerReference = null, userName = null, userType = STAFF)
    val cancelBy = ActionedByDto(bookerReference = null, userName = null, userType = STAFF)

    val eventList = mutableListOf(
      EventAuditDto(type = BOOKED_VISIT, actionedBy = createdBy, applicationMethodType = EMAIL),
      EventAuditDto(type = UPDATED_VISIT, actionedBy = updatedBy, applicationMethodType = EMAIL),
      EventAuditDto(type = CANCELLED_VISIT, actionedBy = cancelBy, applicationMethodType = EMAIL),
    )

    visitSchedulerMockServer.stubGetVisitHistory(reference, eventList)

    val visitDto = createVisitDto(reference = reference)
    visitSchedulerMockServer.stubGetVisit(reference, visitDto)

    // When
    val responseSpec = callVisitHistoryByReference(webTestClient, reference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val visitHistoryDetailsDto = getVisitHistoryDetailsDto(responseSpec)
    assertThat(visitHistoryDetailsDto.eventsAudit[0].actionedByFullName).isEqualTo(createdByName)
    assertThat(visitHistoryDetailsDto.eventsAudit[1].actionedByFullName).isNull()
    assertThat(visitHistoryDetailsDto.eventsAudit[2].actionedByFullName).isNull()
  }

  @Test
  fun `when visit does not exist search by reference returns NOT_FOUND status`() {
    // Given
    val reference = "xx-yy-cc-dd"
    visitSchedulerMockServer.stubGetVisit(reference, null)

    // When
    val responseSpec = callVisitHistoryByReference(webTestClient, reference, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
  }
}
