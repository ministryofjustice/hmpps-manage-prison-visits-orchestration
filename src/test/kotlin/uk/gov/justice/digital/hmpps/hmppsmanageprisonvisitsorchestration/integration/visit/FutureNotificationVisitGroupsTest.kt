package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.visit

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.OrchestrationNotificationGroupDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.NotificationEventType.NON_ASSOCIATION_EVENT
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase

@DisplayName("GET /visits/notification/{prisonCode}/groups")
class FutureNotificationVisitGroupsTest : IntegrationTestBase() {

  val prisonCode = "ABC"

  @Test
  fun `when notification group is requested for all prisons`() {
    // Given
    val dtoStub = visitSchedulerMockServer.stubFutureNotificationVisitGroups(prisonCode)
    manageUsersApiMockServer.stubGetUserDetails("Username1", "Aled")
    manageUsersApiMockServer.stubGetUserDetails("Username2", "Gwyn")
    // When
    val responseSpec = callFutureNotificationVisitGroups(webTestClient, prisonCode, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val dtoArray = this.getNotificationGroupDtoDto(responseSpec)
    Assertions.assertThat(dtoArray).hasSize(1)
    with(dtoArray[0]) {
      Assertions.assertThat(reference).isEqualTo("v7*d7*ed*7u")
      Assertions.assertThat(type).isEqualTo(NON_ASSOCIATION_EVENT)
      Assertions.assertThat(affectedVisits).hasSize(2)
      with(affectedVisits[0]) {
        Assertions.assertThat(prisonerNumber).isEqualTo("AF34567G")
        Assertions.assertThat(bookedByUserName).isEqualTo("Username1")
        Assertions.assertThat(bookedByName).isEqualTo("Aled")
        Assertions.assertThat(visitDate).isEqualTo(dtoStub.affectedVisits[0].visitDate)
        Assertions.assertThat(bookingReference).isEqualTo("v1-d7-ed-7u")
        Assertions.assertThat(notificationEventAttributes.size).isEqualTo(1)
      }
      with(affectedVisits[1]) {
        Assertions.assertThat(prisonerNumber).isEqualTo("BF34567G")
        Assertions.assertThat(bookedByUserName).isEqualTo("Username2")
        Assertions.assertThat(bookedByName).isEqualTo("Gwyn")
        Assertions.assertThat(visitDate).isEqualTo(dtoStub.affectedVisits[1].visitDate)
        Assertions.assertThat(bookingReference).isEqualTo("v2-d7-ed-7u")
        Assertions.assertThat(notificationEventAttributes.size).isEqualTo(1)
      }
    }
  }

  fun getNotificationGroupDtoDto(responseSpec: ResponseSpec): Array<OrchestrationNotificationGroupDto> =
    objectMapper.readValue(responseSpec.expectBody().returnResult().responseBody, Array<OrchestrationNotificationGroupDto>::class.java)

  fun callFutureNotificationVisitGroups(
    webTestClient: WebTestClient,
    prisonCode: String,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): ResponseSpec {
    return webTestClient.get().uri("/visits/notification/$prisonCode/groups")
      .headers(authHttpHeaders)
      .exchange()
  }
}
