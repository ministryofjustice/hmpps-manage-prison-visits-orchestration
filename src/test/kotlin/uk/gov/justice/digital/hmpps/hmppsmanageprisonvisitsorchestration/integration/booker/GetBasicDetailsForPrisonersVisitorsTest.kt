package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.booker

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonerContactRegistryClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.VisitorBasicInfoDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase

@DisplayName("Get Basic Contact Profile for a list of visitors")
class GetBasicDetailsForPrisonersVisitorsTest : IntegrationTestBase() {
  private final val prisonerId = "AA112233B"

  private val visitor1 = VisitorDetails(1, "First", "VisitorA")
  private val visitor2 = VisitorDetails(2, "Second", "VisitorB")
  private val visitor3 = VisitorDetails(3, "Third", "VisitorC")

  private val contactsDto = createContactsList(listOf(visitor1, visitor2))

  @SpyBean
  lateinit var prisonerContactRegistryClientSpy: PrisonerContactRegistryClient

  fun callGetVisitorsContactProfile(
    webTestClient: WebTestClient,
    authHttpHeaders: (HttpHeaders) -> Unit,
    prisonerId: String,
    visitorIds: List<Long>,
  ): WebTestClient.ResponseSpec {
    return webTestClient.get().uri("/prisoner/$prisonerId/visitors/${visitorIds.joinToString(",")}/basic-details")
      .headers(authHttpHeaders)
      .exchange()
  }

  @Test
  fun `when prisoner has single visitor then visitors basic profile is returned`() {
    // Given
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId, contactsDto)

    // When
    val responseSpec = callGetVisitorsContactProfile(webTestClient, rolePublicVisitsBookingHttpHeaders, prisonerId, listOf(visitor1.personId))

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val contactsList = getResults(returnResult)

    Assertions.assertThat(contactsList.size).isEqualTo(1)
    assertVisitorContactBasicDetails(contactsList[0], visitor1)

    verify(prisonerContactRegistryClientSpy, times(1)).getPrisonersSocialContacts(prisonerId, false)
  }

  @Test
  fun `when prisoner has multiple visitors then visitors basic profile is returned`() {
    // Given
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId, contactsDto)

    // When
    val responseSpec = callGetVisitorsContactProfile(webTestClient, rolePublicVisitsBookingHttpHeaders, prisonerId, listOf(visitor1.personId, visitor2.personId))

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val contactsList = getResults(returnResult)

    Assertions.assertThat(contactsList.size).isEqualTo(2)
    assertVisitorContactBasicDetails(contactsList[0], visitor1)
    assertVisitorContactBasicDetails(contactsList[1], visitor2)

    verify(prisonerContactRegistryClientSpy, times(1)).getPrisonersSocialContacts(prisonerId, false)
  }

  @Test
  fun `when multiple valid visitor ids passed but one of them throws NOT_FOUND then blank profile is returned`() {
    // Given
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId, contactsDto)

    // When
    val responseSpec = callGetVisitorsContactProfile(webTestClient, rolePublicVisitsBookingHttpHeaders, prisonerId, listOf(visitor1.personId, visitor2.personId, visitor3.personId))

// Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val contactsList = getResults(returnResult)

    Assertions.assertThat(contactsList.size).isEqualTo(2)
    assertVisitorContactBasicDetails(contactsList[0], visitor1)
    assertVisitorContactBasicDetails(contactsList[1], visitor2)

    verify(prisonerContactRegistryClientSpy, times(1)).getPrisonersSocialContacts(prisonerId, false)
  }

  @Test
  fun `when get basic visitor contact details called without correct role then FORBIDDEN status is returned`() {
    // When
    val responseSpec = callGetVisitorsContactProfile(webTestClient, setAuthorisation(roles = listOf()), prisonerId, listOf(visitor1.personId))

    // Then
    responseSpec.expectStatus().isForbidden

    // And
    verify(prisonerContactRegistryClientSpy, times(0)).getPrisonersSocialContacts(prisonerId, false)
  }

  @Test
  fun `when get basic visitor contact details called without token then UNAUTHORIZED status is returned`() {
    // When
    val responseSpec = webTestClient.put().uri("/prisoner/AA123/visitors/1")
      .exchange()

    // Then
    responseSpec.expectStatus().isUnauthorized

    // And
    verify(prisonerContactRegistryClientSpy, times(0)).getPrisonersSocialContacts(prisonerId, false)
  }

  private fun assertVisitorContactBasicDetails(visitorBasicInfo: VisitorBasicInfoDto, visitorDetails: VisitorDetails) {
    Assertions.assertThat(visitorBasicInfo.personId).isEqualTo(visitorDetails.personId)
    Assertions.assertThat(visitorBasicInfo.firstName).isEqualTo(visitorDetails.firstName)
    Assertions.assertThat(visitorBasicInfo.lastName).isEqualTo(visitorDetails.lastName)
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): List<VisitorBasicInfoDto> {
    return objectMapper.readValue(returnResult.returnResult().responseBody, Array<VisitorBasicInfoDto>::class.java).toList()
  }
}
