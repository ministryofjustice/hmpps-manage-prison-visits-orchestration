package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.prisons

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonRegisterClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitSchedulerClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.PrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.register.PrisonRegisterContactDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.register.PrisonRegisterPrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSchedulerPrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.TestObjectMapper

@DisplayName("Get prison tests")
class GetPrisonTest : IntegrationTestBase() {
  @MockitoSpyBean
  lateinit var prisonRegisterClientSpy: PrisonRegisterClient

  @MockitoSpyBean
  lateinit var visitSchedulerClientSpy: VisitSchedulerClient

  final val prisonCode = "HEI"
  val visitSchedulerPrisonDto = VisitSchedulerPrisonDto(prisonCode, true, 2, 28, 6, 3, 3, 18)
  val prisonRegisterPrisonDto = PrisonRegisterPrisonDto(prisonCode, "HMP Hewell")
  val prisonRegisterPrisonContactDetailsDto = PrisonRegisterContactDetailsDto("example@email.com", "07777777777", "https://www.example.com")

  fun callGetSupportedPrisons(
    webTestClient: WebTestClient,
    prisonCode: String,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec = webTestClient.get().uri("/config/prisons/prison/$prisonCode")
    .headers(authHttpHeaders)
    .exchange()

  @Test
  fun `when prison existS on both visit-scheduler and prison-register then prison is returned`() {
    // Given

    visitSchedulerMockServer.stubGetPrison("HEI", visitSchedulerPrisonDto = visitSchedulerPrisonDto)
    prisonRegisterMockServer.stubGetPrison(prisonCode, prisonRegisterPrisonDto)
    prisonRegisterMockServer.stubGetPrisonContactDetails(prisonCode, prisonRegisterPrisonContactDetailsDto)

    // When
    val responseSpec = callGetSupportedPrisons(webTestClient, "HEI", roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val result = getResult(returnResult)

    Assertions.assertThat(result.active).isEqualTo(visitSchedulerPrisonDto.active)
    Assertions.assertThat(result.code).isEqualTo(visitSchedulerPrisonDto.code)
    Assertions.assertThat(result.prisonName).isEqualTo(prisonRegisterPrisonDto.prisonName)
    Assertions.assertThat(result.policyNoticeDaysMin).isEqualTo(visitSchedulerPrisonDto.policyNoticeDaysMin)
    Assertions.assertThat(result.policyNoticeDaysMax).isEqualTo(visitSchedulerPrisonDto.policyNoticeDaysMax)
    Assertions.assertThat(result.maxAdultVisitors).isEqualTo(visitSchedulerPrisonDto.maxAdultVisitors)
    Assertions.assertThat(result.maxChildVisitors).isEqualTo(visitSchedulerPrisonDto.maxChildVisitors)
    Assertions.assertThat(result.maxTotalVisitors).isEqualTo(visitSchedulerPrisonDto.maxTotalVisitors)
    Assertions.assertThat(result.adultAgeYears).isEqualTo(visitSchedulerPrisonDto.adultAgeYears)

    verify(prisonRegisterClientSpy, times(1)).getPrison(prisonCode)
    verify(visitSchedulerClientSpy, times(1)).getPrison(prisonCode)
  }

  @Test
  fun `when NOT_FOUND returned from prison-register for contact details, then NO contact details are set and prison is returned`() {
    // Given
    visitSchedulerMockServer.stubGetPrison("HEI", visitSchedulerPrisonDto = visitSchedulerPrisonDto)
    prisonRegisterMockServer.stubGetPrison(prisonCode, prisonRegisterPrisonDto)
    prisonRegisterMockServer.stubGetPrisonContactDetails(prisonCode, null)

    // When
    val responseSpec = callGetSupportedPrisons(webTestClient, "HEI", roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val result = getResult(returnResult)

    Assertions.assertThat(result.active).isEqualTo(visitSchedulerPrisonDto.active)
    Assertions.assertThat(result.code).isEqualTo(visitSchedulerPrisonDto.code)
    Assertions.assertThat(result.prisonName).isEqualTo(prisonRegisterPrisonDto.prisonName)
    Assertions.assertThat(result.policyNoticeDaysMin).isEqualTo(visitSchedulerPrisonDto.policyNoticeDaysMin)
    Assertions.assertThat(result.policyNoticeDaysMax).isEqualTo(visitSchedulerPrisonDto.policyNoticeDaysMax)
    Assertions.assertThat(result.maxAdultVisitors).isEqualTo(visitSchedulerPrisonDto.maxAdultVisitors)
    Assertions.assertThat(result.maxChildVisitors).isEqualTo(visitSchedulerPrisonDto.maxChildVisitors)
    Assertions.assertThat(result.maxTotalVisitors).isEqualTo(visitSchedulerPrisonDto.maxTotalVisitors)
    Assertions.assertThat(result.adultAgeYears).isEqualTo(visitSchedulerPrisonDto.adultAgeYears)
    Assertions.assertThat(result.emailAddress).isNull()
    Assertions.assertThat(result.phoneNumber).isNull()
    Assertions.assertThat(result.webAddress).isNull()

    verify(prisonRegisterClientSpy, times(1)).getPrison(prisonCode)
    verify(visitSchedulerClientSpy, times(1)).getPrison(prisonCode)
  }

  @Test
  fun `when NOT_FOUND is returned from visit scheduler then NOT_FOUND status is sent back`() {
    // Given
    visitSchedulerMockServer.stubGetPrison("HEI", null)
    prisonRegisterMockServer.stubGetPrison(prisonCode, prisonRegisterPrisonDto)

    // When
    val responseSpec = callGetSupportedPrisons(webTestClient, "HEI", roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
    verify(prisonRegisterClientSpy, times(0)).getPrison(prisonCode)
    verify(visitSchedulerClientSpy, times(1)).getPrison(prisonCode)
  }

  @Test
  fun `when NOT_FOUND is returned from prison register then NOT_FOUND status is sent back`() {
    // Given
    visitSchedulerMockServer.stubGetPrison("HEI", visitSchedulerPrisonDto)
    prisonRegisterMockServer.stubGetPrison(prisonCode, null)

    // When
    val responseSpec = callGetSupportedPrisons(webTestClient, "HEI", roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
    verify(prisonRegisterClientSpy, times(1)).getPrison(prisonCode)
    verify(visitSchedulerClientSpy, times(1)).getPrison(prisonCode)
  }

  @Test
  fun `when BAD_REQUEST is returned from visit scheduler then BAD_REQUEST status is sent back`() {
    // Given
    visitSchedulerMockServer.stubGetPrison("HEI", null, HttpStatus.BAD_REQUEST)
    prisonRegisterMockServer.stubGetPrison(prisonCode, prisonRegisterPrisonDto)

    // When
    val responseSpec = callGetSupportedPrisons(webTestClient, "HEI", roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isBadRequest
    verify(prisonRegisterClientSpy, times(0)).getPrison(prisonCode)
    verify(visitSchedulerClientSpy, times(1)).getPrison(prisonCode)
  }

  @Test
  fun `when BAD_REQUEST is returned from prison register then BAD_REQUEST status is sent back`() {
    // Given
    visitSchedulerMockServer.stubGetPrison("HEI", visitSchedulerPrisonDto)
    prisonRegisterMockServer.stubGetPrison(prisonCode, null, HttpStatus.BAD_REQUEST)

    // When
    val responseSpec = callGetSupportedPrisons(webTestClient, "HEI", roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isBadRequest
    verify(prisonRegisterClientSpy, times(1)).getPrison(prisonCode)
    verify(visitSchedulerClientSpy, times(1)).getPrison(prisonCode)
  }

  private fun getResult(returnResult: WebTestClient.BodyContentSpec): PrisonDto = TestObjectMapper.mapper.readValue(returnResult.returnResult().responseBody, PrisonDto::class.java)
}
