package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.builder

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.PrisonerContactDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.OrchestrationVisitorDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitorDto

@Component
class OrchestrationVisitorDtoBuilder {
  fun build(visitor: VisitorDto, contacts: List<PrisonerContactDto>): OrchestrationVisitorDto {
    val contact = contacts.firstOrNull { it.personId == visitor.nomisPersonId }
    return OrchestrationVisitorDto(
      nomisPersonId = visitor.nomisPersonId,
      firstName = contact?.firstName,
      lastName = contact?.lastName,
    )
  }
}
