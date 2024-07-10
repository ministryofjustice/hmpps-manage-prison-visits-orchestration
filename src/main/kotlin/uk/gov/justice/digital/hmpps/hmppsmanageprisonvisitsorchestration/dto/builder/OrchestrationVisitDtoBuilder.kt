package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.builder

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.PrisonerContactDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.OrchestrationVisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitDto

@Component
class OrchestrationVisitDtoBuilder(
  private val orchestrationVisitorDtoBuilder: OrchestrationVisitorDtoBuilder,
) {
  fun build(visitDto: VisitDto, prisonerContacts: List<PrisonerContactDto>): OrchestrationVisitDto {
    return OrchestrationVisitDto(
      reference = visitDto.reference,
      prisonerId = visitDto.prisonerId,
      prisonCode = visitDto.prisonCode,
      visitStatus = visitDto.visitStatus,
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
}
