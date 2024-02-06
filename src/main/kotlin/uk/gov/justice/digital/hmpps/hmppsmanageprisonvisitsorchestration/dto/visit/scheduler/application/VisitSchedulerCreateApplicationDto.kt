package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.application

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank

class VisitSchedulerCreateApplicationDto(
  /**
   * This DTO replicates what the DTO the Visit Scheduler expects to reserve a visit.
   * It replicates the ReserveVisitSlotDto - however there is an additional actionedBy field that will be
   * populated before sending it over to VisitScheduler.
   */
  @field:Valid
  private val createApplicationDto: CreateApplicationDto,

  @field:NotBlank
  val actionedBy: String,
) : CreateApplicationDto(
  createApplicationDto.prisonerId,
  createApplicationDto.sessionTemplateReference,
  createApplicationDto.sessionDate,
  createApplicationDto.applicationRestriction,
  createApplicationDto.visitContact,
  createApplicationDto.visitors,
  createApplicationDto.visitorSupport,
)
