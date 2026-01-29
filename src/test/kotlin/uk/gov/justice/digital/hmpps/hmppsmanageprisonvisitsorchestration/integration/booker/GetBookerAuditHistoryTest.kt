package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.booker

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonVisitBookerRegistryClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller.PUBLIC_BOOKER_GET_BOOKER_AUDIT_PATH
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.BookerAuditDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.BookerHistoryAuditDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.ActionedByDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.EventAuditDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.ApplicationMethodType.WEBSITE
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.EventAuditType.BOOKED_VISIT
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.TestObjectMapper
import java.time.LocalDateTime

@DisplayName("Get permitted prisoners for booker")
class GetBookerAuditHistoryTest : IntegrationTestBase() {
  @MockitoSpyBean
  lateinit var prisonVisitBookerRegistryClientSpy: PrisonVisitBookerRegistryClient

  @Test
  fun `when booker has audit history both booker and visit history is combined and returned`() {
    // Given
    val bookerReference = "booker-1"
    val bookerAudit1 = BookerAuditDto(bookerReference = bookerReference, auditType = "BOOKER_CREATED", text = "booker created", createdTimestamp = LocalDateTime.now())
    val bookerAudit2 = BookerAuditDto(bookerReference = bookerReference, auditType = "REGISTER_PRISONER_SEARCH", text = "prisoner searched", createdTimestamp = LocalDateTime.now())
    val bookerAudit3 = BookerAuditDto(bookerReference = bookerReference, auditType = "PRISONER_REGISTERED", text = "prisoner registered", createdTimestamp = LocalDateTime.now())
    val bookerAudit4 = BookerAuditDto(bookerReference = bookerReference, auditType = "VISITOR_ADDED_TO_PRISONER", text = "visitor 1 added to prisoner", createdTimestamp = LocalDateTime.now())
    val bookerVisitAudit1 = EventAuditDto(
      type = BOOKED_VISIT,
      actionedBy = ActionedByDto(bookerReference = bookerReference, userName = null, userType = UserType.PUBLIC),
      createTimestamp = LocalDateTime.now(),
      applicationMethodType = WEBSITE,
    )
    val bookerAudit5 = BookerAuditDto(bookerReference = bookerReference, auditType = "VISITOR_ADDED_TO_PRISONER", text = "visitor 2 added to prisoner", createdTimestamp = LocalDateTime.now())

    visitSchedulerMockServer.stubGetBookerVisitAuditHistory(bookerReference, listOf(bookerVisitAudit1))
    prisonVisitBookerRegistryMockServer.stubGetBookerAuditHistory(bookerReference, listOf(bookerAudit1, bookerAudit2, bookerAudit3, bookerAudit4, bookerAudit5))

    // When
    val responseSpec = callGetBookerAuditHistory(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, bookerReference)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val bookerAuditHistoryList = getResults(returnResult)

    Assertions.assertThat(bookerAuditHistoryList.size).isEqualTo(6)
    Assertions.assertThat(bookerAuditHistoryList[0]).isEqualTo(BookerHistoryAuditDto(bookerAudit1))
    Assertions.assertThat(bookerAuditHistoryList[1]).isEqualTo(BookerHistoryAuditDto(bookerAudit2))
    Assertions.assertThat(bookerAuditHistoryList[2]).isEqualTo(BookerHistoryAuditDto(bookerAudit3))
    Assertions.assertThat(bookerAuditHistoryList[3]).isEqualTo(BookerHistoryAuditDto(bookerAudit4))
    Assertions.assertThat(bookerAuditHistoryList[4]).isEqualTo(BookerHistoryAuditDto(bookerVisitAudit1))
    Assertions.assertThat(bookerAuditHistoryList[5]).isEqualTo(BookerHistoryAuditDto(bookerAudit5))

    verify(visitSchedulerClientSpy, times(1)).getBookerHistoryAsMono(bookerReference)
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getBookerAuditHistoryAsMono(bookerReference)
  }

