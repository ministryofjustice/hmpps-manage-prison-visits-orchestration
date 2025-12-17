package uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.builder

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.contact.registry.PrisonerContactDto
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.orchestration.OrchestrationVisitDto
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.prisoner.search.PrisonerDto
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.visit.scheduler.VisitDto

@Component
class OrchestrationVisitDtoBuilder(
  private val orchestrationVisitorDtoBuilder: OrchestrationVisitorDtoBuilder,
) {
  fun build(visitDto: VisitDto, prisonerContacts: List<PrisonerContactDto>, prisoner: PrisonerDto? = null): OrchestrationVisitDto = OrchestrationVisitDto(
    reference = visitDto.reference,
    prisonerId = visitDto.prisonerId,
    prisonerFirstName = prisoner?.firstName,
    prisonerLastName = prisoner?.lastName,
    prisonCode = visitDto.prisonCode,
    visitStatus = visitDto.visitStatus,
    visitSubStatus = visitDto.visitSubStatus,
    outcomeStatus = visitDto.outcomeStatus,
    startTimestamp = visitDto.startTimestamp,
    endTimestamp = visitDto.endTimestamp,
    visitContact = visitDto.visitContact,
    visitors = visitDto.visitors?.map {
      orchestrationVisitorDtoBuilder.build(it, prisonerContacts)
    }?.toList() ?: emptyList(),
    visitorSupport = visitDto.visitorSupport,
  )
}
