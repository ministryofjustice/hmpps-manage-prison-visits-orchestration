package uk.gov.justice.digital.hmpps.visits.orchestration.integration.visit

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.times
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.visits.orchestration.client.PrisonerContactRegistryClient
import uk.gov.justice.digital.hmpps.visits.orchestration.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.contact.registry.PrisonerContactDto
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.orchestration.OrchestrationVisitDto
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.prisoner.search.PrisonerDto
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.visits.orchestration.integration.IntegrationTestBase
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("Get public past booked visits by booker reference")
class PublicPastVisitsByBookerReferenceTest : IntegrationTestBase() {
  @MockitoSpyBean
  private lateinit var prisonerContactRegistryClient: PrisonerContactRegistryClient

  @MockitoSpyBean
  private lateinit var prisonerSearchClient: PrisonerSearchClient

  private lateinit var visitDto: VisitDto
  private lateinit var visitDto2: VisitDto
  private lateinit var prisoner: PrisonerDto
  private lateinit var contact1: PrisonerContactDto
  private lateinit var contact2: PrisonerContactDto
  private lateinit var contact3: PrisonerContactDto
  private lateinit var contacts: List<PrisonerContactDto>
  private final val prisonerId = "ABC"
  private final val prisonId = "MDI"

  @BeforeEach
  fun setup() {
    prisoner = createPrisoner(prisonerId, "james", "smith", LocalDate.of(1965, 12, 12), prisonId, convictedStatus = "Convicted")

    contact1 = createContactDto(firstName = "First", lastName = "Smith", dateOfBirth = LocalDate.of(2000, 1, 1))
    contact2 = createContactDto(firstName = "Second", lastName = "Smith", dateOfBirth = LocalDate.of(2000, 1, 1))
    contact3 = createContactDto(firstName = "Third", lastName = "Smith", dateOfBirth = LocalDate.of(2000, 1, 1))
    contacts = listOf(contact1, contact2, contact3)

    visitDto = createVisitDto(
      prisonerId = prisonerId,
      reference = "ss-bb",
      startTimestamp = LocalDateTime.now().minusDays(1),
      endTimestamp = LocalDateTime.now().minusDays(1),
      visitors = listOf(createVisitorDto(contact1), createVisitorDto(contact2)),
    )
    visitDto2 = createVisitDto(
      prisonerId = prisonerId,
      reference = "xx-bb",
      startTimestamp = LocalDateTime.now().minusDays(2),
      endTimestamp = LocalDateTime.now().minusDays(2),
      visitors = listOf(createVisitorDto(contact2), createVisitorDto(contact3)),
    )
  }