  @Test
  fun `when booker has no visit history only booker history is returned`() {
    // Given
    val bookerReference = "booker-1"
    val bookerAudit1 = BookerAuditDto(bookerReference = bookerReference, auditType = "BOOKER_CREATED", text = "booker created", createdTimestamp = LocalDateTime.now())
    val bookerAudit2 = BookerAuditDto(bookerReference = bookerReference, auditType = "REGISTER_PRISONER_SEARCH", text = "prisoner searched", createdTimestamp = LocalDateTime.now())
    val bookerAudit3 = BookerAuditDto(bookerReference = bookerReference, auditType = "PRISONER_REGISTERED", text = "prisoner registered", createdTimestamp = LocalDateTime.now())
    val bookerAudit4 = BookerAuditDto(bookerReference = bookerReference, auditType = "VISITOR_ADDED_TO_PRISONER", text = "visitor 1 added to prisoner", createdTimestamp = LocalDateTime.now())

    visitSchedulerMockServer.stubGetBookerVisitAuditHistory(bookerReference, emptyList())
    prisonVisitBookerRegistryMockServer.stubGetBookerAuditHistory(bookerReference, listOf(bookerAudit1, bookerAudit2, bookerAudit3, bookerAudit4))

    // When
    val responseSpec = callGetBookerAuditHistory(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, bookerReference)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val bookerAuditHistoryList = getResults(returnResult)

    Assertions.assertThat(bookerAuditHistoryList.size).isEqualTo(4)
    Assertions.assertThat(bookerAuditHistoryList[0]).isEqualTo(BookerHistoryAuditDto(bookerAudit1))
    Assertions.assertThat(bookerAuditHistoryList[1]).isEqualTo(BookerHistoryAuditDto(bookerAudit2))
    Assertions.assertThat(bookerAuditHistoryList[2]).isEqualTo(BookerHistoryAuditDto(bookerAudit3))
    Assertions.assertThat(bookerAuditHistoryList[3]).isEqualTo(BookerHistoryAuditDto(bookerAudit4))

    verify(visitSchedulerClientSpy, times(1)).getBookerHistoryAsMono(bookerReference)
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getBookerAuditHistoryAsMono(bookerReference)
  }

  @Test
  fun `when booker has no booker history only visit history is returned`() {
    // Given
    val bookerReference = "booker-1"
    val bookerVisitAudit1 = EventAuditDto(
      type = BOOKED_VISIT,
      actionedBy = ActionedByDto(bookerReference = bookerReference, userName = null, userType = UserType.PUBLIC),
      createTimestamp = LocalDateTime.now(),
      applicationMethodType = WEBSITE,
    )
    visitSchedulerMockServer.stubGetBookerVisitAuditHistory(bookerReference, listOf(bookerVisitAudit1))
    prisonVisitBookerRegistryMockServer.stubGetBookerAuditHistory(bookerReference, emptyList())

    // When
    val responseSpec = callGetBookerAuditHistory(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, bookerReference)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val bookerAuditHistoryList = getResults(returnResult)

    Assertions.assertThat(bookerAuditHistoryList.size).isEqualTo(1)
    Assertions.assertThat(bookerAuditHistoryList[0]).isEqualTo(BookerHistoryAuditDto(bookerVisitAudit1))

    verify(visitSchedulerClientSpy, times(1)).getBookerHistoryAsMono(bookerReference)
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getBookerAuditHistoryAsMono(bookerReference)
  }

  @Test
  fun `when booker audit call returns NOT_FOUND then NOT_FOUND error is returned`() {
    // Given
    val bookerReference = "booker-1"
    val bookerVisitAudit1 = EventAuditDto(
      type = BOOKED_VISIT,
      actionedBy = ActionedByDto(bookerReference = bookerReference, userName = null, userType = UserType.PUBLIC),
      createTimestamp = LocalDateTime.now(),
      applicationMethodType = WEBSITE,
    )
    prisonVisitBookerRegistryMockServer.stubGetBookerAuditHistory(bookerReference, null, HttpStatus.NOT_FOUND)
    visitSchedulerMockServer.stubGetBookerVisitAuditHistory(bookerReference, listOf(bookerVisitAudit1))

    // When
    val responseSpec = callGetBookerAuditHistory(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, bookerReference)

    // Then
    responseSpec.expectStatus().isNotFound
    verify(visitSchedulerClientSpy, times(1)).getBookerHistoryAsMono(bookerReference)
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getBookerAuditHistoryAsMono(bookerReference)
  }

