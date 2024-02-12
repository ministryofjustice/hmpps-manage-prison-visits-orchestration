package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType

class ManageUsersApiMockServer : WireMockServer(8097) {
  fun stubGetUserDetails(userId: String, fullName: String? = "$userId-name") {
    val responseBuilder = aResponse()
      .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)

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
}
