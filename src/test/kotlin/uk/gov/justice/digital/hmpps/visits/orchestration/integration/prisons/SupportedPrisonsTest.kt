package uk.gov.justice.digital.hmpps.visits.orchestration.integration.prisons

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
import uk.gov.justice.digital.hmpps.visits.orchestration.client.PrisonRegisterClient
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.prison.register.PrisonRegisterPrisonDto
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.enums.UserType
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.enums.UserType.PUBLIC
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.enums.UserType.STAFF
import uk.gov.justice.digital.hmpps.visits.orchestration.integration.IntegrationTestBase

@DisplayName("Get supported prisons")
class SupportedPrisonsTest : IntegrationTestBase() {
  @MockitoSpyBean
  private lateinit var prisonRegisterClientSpy: PrisonRegisterClient

  fun callGetSupportedPrisons(
    type: UserType,
    webTestClient: WebTestClient,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec = webTestClient.get().uri("/config/prisons/user-type/${type.name}/supported")
    .headers(authHttpHeaders)
    .exchange()

  fun callGetSupportedPrisonsDetails(
    type: UserType,
    webTestClient: WebTestClient,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec = webTestClient.get().uri("/config/prisons/user-type/${type.name}/supported/detailed")
    .headers(authHttpHeaders)
    .exchange()

  val prison1Dto = PrisonRegisterPrisonDto("BLI", "BLI Prison")
  val prison2Dto = PrisonRegisterPrisonDto("HEI", "HEI Prison")

  @Test
  fun `when active prisons exist then all active prisons are returned`() {
    // Given
    val prisons = arrayOf("BLI", "HEI")
    visitSchedulerMockServer.stubGetSupportedPrisons(STAFF, prisons.toMutableList())

    // When
    val responseSpec = callGetSupportedPrisons(STAFF, webTestClient, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val results = getResults(returnResult)

    Assertions.assertThat(results.size).isEqualTo(2)
    Assertions.assertThat(results).containsExactlyInAnyOrder(*prisons)
  }

  @Test
  fun `when active prisons do not exist then empty list is returned`() {
    // Given
    visitSchedulerMockServer.stubGetSupportedPrisons(PUBLIC, mutableListOf())

    // When
    val responseSpec = callGetSupportedPrisons(PUBLIC, webTestClient, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(0)
  }

  @Test
  fun `when get detailed view of supported prisons called and active prisons exist then all active prisons details are returned`() {
    // Given
    val prisons = listOf("BLI", "HEI")

    visitSchedulerMockServer.stubGetSupportedPrisons(STAFF, prisons.toMutableList())
    prisonRegisterMockServer.stubPrisonsByIds(listOf(prison1Dto, prison2Dto))

    // When
    val responseSpec = callGetSupportedPrisonsDetails(STAFF, webTestClient, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val results = getPrisonDetailsResults(returnResult)

    Assertions.assertThat(results.size).isEqualTo(2)
    Assertions.assertThat(results).contains(prison1Dto)
    Assertions.assertThat(results).contains(prison2Dto)
    verify(prisonRegisterClientSpy, times(1)).prisonsByIds(prisons)
  }

  @Test
  fun `when get detailed view of supported prisons called and prisons do not exist in prison register then empty list is returned`() {
    // Given
    val prisons = emptyList<String>()

    visitSchedulerMockServer.stubGetSupportedPrisons(STAFF, prisons.toMutableList())
    prisonRegisterMockServer.stubPrisonsByIds(emptyList())

    // When
    val responseSpec = callGetSupportedPrisonsDetails(STAFF, webTestClient, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(0)
    verify(prisonRegisterClientSpy, times(0)).prisonsByIds(any())
  }

  @Test
  fun `when get detailed view of supported prisons called and prison register returns only 1 prison then that prison is not returned`() {
    // Given
    val prisons = listOf("BLI", "HEI")

    visitSchedulerMockServer.stubGetSupportedPrisons(STAFF, prisons.toMutableList())
    prisonRegisterMockServer.stubPrisonsByIds(listOf(prison1Dto))

    // When
    val responseSpec = callGetSupportedPrisonsDetails(STAFF, webTestClient, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(1)

    val results = getPrisonDetailsResults(returnResult)
    Assertions.assertThat(results.size).isEqualTo(1)
    Assertions.assertThat(results).contains(prison1Dto)
    verify(prisonRegisterClientSpy, times(1)).prisonsByIds(prisons)
  }

  @Test
  fun `when get detailed view of supported prisons called and active prisons exist then all active prisons details are returned sorted by prison name`() {
    // Given
    val prisons = listOf("BLI", "HEI")
    val prisonZDto = PrisonRegisterPrisonDto("BLI", "Z Prison")
    val prisonBDto = PrisonRegisterPrisonDto("BLI", "B Prison")
    val prisonHDto = PrisonRegisterPrisonDto("HEI", "H Prison")
    val prisonGDto = PrisonRegisterPrisonDto("HEI", "G Prison")

    // lower case prison names
    val prisonADto = PrisonRegisterPrisonDto("HEI", "a Prison")
    val prisonCDto = PrisonRegisterPrisonDto("HEI", "c Prison")

    visitSchedulerMockServer.stubGetSupportedPrisons(STAFF, prisons.toMutableList())
    prisonRegisterMockServer.stubPrisonsByIds(listOf(prisonZDto, prisonBDto, prisonHDto, prisonGDto, prisonADto, prisonCDto))

    // When
    val responseSpec = callGetSupportedPrisonsDetails(STAFF, webTestClient, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val results = getPrisonDetailsResults(returnResult)

    Assertions.assertThat(results.size).isEqualTo(6)
    Assertions.assertThat(results).contains(prisonADto)
    Assertions.assertThat(results).contains(prisonBDto)
    Assertions.assertThat(results).contains(prisonCDto)
    Assertions.assertThat(results).contains(prisonGDto)
    Assertions.assertThat(results).contains(prisonHDto)
    Assertions.assertThat(results).contains(prisonZDto)
    verify(prisonRegisterClientSpy, times(1)).prisonsByIds(prisons)
  }

  @Test
  fun `when get detailed view of supported prisons called and prison register returns NOT_FOUND then NOT_FOUND is returned`() {
    // Given
    val prisons = listOf("BLI", "HEI")
    prisonRegisterMockServer.stubPrisonsByIds(listOf(prison1Dto, prison1Dto))

    visitSchedulerMockServer.stubGetSupportedPrisons(STAFF, prisons.toMutableList())
    prisonRegisterMockServer.stubPrisonsByIds(null, HttpStatus.NOT_FOUND)

    // When
    val responseSpec = callGetSupportedPrisonsDetails(STAFF, webTestClient, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
    verify(prisonRegisterClientSpy, times(1)).prisonsByIds(prisons)
  }

  @Test
  fun `when get detailed view of supported prisons called and prison register returns a INTERNAL_SERVER_ERROR then empty list is returned`() {
    // Given
    val prisons = listOf("BLI", "HEI")
    prisonRegisterMockServer.stubPrisonsByIds(listOf(prison1Dto, prison1Dto))

    visitSchedulerMockServer.stubGetSupportedPrisons(STAFF, prisons.toMutableList())
    prisonRegisterMockServer.stubPrisonsByIds(null, HttpStatus.INTERNAL_SERVER_ERROR)

    // When
    val responseSpec = callGetSupportedPrisonsDetails(STAFF, webTestClient, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().is5xxServerError
  }

  @Test
  fun `when get detailed view of supported prisons called and visit scheduler returns a NOT_FOUND then NOT_FOUND is returned`() {
    // Given
    val prisons = listOf("BLI", "HEI")

    visitSchedulerMockServer.stubGetSupportedPrisons(STAFF, null, HttpStatus.NOT_FOUND)
    prisonRegisterMockServer.stubPrisonsByIds(emptyList())

    // When
    val responseSpec = callGetSupportedPrisonsDetails(STAFF, webTestClient, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
    verify(prisonRegisterClientSpy, times(0)).prisonsByIds(prisons)
  }

  @Test
  fun `when get detailed view of supported prisons called and visit scheduler returns an INTERNAL_SERVER_ERROR then empty list is returned`() {
    // Given
    visitSchedulerMockServer.stubGetSupportedPrisons(STAFF, null, HttpStatus.INTERNAL_SERVER_ERROR)
    prisonRegisterMockServer.stubPrisonsByIds(emptyList())

    // When
    val responseSpec = callGetSupportedPrisonsDetails(STAFF, webTestClient, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().is5xxServerError
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): Array<String> = objectMapper.readValue(returnResult.returnResult().responseBody, Array<String>::class.java)

  private fun getPrisonDetailsResults(returnResult: WebTestClient.BodyContentSpec): Array<PrisonRegisterPrisonDto> = objectMapper.readValue(returnResult.returnResult().responseBody, Array<PrisonRegisterPrisonDto>::class.java)
}