  @Test
  fun `when booker audit call returns INTERNAL_SERVER_ERROR then INTERNAL_SERVER_ERROR error is returned`() {
    // Given
    val bookerReference = "booker-1"
    val bookerVisitAudit1 = EventAuditDto(
      type = BOOKED_VISIT,
      actionedBy = ActionedByDto(bookerReference = bookerReference, userName = null, userType = UserType.PUBLIC),
      createTimestamp = LocalDateTime.now(),
      applicationMethodType = WEBSITE,
    )
    prisonVisitBookerRegistryMockServer.stubGetBookerAuditHistory(bookerReference, null, HttpStatus.INTERNAL_SERVER_ERROR)
    visitSchedulerMockServer.stubGetBookerVisitAuditHistory(bookerReference, listOf(bookerVisitAudit1))

    // When
    val responseSpec = callGetBookerAuditHistory(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, bookerReference)

    // Then
    responseSpec.expectStatus().is5xxServerError
    verify(visitSchedulerClientSpy, times(1)).getBookerHistoryAsMono(bookerReference)
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getBookerAuditHistoryAsMono(bookerReference)
  }

  @Test
  fun `when visit scheduler booker audit call returns NOT_FOUND then NOT_FOUND error is returned`() {
    // Given
    val bookerReference = "booker-1"
    val bookerAudit1 = BookerAuditDto(bookerReference = bookerReference, auditType = "BOOKER_CREATED", text = "booker created", createdTimestamp = LocalDateTime.now())

    prisonVisitBookerRegistryMockServer.stubGetBookerAuditHistory(bookerReference, listOf(bookerAudit1))
    visitSchedulerMockServer.stubGetBookerVisitAuditHistory(bookerReference, null, HttpStatus.NOT_FOUND)

    // When
    val responseSpec = callGetBookerAuditHistory(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, bookerReference)

    // Then
    responseSpec.expectStatus().isNotFound
    verify(visitSchedulerClientSpy, times(1)).getBookerHistoryAsMono(bookerReference)
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getBookerAuditHistoryAsMono(bookerReference)
  }

  @Test
  fun `when visit scheduler booker audit call returns INTERNAL_SERVER_ERROR then INTERNAL_SERVER_ERROR error is returned`() {
    // Given
    val bookerReference = "booker-1"
    val bookerAudit1 = BookerAuditDto(bookerReference = bookerReference, auditType = "BOOKER_CREATED", text = "booker created", createdTimestamp = LocalDateTime.now())

    prisonVisitBookerRegistryMockServer.stubGetBookerAuditHistory(bookerReference, listOf(bookerAudit1))
    visitSchedulerMockServer.stubGetBookerVisitAuditHistory(bookerReference, null, HttpStatus.INTERNAL_SERVER_ERROR)

    // When
    val responseSpec = callGetBookerAuditHistory(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, bookerReference)

    // Then
    responseSpec.expectStatus().is5xxServerError
    verify(visitSchedulerClientSpy, times(1)).getBookerHistoryAsMono(bookerReference)
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getBookerAuditHistoryAsMono(bookerReference)
  }

  @Test
  fun `when booker audit history is called without correct role then FORBIDDEN status is returned`() {
    // When
    val invalidRoleHeaders = setAuthorisation(roles = listOf("ROLE_INVALID"))
    val responseSpec = callGetBookerAuditHistory(webTestClient, invalidRoleHeaders, "test")

    // Then
    responseSpec.expectStatus().isForbidden

    // And
    verify(prisonVisitBookerRegistryClientSpy, times(0)).bookerAuthorisation(any())
  }

  @Test
  fun `when booker audit history is called without token then UNAUTHORIZED status is returned`() {
    // When
    val url = PUBLIC_BOOKER_GET_BOOKER_AUDIT_PATH.replace("{bookerReference}", "bookerReference")
    val responseSpec = webTestClient.put().uri(url)
      .exchange()

    // Then
    responseSpec.expectStatus().isUnauthorized

    // And
    verify(prisonVisitBookerRegistryClientSpy, times(0)).bookerAuthorisation(any())
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): List<BookerHistoryAuditDto> = TestObjectMapper.mapper.readValue(returnResult.returnResult().responseBody, Array<BookerHistoryAuditDto>::class.java).toList()

  fun callGetBookerAuditHistory(
    webTestClient: WebTestClient,
    authHttpHeaders: (HttpHeaders) -> Unit,
    bookerReference: String,
  ): WebTestClient.ResponseSpec = webTestClient.get().uri(PUBLIC_BOOKER_GET_BOOKER_AUDIT_PATH.replace("{bookerReference}", bookerReference))
    .headers(authHttpHeaders)
    .exchange()
}
