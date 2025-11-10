package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.booker

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonVisitBookerRegistryClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonerContactRegistryClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller.PUBLIC_BOOKER_DETAILS
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.PermittedPrisonerForBookerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.PermittedVisitorForPermittedPrisonerBookerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.admin.BookerDetailedInfoDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.admin.BookerInfoDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.register.PrisonRegisterPrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("Get booker via booker reference")
class GetBookerByReferenceTest : IntegrationTestBase() {
  @MockitoSpyBean
  lateinit var prisonVisitBookerRegistryClientSpy: PrisonVisitBookerRegistryClient

  @MockitoSpyBean
  lateinit var prisonerContactRegistryClient: PrisonerContactRegistryClient

  private final val prisoner1Id = "AA123456"
  private final val prisoner2Id = "BB987654"
  private final val prisonCode = "HEI"

  private final val prisoner1Dto = createPrisoner(
    prisonerId = prisoner1Id,
    firstName = "FirstName",
    lastName = "LastName",
    dateOfBirth = LocalDate.of(2000, 1, 31),
    convictedStatus = "Convicted",
  )

  private final val prisoner2Dto = createPrisoner(
    prisonerId = prisoner2Id,
    firstName = "First",
    lastName = "Last",
    dateOfBirth = LocalDate.of(2001, 12, 1),
    convictedStatus = "Convicted",
  )

  private final val permittedPrisonerA = PermittedPrisonerForBookerDto(prisoner1Id, true, "HEI", listOf(PermittedVisitorForPermittedPrisonerBookerDto(1L, true)))

  private final val permittedPrisonerB = PermittedPrisonerForBookerDto(prisoner2Id, true, "HEI", listOf(PermittedVisitorForPermittedPrisonerBookerDto(2L, true)))

  private final val prisonDto = PrisonRegisterPrisonDto(prisonCode, "Hewell")

