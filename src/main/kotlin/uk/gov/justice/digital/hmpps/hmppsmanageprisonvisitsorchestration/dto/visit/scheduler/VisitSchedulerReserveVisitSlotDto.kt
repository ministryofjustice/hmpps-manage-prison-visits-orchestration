package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank

/**
 * This DTO replicates what the DTO the Visit Scheduler expects to reserve a visit.
 * It replicates the ReserveVisitSlotDto - however there is an additional actionedBy field that will be
 * populated before sending it over to VisitScheduler.
 */
data class VisitSchedulerReserveVisitSlotDto(
  @field:Valid
  private val reserveVisitSlotDto: ReserveVisitSlotDto,

  @field:NotBlank
  val actionedBy: String,
) : ReserveVisitSlotDto(
  reserveVisitSlotDto.prisonerId,
  reserveVisitSlotDto.sessionTemplateReference,
  reserveVisitSlotDto.visitRestriction,
  reserveVisitSlotDto.startTimestamp,
  reserveVisitSlotDto.endTimestamp,
  reserveVisitSlotDto.visitContact,
  reserveVisitSlotDto.visitors,
  reserveVisitSlotDto.visitorSupport,
)
