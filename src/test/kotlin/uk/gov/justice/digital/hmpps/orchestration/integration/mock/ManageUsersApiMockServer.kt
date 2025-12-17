package uk.gov.justice.digital.hmpps.orchestration.integration.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.orchestration.integration.mock.MockUtils.Companion.createJsonResponseBuilder

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
}
