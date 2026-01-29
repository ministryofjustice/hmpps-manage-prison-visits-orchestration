package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.manage.users.UserExtendedDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.MockUtils.Companion.createJsonResponseBuilder
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.MockUtils.Companion.getJsonString

class ManageUsersApiMockServer : WireMockServer(8097) {
  fun stubGetUserDetails(userId: String, fullName: String? = "$userId-name") {
    val responseBuilder = createJsonResponseBuilder()

    stubFor(
      get("/users/$userId")
        .willReturn(
          responseBuilder
            .withStatus(HttpStatus.OK.value())
            .withBody(
              """
              {
                 "username": "$userId",
                 "name": "$fullName"
                }
              """.trimIndent(),
            ),
        ),
    )
  }

  fun stubGetUserDetailsFailure(userId: String, status: HttpStatus = HttpStatus.NOT_FOUND) {
    val responseBuilder = createJsonResponseBuilder()

    stubFor(
      get("/users/$userId")
        .willReturn(
          responseBuilder
            .withStatus(status.value()),
        ),
    )
  }

  fun stubGetMultipleUserDetails(userIds: List<String>, userDetails: Map<String, UserExtendedDetailsDto>?, httpStatus: HttpStatus = HttpStatus.NOT_FOUND) {
    val responseBuilder = createJsonResponseBuilder()

    stubFor(
      post("/prisonusers/find-by-usernames")
        .willReturn(
          if (userDetails == null) {
            responseBuilder.withStatus(httpStatus.value())
          } else {
            responseBuilder.withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(userDetails))
          },
        ),
    )
  }
}
