package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.visit

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.times
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonerContactRegistryClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.PrisonerContactDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.OrchestrationVisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitStatus.CANCELLED
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("Get public cancelled visits by booker reference")
class PublicCancelledVisitsByBookerReferenceTest : IntegrationTestBase() {
  @SpyBean
  private lateinit var prisonerContactRegistryClient: PrisonerContactRegistryClient

  private lateinit var visitDto: VisitDto
  private lateinit var visitDto2: VisitDto
  private lateinit var contact1: PrisonerContactDto
  private lateinit var contact2: PrisonerContactDto
  private lateinit var contact3: PrisonerContactDto
  private lateinit var contacts: List<PrisonerContactDto>
  private final val prisonerId = "ABC"

  @BeforeEach
  fun setup() {
    contact1 = createContactDto(firstName = "First", lastName = "Smith", dateOfBirth = LocalDate.of(2000, 1, 1))
    contact2 = createContactDto(firstName = "Second", lastName = "Smith", dateOfBirth = LocalDate.of(2000, 1, 1))
    contact3 = createContactDto(firstName = "Third", lastName = "Smith", dateOfBirth = LocalDate.of(2000, 1, 1))
    contacts = listOf(contact1, contact2, contact3)

    visitDto = createVisitDto(
      prisonerId = prisonerId,
      reference = "ss-bb",
      startTimestamp = LocalDateTime.now().plusDays(1),
      endTimestamp = LocalDateTime.now().plusDays(1),
      visitStatus = CANCELLED,
      visitors = listOf(createVisitorDto(contact1), createVisitorDto(contact2)),
    )
    visitDto2 = createVisitDto(
      prisonerId = prisonerId,
      reference = "xx-bb",
      startTimestamp = LocalDateTime.now().plusDays(2),
      endTimestamp = LocalDateTime.now().plusDays(2),
      visitStatus = CANCELLED,
      visitors = listOf(createVisitorDto(contact2), createVisitorDto(contact3)),
    )
  }

  @Test
  fun `when cancelled visits for booker exists then get cancelled visits returns content`() {
    // Given
    val visitsList = mutableListOf(visitDto, visitDto2)
    visitSchedulerMockServer.stubPublicCancelledVisitsByBookerReference(prisonerId, visitsList)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId = prisonerId, withAddress = false, approvedVisitorsOnly = false, contactsList = listOf(contact1, contact2, contact3))

    // When
    val responseSpec = callPublicCancelledVisits(webTestClient, prisonerId, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk
      .expectBody()

    val visits = getResults(returnResult)
    Assertions.assertThat(visits.size).isEqualTo(2)
  }

  @Test
  fun `when cancelled visits for booker exists then the visitors names are populated`() {
    // Given
    val visitsList = mutableListOf(visitDto, visitDto2)
    visitSchedulerMockServer.stubPublicCancelledVisitsByBookerReference(prisonerId, visitsList)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId = prisonerId, withAddress = false, approvedVisitorsOnly = false, contactsList = listOf(contact1, contact2, contact3))
    // When
    val responseSpec = callPublicCancelledVisits(webTestClient, prisonerId, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk
      .expectBody()

    val visits = getResults(returnResult)
    Assertions.assertThat(visits.size).isEqualTo(2)
    Assertions.assertThat(visits[0].visitors.size).isEqualTo(2)
    assertVisitorDetails(visits[0].visitors, contacts)
    assertVisitorDetails(visits[1].visitors, contacts)
    Mockito.verify(prisonerContactRegistryClient, times(1)).getPrisonersSocialContacts(prisonerId, withAddress = false, false, null, null, null)
  }

  @Test
  fun `when cancelled visits for booker do not exists then get cancelled visits returns empty content`() {
    // Given
    val prisonerId = "AABBCC"
    val emptyList = mutableListOf<VisitDto>()
    visitSchedulerMockServer.stubPublicCancelledVisitsByBookerReference(prisonerId, emptyList)

    // When
    val responseSpec = callPublicCancelledVisits(webTestClient, prisonerId, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk
      .expectBody()
    val visits = getResults(returnResult)
    Assertions.assertThat(visits.size).isEqualTo(0)
  }

  @Test
  fun `when cancelled visits for booker exists but prisoner contact registry returns 404 visits are returned but visitors names are not populated`() {
    // Given
    val visitsList = mutableListOf(visitDto, visitDto2)
    visitSchedulerMockServer.stubPublicCancelledVisitsByBookerReference(prisonerId, visitsList)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId = prisonerId, withAddress = false, approvedVisitorsOnly = false, contactsList = null)
    // When
    val responseSpec = callPublicCancelledVisits(webTestClient, prisonerId, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk
      .expectBody()

    val visits = getResults(returnResult)
    Assertions.assertThat(visits.size).isEqualTo(2)
    Assertions.assertThat(visits[0].visitors.size).isEqualTo(2)
    Assertions.assertThat(visits[1].visitors.size).isEqualTo(2)

    val allVisitors = visits.flatMap { it.visitors }
    for (visitor in allVisitors) {
      Assertions.assertThat(visitor.nomisPersonId).isNotNull()
      Assertions.assertThat(visitor.firstName).isNull()
      Assertions.assertThat(visitor.lastName).isNull()
    }

    Mockito.verify(prisonerContactRegistryClient, times(1)).getPrisonersSocialContacts(prisonerId, withAddress = false, approvedVisitorsOnly = false, null, null, null)
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): Array<OrchestrationVisitDto> {
    return objectMapper.readValue(returnResult.returnResult().responseBody, Array<OrchestrationVisitDto>::class.java)
  }
}
