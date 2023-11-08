package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.visit

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.NotificationEventType.NON_ASSOCIATION_EVENT
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.NotificationGroupDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase

@DisplayName("GET visits/notification/groups")
class FutureNotificationVisitGroupsTest : IntegrationTestBase() {

  @Test
  fun `when notification group is requested for all prisons`() {
    // Given
    visitSchedulerMockServer.stubFutureNotificationVisitGroups()

    // When
    val responseSpec = callFutureNotificationVisitGroups(webTestClient, roleVisitSchedulerHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val dtoArray = this.getNotificationGroupDtoDto(responseSpec)
    Assertions.assertThat(dtoArray).hasSize(1)
    with(dtoArray[0]) {
      Assertions.assertThat(reference).isEqualTo("v7*d7*ed*7u")
      Assertions.assertThat(type).isEqualTo(NON_ASSOCIATION_EVENT)
      Assertions.assertThat(affectedVisits).hasSize(2)
    }
  }

  fun getNotificationGroupDtoDto(responseSpec: ResponseSpec): Array<NotificationGroupDto> =
    objectMapper.readValue(responseSpec.expectBody().returnResult().responseBody, Array<NotificationGroupDto>::class.java)

  fun callFutureNotificationVisitGroups(
    webTestClient: WebTestClient,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): ResponseSpec {
    return webTestClient.get().uri("/visits/notification/groups")
      .headers(authHttpHeaders)
      .exchange()
  }
}
