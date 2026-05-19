package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.ContactWithOptionalPrisonerRelationshipDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.AttributeSearchPrisonerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.passes.VisitPassDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.passes.VisitPassVisitorDto
import java.time.Duration

@Component
class VisitPassesClient(
  private val prisonerContactRegistryClient: PrisonerContactRegistryClient,
  private val prisonerSearchClient: PrisonerSearchClient,
  @param:Value("\${visit.passes.timeout:10s}") private val apiTimeout: Duration,
) {
  companion object {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getVisitPasses(visits: List<VisitDto>): List<VisitPassDto> {
    logger.info("Getting visit passes for {} visit(s)", visits.size)
    if (visits.isEmpty()) {
      return emptyList()
    }

    val visitorIds = visits.mapNotNull { it.visitors }.flatten().map { it.nomisPersonId }.distinct()
    val prisonerIds = visits.map { it.prisonerId }.distinct()
    logger.info("Getting contact details for {} contact(s)", visitorIds.size)
    logger.info("Getting prisoner details for {} prisoner(s)", prisonerIds.size)

    val prisonerSearchMono = prisonerSearchClient.getPrisonersByPrisonerIdsAttributeSearchAsMono(prisonerIds)
    val visitorsMono = prisonerContactRegistryClient.searchContactsAsMono(visitorIds)
    val prisonerAndVisitorDetails = Mono.zip(prisonerSearchMono, visitorsMono)
      .block(apiTimeout)
      ?: throw IllegalStateException("Failed to retrieve prisoner and visitor details")
    val prisonerDetails = prisonerAndVisitorDetails.t1.toList().associateBy { it.prisonerNumber }
    val visitorDetails = prisonerAndVisitorDetails.t2.associateBy { it.contactId }

    return getVisitPasses(visits, prisonerDetails, visitorDetails)
  }

  private fun getVisitPasses(visits: List<VisitDto>, prisonerDetails: Map<String, AttributeSearchPrisonerDto>, visitorDetails: Map<Long, ContactWithOptionalPrisonerRelationshipDto>): List<VisitPassDto> = visits.map { visit ->
    val prisonerName = getPrisonerName(visit.prisonerId, prisonerDetails)
    val visitors = getVisitorDetails(visit, visitorDetails)
    VisitPassDto(
      reference = visit.reference,
      startTime = visit.startTimestamp.toLocalTime(),
      endTime = visit.endTimestamp.toLocalTime(),
      prisonerId = visit.prisonerId,
      prisonerFirstName = prisonerName.first,
      prisonerLastName = prisonerName.second,
      visitRestriction = visit.visitRestriction,
      visitors = visitors,
    )
  }

  private fun getPrisonerName(prisonerId: String, prisonersMap: Map<String, AttributeSearchPrisonerDto>): Pair<String, String> {
    val prisonerDetails = prisonersMap[prisonerId]
    return if (prisonerDetails == null) {
      logger.error("No prisoner found for prisoner id - $prisonerId")
      throw IllegalStateException("No prisoner found for prisoner id - $prisonerId")
    } else {
      Pair(prisonerDetails.firstName, prisonerDetails.lastName)
    }
  }

  private fun getVisitorDetails(visitDto: VisitDto, visitorsMap: Map<Long, ContactWithOptionalPrisonerRelationshipDto>): List<VisitPassVisitorDto> {
    val visitorIdsForVisit = visitDto.visitors?.map { it.nomisPersonId }?.distinct()
    val visitorDetails: MutableList<VisitPassVisitorDto> = mutableListOf()
    visitorIdsForVisit?.forEach { visitorId ->
      visitorDetails.add(getVisitPassVisitorDto(visitorId, visitorsMap))
    }

    return visitorDetails.toList()
  }

  private fun getVisitPassVisitorDto(visitorId: Long, visitorsMap: Map<Long, ContactWithOptionalPrisonerRelationshipDto>): VisitPassVisitorDto {
    val visitorDetails = visitorsMap[visitorId]
    return if (visitorDetails == null) {
      logger.error("No visitor found for visitor id - $visitorId")
      throw IllegalStateException("No visitor found for visitor id - $visitorId")
    } else {
      VisitPassVisitorDto(
        nomisPersonId = visitorDetails.contactId,
        firstName = visitorDetails.firstName,
        lastName = visitorDetails.lastName,
        dob = visitorDetails.dateOfBirth,
        address = visitorDetails.address,
      )
    }
  }
}
