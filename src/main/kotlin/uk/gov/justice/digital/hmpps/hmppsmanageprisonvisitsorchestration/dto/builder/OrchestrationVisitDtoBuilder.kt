package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.builder

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.OrchestrationVisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.OrchestrationVisitorDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitDto

@Component
class OrchestrationVisitDtoBuilder {

  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }
  fun build(visitDto: VisitDto): OrchestrationVisitDto {
    return OrchestrationVisitDto(
      reference = visitDto.reference,
      prisonerId = visitDto.prisonerId,
      prisonCode = visitDto.prisonCode,
      visitStatus = visitDto.visitStatus,
      outcomeStatus = visitDto.outcomeStatus,
      startTimestamp = visitDto.startTimestamp,
      endTimestamp = visitDto.endTimestamp,
      visitContact = visitDto.visitContact,
      visitors = visitDto.visitors?.map { OrchestrationVisitorDto(it.nomisPersonId) }?.toList() ?: emptyList(),
      visitorSupport = visitDto.visitorSupport,
    )
  }
}
