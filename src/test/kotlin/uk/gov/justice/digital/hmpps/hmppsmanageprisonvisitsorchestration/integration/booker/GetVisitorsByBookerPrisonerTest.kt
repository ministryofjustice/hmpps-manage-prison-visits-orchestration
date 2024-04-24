package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.booker

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonVisitBookerRegistryClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonerContactRegistryClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.BookerPrisonerVisitorsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.VisitorBasicInfoDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import java.time.LocalDate

@DisplayName("Get visitors by booker's prisoner")
class GetVisitorsByBookerPrisonerTest : IntegrationTestBase() {
  @SpyBean
  lateinit var prisonVisitBookerRegistryClientSpy: PrisonVisitBookerRegistryClient

  @SpyBean
  lateinit var prisonerContactRegistryClientSpy: PrisonerContactRegistryClient

  private val bookerReference = "booker-1"

  private final val prisonerId = "AA112233B"

  private val visitor1 = VisitorDetails(1, "First", "VisitorA", LocalDate.of(2000, 1, 31), true)
  private val visitor2 = VisitorDetails(2, "Second", "VisitorB", LocalDate.of(2021, 2, 21), true)

  // visitor 3 does not have a DOB
  private val visitor3 = VisitorDetails(3, "Third", "VisitorC", null, true)

  // visitor 4 is not approved.
  private val visitor4 = VisitorDetails(4, "Fourth", "VisitorD", LocalDate.of(1990, 4, 1), false)

  private val contactsList = createContactsList(listOf(visitor1, visitor2))

  fun callGetVisitorsByBookersPrisoner(
    webTestClient: WebTestClient,
    authHttpHeaders: (HttpHeaders) -> Unit,
    bookerReference: String,
    prisonerNumber: String,
  ): WebTestClient.ResponseSpec {
    return webTestClient.get().uri("/public/booker/$bookerReference/prisoners/$prisonerNumber/visitors")
      .headers(authHttpHeaders)
      .exchange()
  }

