package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler

import com.fasterxml.jackson.annotation.JsonProperty
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
  reserveVisitSlotDto.prisonCode,
  reserveVisitSlotDto.visitRoom,
  reserveVisitSlotDto.visitType,
  reserveVisitSlotDto.visitRestriction,
  reserveVisitSlotDto.startTimestamp,
  reserveVisitSlotDto.endTimestamp,
  reserveVisitSlotDto.visitContact,
  reserveVisitSlotDto.visitors,
  reserveVisitSlotDto.visitorSupport,
) {
  @JsonProperty("prisonId")
  override val prisonCode = reserveVisitSlotDto.prisonCode
}
