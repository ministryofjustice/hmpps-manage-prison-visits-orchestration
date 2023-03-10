package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.http.HttpHeader
import com.github.tomakehurst.wiremock.http.HttpHeaders
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class HmppsAuthExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {

  companion object {
    @JvmField
    val hmppsAuthApi = HmppsAuthMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    hmppsAuthApi.start()
    hmppsAuthApi.stubGrantToken()
    hmppsAuthApi.stubGetUserDetails()
  }

  override fun beforeEach(context: ExtensionContext) {
    hmppsAuthApi.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    hmppsAuthApi.stop()
  }
}

class HmppsAuthMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 8090
  }

  fun stubGrantToken() {
    stubFor(
      post(WireMock.urlEqualTo("/auth/oauth/token"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              """
              {
                "token_type": "bearer",
                "access_token": "atoken"
              }
              """.trimIndent(),
            ),
        ),
    )
  }
  fun stubGetUserDetails() {
    stubFor(
      get(WireMock.urlEqualTo("/auth/api/user/me"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              """
              {
                "activeCaseLoadId": "LEI"
              }
              """.trimIndent(),
            ),
        ),
    )
  }

  // If the user is not known then this endpoint still returns the username, just not the active caseload
  fun stubFailToGetUserDetails() {
    stubFor(
      get(WireMock.urlEqualTo("/auth/api/user/me"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              """
              {
                "username": "you"
              }
              """.trimIndent(),
            ),
        ),
    )
  }

  fun stubHealthPing(status: Int) {
    stubFor(
      get("/auth/health/ping").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(if (status == 200) "pong" else "some error")
          .withStatus(status),
      ),
    )
  }
}