  @Test
  fun `when booker's prisoners has valid visitors then all allowed visitors are returned`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisonerVisitors(
      bookerReference,
      prisonerId,
      listOf(
        BookerPrisonerVisitorsDto(prisonerId, visitor1.personId),
        BookerPrisonerVisitorsDto(prisonerId, visitor2.personId),
      ),
    )

    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId, contactsList)

    // When
    val responseSpec = callGetVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, bookerReference, prisonerId)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(2)
    assertVisitorContactBasicDetails(prisonerDetailsList[0], visitor1)
    assertVisitorContactBasicDetails(prisonerDetailsList[1], visitor2)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getVisitorsForBookersAssociatedPrisoner(bookerReference, prisonerId)
    verify(prisonerContactRegistryClientSpy, times(1)).getPrisonersSocialContacts(prisonerId, false)
  }

  @Test
  fun `when booker's prisoners has no valid visitors then no visitors are returned`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisonerVisitors(
      bookerReference,
      prisonerId,
      emptyList(),
    )

    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId, contactsList)

    // When
    val responseSpec = callGetVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, bookerReference, prisonerId)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(0)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getVisitorsForBookersAssociatedPrisoner(bookerReference, prisonerId)
    verify(prisonerContactRegistryClientSpy, times(0)).getPrisonersSocialContacts(prisonerId, false)
  }

  @Test
  fun `when booker's prisoners has valid visitors but one of them has no date of birth then that visitor is not returned`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisonerVisitors(
      bookerReference,
      prisonerId,
      listOf(
        BookerPrisonerVisitorsDto(prisonerId, visitor1.personId),
        BookerPrisonerVisitorsDto(prisonerId, visitor3.personId),
      ),
    )

    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId, contactsList)

    // When
    val responseSpec = callGetVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, bookerReference, prisonerId)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(1)
    assertVisitorContactBasicDetails(prisonerDetailsList[0], visitor1)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getVisitorsForBookersAssociatedPrisoner(bookerReference, prisonerId)
    verify(prisonerContactRegistryClientSpy, times(1)).getPrisonersSocialContacts(prisonerId, false)
  }

  @Test
  fun `when booker's prisoners has valid visitors but one of them is not approved then that visitor is not returned`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisonerVisitors(
      bookerReference,
      prisonerId,
      listOf(
        BookerPrisonerVisitorsDto(prisonerId, visitor1.personId),
        BookerPrisonerVisitorsDto(prisonerId, visitor4.personId),
      ),
    )

    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId, contactsList)

    // When
    val responseSpec = callGetVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, bookerReference, prisonerId)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(1)
    assertVisitorContactBasicDetails(prisonerDetailsList[0], visitor1)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getVisitorsForBookersAssociatedPrisoner(bookerReference, prisonerId)
    verify(prisonerContactRegistryClientSpy, times(1)).getPrisonersSocialContacts(prisonerId, false)
  }

  @Test
  fun `when NOT_FOUND is returned from booker registry then NOT_FOUND status is sent back`() {
    // Given
    // prison visit booker registry returns 404
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisonerVisitors(
      bookerReference,
      prisonerId,
      null,
      HttpStatus.NOT_FOUND,
    )

    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId, contactsList)

    // When
    val responseSpec = callGetVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, bookerReference, prisonerId)

    // Then
    responseSpec.expectStatus().isNotFound

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getVisitorsForBookersAssociatedPrisoner(bookerReference, prisonerId)
    verify(prisonerContactRegistryClientSpy, times(0)).getPrisonersSocialContacts(prisonerId, false)
  }

  @Test
  fun `when INTERNAL_SERVER_ERROR is returned from booker registry then INTERNAL_SERVER_ERROR status is sent back`() {
    // Given
    // prison visit booker registry returns 404
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisonerVisitors(
      bookerReference,
      prisonerId,
      null,
      HttpStatus.INTERNAL_SERVER_ERROR,
    )

    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId, contactsList)

    // When
    val responseSpec = callGetVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, bookerReference, prisonerId)

    // Then
    responseSpec.expectStatus().is5xxServerError

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getVisitorsForBookersAssociatedPrisoner(bookerReference, prisonerId)
    verify(prisonerContactRegistryClientSpy, times(0)).getPrisonersSocialContacts(prisonerId, false)
  }

  @Test
  fun `when NOT_FOUND is returned from prisoner contact registry then empty visitor list is returned`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisonerVisitors(
      bookerReference,
      prisonerId,
      listOf(
        BookerPrisonerVisitorsDto(prisonerId, visitor1.personId),
        BookerPrisonerVisitorsDto(prisonerId, visitor2.personId),
      ),
    )

    // prisoner contact registry returns 404
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId, null, HttpStatus.NOT_FOUND)

    // When
    val responseSpec = callGetVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, bookerReference, prisonerId)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val prisonerDetailsList = getResults(returnResult)

    Assertions.assertThat(prisonerDetailsList.size).isEqualTo(0)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getVisitorsForBookersAssociatedPrisoner(bookerReference, prisonerId)
    verify(prisonerContactRegistryClientSpy, times(1)).getPrisonersSocialContacts(prisonerId, false)
  }

  @Test
  fun `when INTERNAL_SERVER_ERROR is returned from prisoner contact registry then INTERNAL_SERVER_ERROR status code is sent back`() {
    // Given
    prisonVisitBookerRegistryMockServer.stubGetBookersPrisonerVisitors(
      bookerReference,
      prisonerId,
      listOf(
        BookerPrisonerVisitorsDto(prisonerId, visitor1.personId),
        BookerPrisonerVisitorsDto(prisonerId, visitor2.personId),
      ),
    )

    // prisoner contact registry returns INTERNAL_SERVER_ERROR
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId, null, HttpStatus.INTERNAL_SERVER_ERROR)

    // When
    val responseSpec = callGetVisitorsByBookersPrisoner(webTestClient, roleVSIPOrchestrationServiceHttpHeaders, bookerReference, prisonerId)

    // Then
    responseSpec.expectStatus().is5xxServerError
    verify(prisonVisitBookerRegistryClientSpy, times(1)).getVisitorsForBookersAssociatedPrisoner(bookerReference, prisonerId)
    verify(prisonerContactRegistryClientSpy, times(1)).getPrisonersSocialContacts(prisonerId, false)
  }

  @Test
  fun `when get visitors by prisoner called without correct role then access forbidden is returned`() {
    // When
    val invalidRoleHttpHeaders = setAuthorisation(roles = listOf("ROLE_INVALID"))
    val responseSpec = callGetVisitorsByBookersPrisoner(webTestClient, invalidRoleHttpHeaders, bookerReference, prisonerId)

    // Then
    responseSpec.expectStatus().isForbidden

    // And

    verify(prisonVisitBookerRegistryClientSpy, times(0)).getVisitorsForBookersAssociatedPrisoner(bookerReference, prisonerId)
    verify(prisonerContactRegistryClientSpy, times(0)).getPrisonersSocialContacts(prisonerId, false)
  }

  @Test
  fun `when get visitors by prisoner called without token then unauthorised status is returned`() {
    // When
    val responseSpec = webTestClient.get().uri("/public/booker/$bookerReference/prisoners/$prisonerId/visitors").exchange()

    // Then
    responseSpec.expectStatus().isUnauthorized

    // And

    verify(prisonVisitBookerRegistryClientSpy, times(0)).getVisitorsForBookersAssociatedPrisoner(bookerReference, prisonerId)
    verify(prisonerContactRegistryClientSpy, times(0)).getPrisonersSocialContacts(prisonerId, false)
  }

  private fun assertVisitorContactBasicDetails(visitorBasicInfo: VisitorBasicInfoDto, visitorDetails: VisitorDetails) {
    Assertions.assertThat(visitorBasicInfo.personId).isEqualTo(visitorDetails.personId)
    Assertions.assertThat(visitorBasicInfo.firstName).isEqualTo(visitorDetails.firstName)
    Assertions.assertThat(visitorBasicInfo.lastName).isEqualTo(visitorDetails.lastName)
    Assertions.assertThat(visitorBasicInfo.dateOfBirth).isEqualTo(visitorDetails.dateOfBirth)
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): List<VisitorBasicInfoDto> {
    return objectMapper.readValue(returnResult.returnResult().responseBody, Array<VisitorBasicInfoDto>::class.java).toList()
  }
}
