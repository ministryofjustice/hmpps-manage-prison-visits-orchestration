package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.visit

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitMinSummaryDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitRestriction
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import java.time.LocalDate

@DisplayName("Get visits by session template for a date and status")
class VisitsBySessionTemplateTest : IntegrationTestBase() {
  private val prisonerId1 = "AA123BB"
  private val prisonerId2 = "BB123BB"

  private final val prisonerDto1 = createPrisoner(
    prisonerId = prisonerId1,
    firstName = "John",
    lastName = "Smith",
    dateOfBirth = LocalDate.of(2000, 1, 1),
  )

  private final val prisonerDto2 = createPrisoner(
    prisonerId = prisonerId2,
    firstName = "Johnny",
    lastName = "Bravo",
    dateOfBirth = LocalDate.of(2000, 1, 1),
  )

  @Test
  fun `when visits exist for session template and date then minimum visit details are returned`() {
    // Given
    val sessionTemplateReference = "session-1"
    val sessionDate = LocalDate.now()
    val visitDto = createVisitDto(reference = "ss-bb", prisonerId = prisonerId1, sessionTemplateReference = sessionTemplateReference, startTimestamp = sessionDate.atTime(10, 0), endTimestamp = sessionDate.atTime(11, 0))
    val visitDto2 = createVisitDto(reference = "xx-bb", prisonerId = prisonerId2, sessionTemplateReference = sessionTemplateReference, startTimestamp = sessionDate.atTime(10, 0), endTimestamp = sessionDate.atTime(11, 0))
    val visitStatus = "BOOKED"
    val visitRestriction = VisitRestriction.OPEN
    val visitsList = mutableListOf(visitDto, visitDto2)

    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId1, prisonerDto1)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId2, prisonerDto2)
    visitSchedulerMockServer.stubGetVisitsBySessionTemplate(sessionTemplateReference, sessionDate, listOf(visitStatus), listOf(visitRestriction), 0, 1000, visitsList)

    // When
    val responseSpec = callGetVisitsBySessionTemplate(webTestClient, sessionTemplateReference, sessionDate, listOf(visitStatus), listOf(visitRestriction), 1, 10, roleVisitSchedulerHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val visits = getResults(responseSpec).toList()
    Assertions.assertThat(visits).hasSize(2)
    val visitReferences = visits.stream().map { it.visitReference }
    Assertions.assertThat(visitReferences).containsExactlyInAnyOrder(visitDto.reference, visitDto2.reference)

    val visit1 = getVisitByReference(visits, visitDto.reference)
    assertVisitDetails(visit1, visitDto.reference, prisonerId1, prisonerDto1.firstName, prisonerDto1.lastName)

    val visit2 = getVisitByReference(visits, visitDto2.reference)
    assertVisitDetails(visit2, visitDto2.reference, prisonerId2, prisonerDto2.firstName, prisonerDto2.lastName)
  }

  @Test
  fun `when visits do not exist for session template and date then no visits are returned`() {
    // Given
    val sessionTemplateReference = "session-1"
    val sessionDate = LocalDate.now()
    val visitStatus = "BOOKED"
    val visitRestriction = VisitRestriction.OPEN

    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId1, prisonerDto1)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId2, prisonerDto2)
    visitSchedulerMockServer.stubGetVisitsBySessionTemplate(sessionTemplateReference, sessionDate, listOf(visitStatus), listOf(visitRestriction), 0, 1000, mutableListOf())

    // When
    val responseSpec = callGetVisitsBySessionTemplate(webTestClient, sessionTemplateReference, sessionDate, listOf(visitStatus), listOf(visitRestriction), 1, 10, roleVisitSchedulerHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val visits = getResults(responseSpec).toList()
    Assertions.assertThat(visits).hasSize(0)
  }

  @Test
  fun `when prisoner search returns an error and date then first and last name is replaced by prisoner id`() {
    // Given
    val sessionTemplateReference = "session-1"
    val sessionDate = LocalDate.now()
    val visitDto = createVisitDto(reference = "ss-bb", prisonerId = prisonerId1, sessionTemplateReference = sessionTemplateReference, startTimestamp = sessionDate.atTime(10, 0), endTimestamp = sessionDate.atTime(11, 0))
    val visitDto2 = createVisitDto(reference = "xx-bb", prisonerId = prisonerId2, sessionTemplateReference = sessionTemplateReference, startTimestamp = sessionDate.atTime(10, 0), endTimestamp = sessionDate.atTime(11, 0))
    val visitStatus = "BOOKED"
    val visitRestriction = VisitRestriction.OPEN
    val visitsList = mutableListOf(visitDto, visitDto2)

    // prisoner1 search returns 404
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId1, null)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId2, prisonerDto2)
    visitSchedulerMockServer.stubGetVisitsBySessionTemplate(sessionTemplateReference, sessionDate, listOf(visitStatus), listOf(visitRestriction), 0, 1000, visitsList)

    // When
    val responseSpec = callGetVisitsBySessionTemplate(webTestClient, sessionTemplateReference, sessionDate, listOf(visitStatus), listOf(visitRestriction), 1, 10, roleVisitSchedulerHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val visits = getResults(responseSpec).toList()
    Assertions.assertThat(visits).hasSize(2)
    val visitReferences = visits.stream().map { it.visitReference }
    Assertions.assertThat(visitReferences).containsExactlyInAnyOrder(visitDto.reference, visitDto2.reference)

    val visit1 = getVisitByReference(visits, visitDto.reference)
    // prisoner names should be replaced by prisoner ids
    assertVisitDetails(visit1, visitDto.reference, prisonerId1, prisonerId1, prisonerId1)

    val visit2 = getVisitByReference(visits, visitDto2.reference)
    assertVisitDetails(visit2, visitDto2.reference, prisonerId2, prisonerDto2.firstName, prisonerDto2.lastName)
  }

  private fun getResults(responseSpec: WebTestClient.ResponseSpec): Array<VisitMinSummaryDto> {
    return objectMapper.readValue(responseSpec.expectBody().returnResult().responseBody, Array<VisitMinSummaryDto>::class.java)
  }

  private fun assertVisitDetails(visit: VisitMinSummaryDto, visitReference: String, prisonerId: String, firstName: String, lastName: String) {
    Assertions.assertThat(visit.visitReference).isEqualTo(visitReference)
    Assertions.assertThat(visit.prisonerId).isEqualTo(prisonerId)
    Assertions.assertThat(visit.firstName).isEqualTo(firstName)
    Assertions.assertThat(visit.lastName).isEqualTo(lastName)
  }

  private fun getVisitByReference(visits: List<VisitMinSummaryDto>, reference: String): VisitMinSummaryDto {
    return visits.toList().stream().filter { it.visitReference == reference }.findFirst().get()
  }
}
