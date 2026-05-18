package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.visit.passes

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.AddressDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.ContactWithOptionalPrisonerRelationshipDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitPassDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitPassVisitorDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitorDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitStatus
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.TestObjectMapper
import java.time.LocalDate

@DisplayName("Get Visit passes for a prison and for a given date")
class GetVisitPassesTest : IntegrationTestBase() {
  val prisonCode = "HEI"
  val prisoner1 = createPrisoner(
    prisonerId = "AB1234CD",
    firstName = "Johnny",
    lastName = "English",
    dateOfBirth = LocalDate.of(1980, 1, 1),
    prisonId = prisonCode,
    convictedStatus = "REMAND",
  )

  val contact1 = createContactWithOptionalPrisonerRelationshipDto(
    personId = 1,
    firstName = "Jim",
    lastName = "Bim",
    dateOfBirth = LocalDate.of(1980, 1, 1),
    address = AddressDto(flat = "11", street = "ABX Lane", town = "Test Town", primary = true, noFixedAddress = false),
  )

  val contact2 = createContactWithOptionalPrisonerRelationshipDto(
    personId = 2,
    firstName = "Ann",
    lastName = "Can",
    dateOfBirth = null,
    address = AddressDto(premise = "West", street = "DVS Street", town = "Test Town", primary = false, noFixedAddress = false),
  )

  @Test
  fun `when a prison has multiple booked visits then get visit passes returns visit passes with details populated`() {
    // Given
    val visitors = listOf(VisitorDto(contact1.contactId, false), VisitorDto(contact2.contactId, false))
    val visit1 = createVisitDto(reference = "visit-1", prisonerId = prisoner1.prisonerNumber, visitors = visitors, startTimestamp = LocalDate.now().atTime(10, 0), endTimestamp = LocalDate.now().atTime(11, 0))
    visitSchedulerMockServer.stubGetVisits(prisonCode = prisonCode, startDate = LocalDate.now(), endDate = LocalDate.now(), page = 0, size = 300, visitStatus = listOf(VisitStatus.BOOKED.name), visits = listOf(visit1))
    prisonerContactRegistryMockServer.stubSearchContacts(contactIds = listOf(contact1.contactId, contact2.contactId), contactsList = listOf(contact1, contact2))
    prisonOffenderSearchMockServer.stubGetPrisonersByPrisonerIds(listOf(prisoner1.prisonerNumber), listOf(prisoner1))

    // When
    val responseSpec = callGetVisitPasses(webTestClient, prisonCode, LocalDate.now(), roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val visitPasses = getResults(responseSpec.expectBody())
    assertThat(visitPasses.size).isEqualTo(1)
    assertVisitPassDetails(visit1, prisoner1, listOf(contact1, contact2), visitPasses[0])
  }

  private fun callGetVisitPasses(
    webTestClient: WebTestClient,
    prisonCode: String,
    visitDate: LocalDate,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec = webTestClient.get()
    .uri("/visit-passes/prison/$prisonCode?visitDate=$visitDate")
    .headers(authHttpHeaders)
    .exchange()
}

private fun getResults(returnResult: WebTestClient.BodyContentSpec): List<VisitPassDto> = TestObjectMapper.mapper.readValue(returnResult.returnResult().responseBody, Array<VisitPassDto>::class.java).toList()

private fun assertVisitPassDetails(
  visitDto: VisitDto,
  prisonerDto: PrisonerDto,
  contactDtoList: List<ContactWithOptionalPrisonerRelationshipDto>,
  visitPassDto: VisitPassDto,
) {
  assertThat(visitPassDto.startTime).isEqualTo(visitDto.startTimestamp.toLocalTime())
  assertThat(visitPassDto.endTime).isEqualTo(visitDto.endTimestamp.toLocalTime())
  assertThat(visitPassDto.prisonerId).isEqualTo(visitDto.prisonerId)
  assertThat(visitPassDto.prisonerId).isEqualTo(prisonerDto.prisonerNumber)
  assertThat(visitPassDto.prisonerFirstName).isEqualTo(prisonerDto.firstName)
  assertThat(visitPassDto.prisonerLastName).isEqualTo(prisonerDto.lastName)
  assertThat(visitPassDto.visitRestriction).isEqualTo(visitDto.visitRestriction)
  assertThat(visitPassDto.visitors.size).isEqualTo(contactDtoList.size)
  contactDtoList.forEachIndexed { index, contactDto ->
    assertVisitors(contactDto, visitPassDto.visitors[index])
  }
}

private fun assertVisitors(
  contactDto: ContactWithOptionalPrisonerRelationshipDto,
  visitPassVisitorDto: VisitPassVisitorDto,
) {
  assertThat(visitPassVisitorDto.firstName).isEqualTo(contactDto.firstName)
  assertThat(visitPassVisitorDto.lastName).isEqualTo(contactDto.lastName)
  assertThat(visitPassVisitorDto.dob).isEqualTo(contactDto.dateOfBirth)
  assertThat(visitPassVisitorDto.address).isEqualTo(contactDto.address)
}