  @Test
  fun `when past visits for booker exists then get past visits returns content`() {
    // Given
    val visitsList = mutableListOf(visitDto, visitDto2)
    visitSchedulerMockServer.stubPublicPastVisitsByBookerReference(prisonerId, visitsList)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisoner)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId = prisonerId, withAddress = false, contactsList = listOf(contact1, contact2, contact3))

    // When
    val responseSpec = callPublicPastVisits(webTestClient, prisonerId, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk
      .expectBody()

    val visits = getResults(returnResult)
    Assertions.assertThat(visits.size).isEqualTo(2)
  }

  @Test
  fun `when past visits for booker exists then the visitors names and prisoner info are also populated`() {
    // Given
    val visitsList = mutableListOf(visitDto, visitDto2)
    visitSchedulerMockServer.stubPublicPastVisitsByBookerReference(prisonerId, visitsList)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisoner)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId = prisonerId, withAddress = false, contactsList = listOf(contact1, contact2, contact3))

    // When
    val responseSpec = callPublicPastVisits(webTestClient, prisonerId, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk
      .expectBody()

    val visits = getResults(returnResult)
    Assertions.assertThat(visits.size).isEqualTo(2)
    Assertions.assertThat(visits[0].visitors.size).isEqualTo(2)
    Assertions.assertThat(visits[0].prisonerFirstName).isEqualTo(prisoner.firstName)
    Assertions.assertThat(visits[0].prisonerLastName).isEqualTo(prisoner.lastName)
    assertVisitorDetails(visits[0].visitors, contacts)
    assertVisitorDetails(visits[1].visitors, contacts)

    Mockito.verify(prisonerContactRegistryClient, times(1)).getPrisonersSocialContacts(prisonerId, withAddress = false)
    Mockito.verify(prisonerSearchClient, times(1)).getPrisonerById(prisonerId)
  }

  @Test
  fun `when past visits for booker exists but prisoner contact registry returns 404 visits are returned but visitors names are not populated`() {
    // Given
    val visitsList = mutableListOf(visitDto, visitDto2)
    visitSchedulerMockServer.stubPublicPastVisitsByBookerReference(prisonerId, visitsList)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, prisoner)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId = prisonerId, withAddress = false, contactsList = null)

    // When
    val responseSpec = callPublicPastVisits(webTestClient, prisonerId, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk
      .expectBody()

    val visits = getResults(returnResult)
    Assertions.assertThat(visits.size).isEqualTo(2)
    Assertions.assertThat(visits[0].visitors.size).isEqualTo(2)
    Assertions.assertThat(visits[0].prisonerFirstName).isEqualTo(prisoner.firstName)
    Assertions.assertThat(visits[0].prisonerLastName).isEqualTo(prisoner.lastName)
    Assertions.assertThat(visits[1].visitors.size).isEqualTo(2)
    Assertions.assertThat(visits[1].prisonerFirstName).isEqualTo(prisoner.firstName)
    Assertions.assertThat(visits[1].prisonerLastName).isEqualTo(prisoner.lastName)

    val allVisitors = visits.flatMap { it.visitors }
    for (visitor in allVisitors) {
      Assertions.assertThat(visitor.nomisPersonId).isNotNull()
      Assertions.assertThat(visitor.firstName).isNull()
      Assertions.assertThat(visitor.lastName).isNull()
    }

    Mockito.verify(prisonerContactRegistryClient, times(1)).getPrisonersSocialContacts(prisonerId, withAddress = false)
    Mockito.verify(prisonerSearchClient, times(1)).getPrisonerById(prisonerId)
  }

  @Test
  fun `when past visits for booker exists but prisoner search returns a 404 then visits are returned without prisoner info`() {
    // Given
    val visitsList = mutableListOf(visitDto, visitDto2)
    visitSchedulerMockServer.stubPublicPastVisitsByBookerReference(prisonerId, visitsList)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId, null)
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId = prisonerId, withAddress = false, contactsList = listOf(contact1, contact2, contact3))

    // When
    val responseSpec = callPublicPastVisits(webTestClient, prisonerId, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk
      .expectBody()

    val visits = getResults(returnResult)
    Assertions.assertThat(visits.size).isEqualTo(2)
    Assertions.assertThat(visits[0].visitors.size).isEqualTo(2)
    assertVisitorDetails(visits[0].visitors, contacts)
    Assertions.assertThat(visits[0].prisonerFirstName).isEqualTo(null)
    Assertions.assertThat(visits[0].prisonerLastName).isEqualTo(null)
    assertVisitorDetails(visits[1].visitors, contacts)
    Assertions.assertThat(visits[0].prisonerFirstName).isEqualTo(null)
    Assertions.assertThat(visits[0].prisonerLastName).isEqualTo(null)

    Mockito.verify(prisonerContactRegistryClient, times(1)).getPrisonersSocialContacts(prisonerId, withAddress = false)
    Mockito.verify(prisonerSearchClient, times(1)).getPrisonerById(prisonerId)
  }

  @Test
  fun `when past visits for booker do not exists then get past visits returns empty content`() {
    // Given
    val prisonerId = "AABBCC"
    val emptyList = mutableListOf<VisitDto>()
    visitSchedulerMockServer.stubPublicPastVisitsByBookerReference(prisonerId, emptyList)

    // When
    val responseSpec = callPublicPastVisits(webTestClient, prisonerId, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk
      .expectBody()
    val visits = getResults(returnResult)
    Assertions.assertThat(visits.size).isEqualTo(0)
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): Array<OrchestrationVisitDto> = objectMapper.readValue(returnResult.returnResult().responseBody, Array<OrchestrationVisitDto>::class.java)
}
