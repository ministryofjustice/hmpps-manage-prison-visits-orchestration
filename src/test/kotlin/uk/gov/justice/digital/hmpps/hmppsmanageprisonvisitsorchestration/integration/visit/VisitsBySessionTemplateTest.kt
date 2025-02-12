package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.visit

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionTimeSlotDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitPreviewDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitorDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitRestriction
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@DisplayName("Get visits by session template for a date and status")
class VisitsBySessionTemplateTest : IntegrationTestBase() {
  private val prisonerId1 = "AA123BB"
  private val prisonerId2 = "BB123BB"
  private val prisonCode = "MDI"
  private lateinit var prisonerDto1: PrisonerDto
  private lateinit var prisonerDto2: PrisonerDto

  @BeforeEach
  fun setupData() {
    prisonerDto1 = createPrisoner(
      prisonerId = prisonerId1,
      firstName = "John",
      lastName = "Smith",
      dateOfBirth = LocalDate.of(2000, 1, 1),
    )

    prisonerDto2 = createPrisoner(
      prisonerId = prisonerId2,
      firstName = "Johnny",
      lastName = "Bravo",
      dateOfBirth = LocalDate.of(2000, 1, 1),
    )
  }

  @Test
  fun `when visits exist for session template and date then minimum visit details are returned`() {
    // Given
    val sessionTemplateReference = "session-1"
    val sessionDate = LocalDate.now()
    val visit1Visitors = listOf(VisitorDto(nomisPersonId = 1, visitContact = false), VisitorDto(nomisPersonId = 2, visitContact = false), VisitorDto(nomisPersonId = 3, visitContact = true))
    val visitDto = createVisitDto(reference = "ss-bb", prisonerId = prisonerId1, sessionTemplateReference = sessionTemplateReference, startTimestamp = sessionDate.atTime(10, 0), endTimestamp = sessionDate.atTime(11, 0), visitors = visit1Visitors, firstBookedDate = LocalDate.now().atTime(11, 0))
    val visitDto2 = createVisitDto(reference = "xx-bb", prisonerId = prisonerId2, sessionTemplateReference = sessionTemplateReference, startTimestamp = sessionDate.atTime(10, 0), endTimestamp = sessionDate.atTime(11, 0), firstBookedDate = LocalDate.now().atTime(12, 0))
    val visitStatus = "BOOKED"
    val visitRestriction = VisitRestriction.OPEN
    val visitsList = mutableListOf(visitDto, visitDto2)

    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId1, prisonerDto1)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId2, prisonerDto2)
    visitSchedulerMockServer.stubGetVisitsBySessionTemplate(sessionTemplateReference, sessionDate, listOf(visitStatus), listOf(visitRestriction), prisonCode, 0, 1000, visitsList)

    // When
    val responseSpec = callGetVisitsBySessionTemplate(webTestClient, sessionTemplateReference, sessionDate, listOf(visitStatus), listOf(visitRestriction), prisonCode, 1, 10, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val visits = getResults(responseSpec).toList()
    Assertions.assertThat(visits).hasSize(2)
    val visitReferences = visits.stream().map { it.visitReference }
    Assertions.assertThat(visitReferences).containsExactlyInAnyOrder(visitDto.reference, visitDto2.reference)

    val visit1 = getVisitByReference(visits, visitDto.reference)
    assertVisitDetails(visit1, visitDto.reference, prisonerId1, prisonerDto1.firstName, prisonerDto1.lastName, 3, visitDto.firstBookedDateTime, visitDto.visitRestriction)

    val visit2 = getVisitByReference(visits, visitDto2.reference)
    assertVisitDetails(visit2, visitDto2.reference, prisonerId2, prisonerDto2.firstName, prisonerDto2.lastName, 0, visitDto2.firstBookedDateTime, visitDto.visitRestriction)
  }

  @Test
  fun `when session template reference passed is null then minimum visit details are returned`() {
    // Given
    val sessionTemplateReference = "session-1"
    val sessionDate = LocalDate.now()
    val visit1Visitors = listOf(VisitorDto(nomisPersonId = 1, visitContact = false), VisitorDto(nomisPersonId = 2, visitContact = false), VisitorDto(nomisPersonId = 3, visitContact = true))
    val visitDto = createVisitDto(reference = "ss-bb", prisonerId = prisonerId1, sessionTemplateReference = null, startTimestamp = sessionDate.atTime(10, 0), endTimestamp = sessionDate.atTime(11, 0), visitors = visit1Visitors)
    val visitDto2 = createVisitDto(reference = "xx-bb", prisonerId = prisonerId2, sessionTemplateReference = null, startTimestamp = sessionDate.atTime(10, 0), endTimestamp = sessionDate.atTime(11, 0))
    val visitStatus = "BOOKED"
    val visitRestriction = VisitRestriction.OPEN
    val visitsList = mutableListOf(visitDto, visitDto2)

    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId1, prisonerDto1)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId2, prisonerDto2)
    visitSchedulerMockServer.stubGetVisitsBySessionTemplate(sessionTemplateReference, sessionDate, listOf(visitStatus), listOf(visitRestriction), prisonCode, 0, 1000, visitsList)

    // When
    val responseSpec = callGetVisitsBySessionTemplate(webTestClient, sessionTemplateReference, sessionDate, listOf(visitStatus), listOf(visitRestriction), prisonCode, 1, 10, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val visits = getResults(responseSpec).toList()
    Assertions.assertThat(visits).hasSize(2)
    val visitReferences = visits.stream().map { it.visitReference }
    Assertions.assertThat(visitReferences).containsExactlyInAnyOrder(visitDto.reference, visitDto2.reference)

    val visit1 = getVisitByReference(visits, visitDto.reference)
    assertVisitDetails(visit1, visitDto.reference, prisonerId1, prisonerDto1.firstName, prisonerDto1.lastName, 3, visitDto.createdTimestamp, visitDto.visitRestriction)

    val visit2 = getVisitByReference(visits, visitDto2.reference)
    assertVisitDetails(visit2, visitDto2.reference, prisonerId2, prisonerDto2.firstName, prisonerDto2.lastName, 0, visitDto2.createdTimestamp, visitDto.visitRestriction)
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
    visitSchedulerMockServer.stubGetVisitsBySessionTemplate(sessionTemplateReference, sessionDate, listOf(visitStatus), listOf(visitRestriction), prisonCode, 0, 1000, mutableListOf())

    // When
    val responseSpec = callGetVisitsBySessionTemplate(webTestClient, sessionTemplateReference, sessionDate, listOf(visitStatus), listOf(visitRestriction), prisonCode, 1, 10, roleVSIPOrchestrationServiceHttpHeaders)

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
    val visit1Visitors = listOf(VisitorDto(nomisPersonId = 1, visitContact = false), VisitorDto(nomisPersonId = 2, visitContact = false), VisitorDto(nomisPersonId = 3, visitContact = true))
    val visitDto = createVisitDto(reference = "ss-bb", prisonerId = prisonerId1, sessionTemplateReference = sessionTemplateReference, startTimestamp = sessionDate.atTime(10, 0), endTimestamp = sessionDate.atTime(11, 0), visitors = visit1Visitors)

    val visit2Visitors = listOf(VisitorDto(nomisPersonId = 11, visitContact = false), VisitorDto(nomisPersonId = 12, visitContact = false))
    val visitDto2 = createVisitDto(reference = "xx-bb", prisonerId = prisonerId2, sessionTemplateReference = sessionTemplateReference, startTimestamp = sessionDate.atTime(10, 0), endTimestamp = sessionDate.atTime(11, 0), visitors = visit2Visitors)
    val visitStatus = "BOOKED"
    val visitRestriction = VisitRestriction.OPEN
    val visitsList = mutableListOf(visitDto, visitDto2)

    // prisoner1 search returns 404
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId1, null)
    prisonOffenderSearchMockServer.stubGetPrisonerById(prisonerId2, prisonerDto2)
    visitSchedulerMockServer.stubGetVisitsBySessionTemplate(sessionTemplateReference, sessionDate, listOf(visitStatus), listOf(visitRestriction), prisonCode, 0, 1000, visitsList)

    // When
    val responseSpec = callGetVisitsBySessionTemplate(webTestClient, sessionTemplateReference, sessionDate, listOf(visitStatus), listOf(visitRestriction), prisonCode, 1, 10, roleVSIPOrchestrationServiceHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    val visits = getResults(responseSpec).toList()
    Assertions.assertThat(visits).hasSize(2)
    val visitReferences = visits.stream().map { it.visitReference }
    Assertions.assertThat(visitReferences).containsExactlyInAnyOrder(visitDto.reference, visitDto2.reference)

    val visit1 = getVisitByReference(visits, visitDto.reference)
    // prisoner names should be replaced by prisoner ids
    assertVisitDetails(visit1, visitDto.reference, prisonerId1, prisonerId1, prisonerId1, 3, visitDto.createdTimestamp, visitDto.visitRestriction)

    val visit2 = getVisitByReference(visits, visitDto2.reference)
    assertVisitDetails(visit2, visitDto2.reference, prisonerId2, prisonerDto2.firstName, prisonerDto2.lastName, 2, visitDto2.createdTimestamp, visitDto.visitRestriction)
  }

  private fun getResults(responseSpec: WebTestClient.ResponseSpec): Array<VisitPreviewDto> = objectMapper.readValue(responseSpec.expectBody().returnResult().responseBody, Array<VisitPreviewDto>::class.java)

  private fun assertVisitDetails(visit: VisitPreviewDto, visitReference: String, prisonerId: String, firstName: String, lastName: String, visitorCount: Int, firstBookedDateTime: LocalDateTime?, visitRestriction: VisitRestriction) {
    Assertions.assertThat(visit.visitReference).isEqualTo(visitReference)
    Assertions.assertThat(visit.prisonerId).isEqualTo(prisonerId)
    Assertions.assertThat(visit.firstName).isEqualTo(firstName)
    Assertions.assertThat(visit.lastName).isEqualTo(lastName)
    Assertions.assertThat(visit.visitorCount).isEqualTo(visitorCount)
    Assertions.assertThat(visit.visitTimeSlot).isEqualTo(SessionTimeSlotDto(LocalTime.of(10, 0), LocalTime.of(11, 0)))
    Assertions.assertThat(visit.firstBookedDateTime).isEqualTo(firstBookedDateTime)
    Assertions.assertThat(visit.visitRestriction).isEqualTo(visitRestriction)
  }

  private fun getVisitByReference(visits: List<VisitPreviewDto>, reference: String): VisitPreviewDto = visits.toList().stream().filter { it.visitReference == reference }.findFirst().get()
}
