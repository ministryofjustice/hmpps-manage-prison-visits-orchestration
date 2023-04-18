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
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.HmppsAuthClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.VisitHistoryDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.OutcomeStatus
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitStatus
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.HmppsAuthExtension.Companion.hmppsAuthApi
import java.time.LocalDateTime

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
  ): WebTestClient.ResponseSpec {
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
    val visitDto = createVisitDto(reference = reference, createdBy = createdBy, updatedBy = lastUpdatedBy, cancelledBy = cancelledBy)
    visitSchedulerMockServer.stubGetVisitHistory(reference, visitDto)

    hmppsAuthApi.stubGetUserDetails(createdBy, "Aled")
    hmppsAuthApi.stubGetUserDetails(lastUpdatedBy, "Ben")
    hmppsAuthApi.stubGetUserDetails(cancelledBy, "Dhiraj")

    // When
    val responseSpec = callVisitHistoryByReference(webTestClient, reference, roleVisitSchedulerHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val visitHistoryDetailsDto = objectMapper.readValue(responseSpec.expectBody().returnResult().responseBody, VisitHistoryDetailsDto::class.java)
    Assertions.assertThat(visitHistoryDetailsDto.visit.reference).isEqualTo(visitDto.reference)
    Assertions.assertThat(visitHistoryDetailsDto.createdBy).isEqualTo("Aled")
    Assertions.assertThat(visitHistoryDetailsDto.updatedBy).isEqualTo("Ben")
    Assertions.assertThat(visitHistoryDetailsDto.cancelledBy).isEqualTo("Dhiraj")
  }

  @Test
  fun `when all user details then only call auth once `() {
    // Given
    val reference = "aa-bb-cc-dd"
    val createdBy = "created-user"
    val lastUpdatedBy = createdBy
    val cancelledBy = createdBy
    val visitDto = createVisitDto(reference = reference, createdBy = createdBy, updatedBy = lastUpdatedBy, cancelledBy = cancelledBy)
    visitSchedulerMockServer.stubGetVisitHistory(reference, visitDto)

    hmppsAuthApi.stubGetUserDetails(createdBy, "Aled")

    // When
    val responseSpec = callVisitHistoryByReference(webTestClient, reference, roleVisitSchedulerHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val visitDtoResponse = objectMapper.readValue(responseSpec.expectBody().returnResult().responseBody, VisitHistoryDetailsDto::class.java)
    Assertions.assertThat(visitDtoResponse.createdBy).isEqualTo("Aled")

    verify(hmppsAuthClient, times(1)).getUserDetails(any())
  }

  @Test
  fun `when visit exists but userid is NOT_KNOWN search by reference returns the visit and names as NOT_KNOWN`() {
    // Given
    val reference = "aa-bb-cc-dd"
    val createdBy = NOT_KNOWN
    val lastUpdatedBy = NOT_KNOWN
    val cancelledBy = NOT_KNOWN

    val visitDto = createVisitDto(reference = reference, createdBy = createdBy, updatedBy = lastUpdatedBy, cancelledBy = cancelledBy)
    visitSchedulerMockServer.stubGetVisitHistory(reference, visitDto)

    // When
    val responseSpec = callVisitHistoryByReference(webTestClient, reference, roleVisitSchedulerHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val visitHistoryDetails = objectMapper.readValue(responseSpec.expectBody().returnResult().responseBody, VisitHistoryDetailsDto::class.java)
    Assertions.assertThat(visitHistoryDetails.visit.reference).isEqualTo(visitDto.reference)
    Assertions.assertThat(visitHistoryDetails.createdBy).isEqualTo(NOT_KNOWN)
    Assertions.assertThat(visitHistoryDetails.updatedBy).isEqualTo(NOT_KNOWN)
    Assertions.assertThat(visitHistoryDetails.cancelledBy).isEqualTo(NOT_KNOWN)
  }

  @Test
  fun `when visit exists but userid is null search by reference returns the visit and names as null`() {
    // Given
    val reference = "aa-bb-cc-dd"
    val createdBy = "invalid-user"
    val visitDto = createVisitDto(reference = reference, createdBy = createdBy, updatedBy = null, cancelledBy = null)
    visitSchedulerMockServer.stubGetVisitHistory(reference, visitDto)

    // When
    val responseSpec = callVisitHistoryByReference(webTestClient, reference, roleVisitSchedulerHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val visitHistoryDetails = objectMapper.readValue(responseSpec.expectBody().returnResult().responseBody, VisitHistoryDetailsDto::class.java)
    Assertions.assertThat(visitHistoryDetails.visit.reference).isEqualTo(visitDto.reference)
    Assertions.assertThat(visitHistoryDetails.createdBy).isEqualTo(createdBy)
    Assertions.assertThat(visitHistoryDetails.updatedBy).isNull()
    Assertions.assertThat(visitHistoryDetails.cancelledBy).isNull()
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

  @Test
  fun `when visit has been booked,updated and then cancel the correct dates and times are returned`() {
    // Given
    val reference = "aa-bb-cc-dd"

    val createDateAndTime = LocalDateTime.of(2023, 1, 3, 21, 10, 20)
    val updateDateAndTime = LocalDateTime.of(2023, 1, 4, 21, 10, 20)
    val cancelDateAndTime = LocalDateTime.of(2023, 1, 5, 21, 10, 20)

    val visitBookedDto = createVisitDto(
      visitStatus = VisitStatus.CANCELLED,
      outcomeStatus = OutcomeStatus.SUPERSEDED_CANCELLATION,
      reference = reference,
      createdTimestamp = createDateAndTime,
    )
    val visitUpdateDto1 = createVisitDto(visitStatus = VisitStatus.CANCELLED, outcomeStatus = OutcomeStatus.SUPERSEDED_CANCELLATION, reference = reference)
    val visitUpdateDto2 = createVisitDto(visitStatus = VisitStatus.CANCELLED, outcomeStatus = OutcomeStatus.SUPERSEDED_CANCELLATION, reference = reference)

    val visitCancelledDto = createVisitDto(
      visitStatus = VisitStatus.CANCELLED,
      reference = reference,
      createdTimestamp = updateDateAndTime,
      modifiedTimestamp = cancelDateAndTime,
      cancelledBy = "Cancel Boy",
    )

    visitSchedulerMockServer.stubGetVisitHistory(reference, listOf(visitBookedDto, visitUpdateDto1, visitUpdateDto2, visitCancelledDto))

    // When
    val responseSpec = callVisitHistoryByReference(webTestClient, reference, roleVisitSchedulerHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val visitHistoryDetailsDto = objectMapper.readValue(responseSpec.expectBody().returnResult().responseBody, VisitHistoryDetailsDto::class.java)
    Assertions.assertThat(visitHistoryDetailsDto.visit.reference).isEqualTo(reference)
    Assertions.assertThat(visitHistoryDetailsDto.visit.cancelledBy).isEqualTo("Cancel Boy")
    Assertions.assertThat(visitHistoryDetailsDto.createdDateAndTime).isEqualTo(createDateAndTime)
    Assertions.assertThat(visitHistoryDetailsDto.updatedDateAndTime).isEqualTo(updateDateAndTime)
    Assertions.assertThat(visitHistoryDetailsDto.cancelledDateAndTime).isEqualTo(cancelDateAndTime)
  }

  @Test
  fun `when visit has been visit has been updated then no cancel date and time is given`() {
    // Given
    val reference = "aa-bb-cc-dd"

    val visitOriginalDto = createVisitDto(visitStatus = VisitStatus.CANCELLED, outcomeStatus = OutcomeStatus.SUPERSEDED_CANCELLATION, reference = reference)
    val visitBookedDto = createVisitDto(visitStatus = VisitStatus.BOOKED, reference = reference)

    visitSchedulerMockServer.stubGetVisitHistory(reference, listOf(visitOriginalDto, visitBookedDto))

    // When
    val responseSpec = callVisitHistoryByReference(webTestClient, reference, roleVisitSchedulerHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val visitHistoryDetailsDto = objectMapper.readValue(responseSpec.expectBody().returnResult().responseBody, VisitHistoryDetailsDto::class.java)
    Assertions.assertThat(visitHistoryDetailsDto.createdDateAndTime).isNotNull
    Assertions.assertThat(visitHistoryDetailsDto.updatedDateAndTime).isNotNull
    Assertions.assertThat(visitHistoryDetailsDto.cancelledDateAndTime).isNull()
  }

  @Test
  fun `when visit has been booked then no cancel or update date and time is given`() {
    // Given
    val reference = "aa-bb-cc-dd"

    val visitOriginalDto = createVisitDto(visitStatus = VisitStatus.BOOKED, reference = reference)

    visitSchedulerMockServer.stubGetVisitHistory(reference, listOf(visitOriginalDto))

    // When
    val responseSpec = callVisitHistoryByReference(webTestClient, reference, roleVisitSchedulerHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val visitHistoryDetailsDto = objectMapper.readValue(responseSpec.expectBody().returnResult().responseBody, VisitHistoryDetailsDto::class.java)
    Assertions.assertThat(visitHistoryDetailsDto.createdDateAndTime).isNotNull
    Assertions.assertThat(visitHistoryDetailsDto.updatedDateAndTime).isNull()
    Assertions.assertThat(visitHistoryDetailsDto.cancelledDateAndTime).isNull()
  }

  @Test
  fun `when visit has been cancel then no cancel or update date and time is given`() {
    // Given
    val reference = "aa-bb-cc-dd"

    val visitOriginalDto = createVisitDto(visitStatus = VisitStatus.BOOKED, reference = reference)

    visitSchedulerMockServer.stubGetVisitHistory(reference, listOf(visitOriginalDto))

    // When
    val responseSpec = callVisitHistoryByReference(webTestClient, reference, roleVisitSchedulerHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val visitHistoryDetailsDto = objectMapper.readValue(responseSpec.expectBody().returnResult().responseBody, VisitHistoryDetailsDto::class.java)
    Assertions.assertThat(visitHistoryDetailsDto.createdDateAndTime).isNotNull
    Assertions.assertThat(visitHistoryDetailsDto.updatedDateAndTime).isNull()
    Assertions.assertThat(visitHistoryDetailsDto.cancelledDateAndTime).isNull()
  }
}