  @Test
  fun `when get booker by reference is called, then a full booker detailed DTO is returned with prisoner and visitor info`() {
    // Given
    val bookerReference = "abc-def-ghi"

    val prisonerContact1 = createContactDto(1, "First", "VisitorA")
    val prisonerContact2 = createContactDto(2, "Second", "VisitorB")
    val prisonerContact3 = createContactDto(3, "Random", "Visitor")

    val booker = BookerInfoDto(
      reference = bookerReference,
      email = "test@test.com",
      createdTimestamp = LocalDateTime.now().minusMonths(1),
      permittedPrisoners = listOf(permittedPrisonerA, permittedPrisonerB),
    )

    prisonVisitBookerRegistryMockServer.stubGetBookerByBookerReference(bookerReference, booker)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId = permittedPrisonerA.prisonerId, contactsList = listOf(prisonerContact1, prisonerContact3))
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId = permittedPrisonerB.prisonerId, contactsList = listOf(prisonerContact2, prisonerContact3))
    prisonOffenderSearchMockServer.stubGetPrisonerById(permittedPrisonerA.prisonerId, prisoner1Dto)
    prisonOffenderSearchMockServer.stubGetPrisonerById(permittedPrisonerB.prisonerId, prisoner2Dto)
    prisonRegisterMockServer.stubGetPrison(prisonCode, prisonDto)

    // When
    val responseSpec = callGetBookerByBookerReference(bookerReference, webTestClient, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val bookerDetailedInfo = getResults(returnResult)

    Assertions.assertThat(bookerDetailedInfo.reference).isEqualTo(bookerReference)
    Assertions.assertThat(bookerDetailedInfo.permittedPrisoners.size).isEqualTo(2)
    Assertions.assertThat(bookerDetailedInfo.permittedPrisoners.first { it.prisoner.prisonerNumber == prisoner1Id }.permittedVisitors.size).isEqualTo(1)
    Assertions.assertThat(bookerDetailedInfo.permittedPrisoners.first { it.prisoner.prisonerNumber == prisoner2Id }.permittedVisitors.size).isEqualTo(1)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getBookerByBookerReference(bookerReference)
    verify(prisonerContactRegistryClient, times(1)).getPrisonersSocialContacts(prisoner1Id, false)
    verify(prisonerContactRegistryClient, times(1)).getPrisonersSocialContacts(prisoner2Id, false)
  }

  @Test
  fun `when get booker by reference is called, but call for one prisoners visitors fails, the rest still process and booker details are returned partially complete`() {
    // Given
    val bookerReference = "abc-def-ghi"

    val prisonerContact1 = createContactDto(1, "First", "VisitorA")
    val prisonerContact3 = createContactDto(3, "Random", "Visitor")

    val booker = BookerInfoDto(
      reference = bookerReference,
      email = "test@test.com",
      createdTimestamp = LocalDateTime.now().minusMonths(1),
      permittedPrisoners = listOf(permittedPrisonerA, permittedPrisonerB),
    )

    prisonVisitBookerRegistryMockServer.stubGetBookerByBookerReference(bookerReference, booker)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId = permittedPrisonerA.prisonerId, contactsList = listOf(prisonerContact1, prisonerContact3))
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId = permittedPrisonerB.prisonerId, contactsList = null, httpStatus = HttpStatus.NOT_FOUND)
    prisonOffenderSearchMockServer.stubGetPrisonerById(permittedPrisonerA.prisonerId, prisoner1Dto)
    prisonOffenderSearchMockServer.stubGetPrisonerById(permittedPrisonerB.prisonerId, prisoner2Dto)
    prisonRegisterMockServer.stubGetPrison(prisonCode, prisonDto)

    // When
    val responseSpec = callGetBookerByBookerReference(bookerReference, webTestClient, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val bookerDetailedInfo = getResults(returnResult)

    Assertions.assertThat(bookerDetailedInfo.reference).isEqualTo(bookerReference)
    Assertions.assertThat(bookerDetailedInfo.permittedPrisoners.size).isEqualTo(2)
    Assertions.assertThat(bookerDetailedInfo.permittedPrisoners.first { it.prisoner.prisonerNumber == prisoner1Id }.permittedVisitors.size).isEqualTo(1)
    Assertions.assertThat(bookerDetailedInfo.permittedPrisoners.first { it.prisoner.prisonerNumber == prisoner2Id }.permittedVisitors.size).isEqualTo(0)

    verify(prisonVisitBookerRegistryClientSpy, times(1)).getBookerByBookerReference(bookerReference)
    verify(prisonerContactRegistryClient, times(1)).getPrisonersSocialContacts(prisoner1Id, false)
    verify(prisonerContactRegistryClient, times(1)).getPrisonersSocialContacts(prisoner2Id, false)
  }

  @Test
  fun `when get booker by reference is called, but call to get prisoner details fails via prisoner search, then request fails with 404`() {
    // Given
    val bookerReference = "abc-def-ghi"

    val prisonerContact1 = createContactDto(1, "First", "VisitorA")
    val prisonerContact2 = createContactDto(2, "Second", "VisitorB")
    val prisonerContact3 = createContactDto(3, "Random", "Visitor")

    val booker = BookerInfoDto(
      reference = bookerReference,
      email = "test@test.com",
      createdTimestamp = LocalDateTime.now().minusMonths(1),
      permittedPrisoners = listOf(permittedPrisonerA, permittedPrisonerB),
    )

    prisonVisitBookerRegistryMockServer.stubGetBookerByBookerReference(bookerReference, booker)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId = permittedPrisonerA.prisonerId, contactsList = listOf(prisonerContact1, prisonerContact3))
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId = permittedPrisonerB.prisonerId, contactsList = listOf(prisonerContact2, prisonerContact3))
    prisonOffenderSearchMockServer.stubGetPrisonerById(permittedPrisonerA.prisonerId, null, httpStatus = HttpStatus.NOT_FOUND)
    prisonOffenderSearchMockServer.stubGetPrisonerById(permittedPrisonerB.prisonerId, prisoner2Dto)
    prisonRegisterMockServer.stubGetPrison(prisonCode, prisonDto)

    // When
    val responseSpec = callGetBookerByBookerReference(bookerReference, webTestClient, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
  }

  @Test
  fun `when booker registry returns a internal server error, then internal server error is thrown upwards to caller`() {
    // Given
    val bookerReference = "abc-def-ghi"

    prisonVisitBookerRegistryMockServer.stubGetBookerByBookerReference(bookerReference, null, HttpStatus.INTERNAL_SERVER_ERROR)

    // When
    val responseSpec = callGetBookerByBookerReference(bookerReference, webTestClient, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().is5xxServerError
  }

  @Test
  fun `when endpoint is called without token then UNAUTHORIZED status is returned`() {
    // Given
    val bookerReference = "abc-def-ghi"

    // When
    val responseSpec = webTestClient.get().uri(PUBLIC_BOOKER_DETAILS.replace("{bookerReference}", bookerReference))
      .exchange()

    // Then
    responseSpec.expectStatus().isUnauthorized
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): BookerDetailedInfoDto = objectMapper.readValue(returnResult.returnResult().responseBody, BookerDetailedInfoDto::class.java)

  fun callGetBookerByBookerReference(
    bookerReference: String,
    webTestClient: WebTestClient,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec = webTestClient.get().uri(PUBLIC_BOOKER_DETAILS.replace("{bookerReference}", bookerReference))
    .headers(authHttpHeaders)
    .exchange()
}
